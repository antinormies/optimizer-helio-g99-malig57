package io.github.antinormies.opt_heliog99.shizuku

import android.util.Log
import java.io.*
import java.net.Socket
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

class AdbTransport : CommandTransport {

    override val name: String get() = "ADB"

    private var listener: CommandTransport.Listener? = null
    private var socket: Socket? = null
    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null
    private var localId = 0
    private var remoteId = 0
    private var connected = false
    private var authenticated = false
    private var keyDir: File? = null

    override val isAvailable: Boolean
        get() {
            if (connected && authenticated) return true
            val port = findAdbPort() ?: return false
            return try { Socket("127.0.0.1", port).also { it.close() }; true }
            catch (_: Exception) { false }
        }

    override val hasPermission: Boolean get() = authenticated

    var adbPort: Int = 0  // 0 = auto-detect

    override fun init(listener: CommandTransport.Listener) {
        this.listener = listener
        connect()
    }

    fun initWithKeyDir(l: CommandTransport.Listener, keyDirectory: File?) {
        keyDir = keyDirectory
        init(l)
    }

    fun updatePort(port: Int) {
        adbPort = port
        if (connected) {
            disconnect()
            connect()
        }
    }

    override fun destroy() {
        disconnect()
        listener = null
    }

    private fun connect() {
        Thread {
            try {
                val port = findAdbPort()
                if (port == null) {
                    listener?.onError("ADB daemon not found on this device")
                    return@Thread
                }

                Log.i(TAG, "Connecting to adbd at 127.0.0.1:$port")
                socket = Socket("127.0.0.1", port)
                socket?.soTimeout = 10000
                inputStream = DataInputStream(BufferedInputStream(socket!!.getInputStream()))
                outputStream = DataOutputStream(BufferedOutputStream(socket!!.getOutputStream()))

                // Receive CNXN
                val cnxn = readPacket()
                if (cnxn == null || cnxn.type != "CNXN") {
                    listener?.onError("ADB: expected CNXN, got ${cnxn?.type}")
                    disconnect()
                    return@Thread
                }

                Log.i(TAG, "ADB CNXN received: ${cnxn.payload.take(50)}")

                // Handle AUTH if needed
                val packet = readPacket()
                if (packet != null && packet.type == "AUTH") {
                    val authResult = handleAuth(packet)
                    if (!authResult) {
                        listener?.onError("ADB authentication failed")
                        disconnect()
                        return@Thread
                    }
                } else if (packet != null && packet.type == "CNXN") {
                    // No auth needed
                } else {
                    listener?.onError("ADB: unexpected packet ${packet?.type}")
                    disconnect()
                    return@Thread
                }

                authenticated = true
                connected = true
                listener?.onConnected()
                Log.i(TAG, "ADB connected and authenticated")

            } catch (e: Exception) {
                Log.e(TAG, "ADB connection failed: ${e.message}")
                listener?.onError("ADB: ${e.message}")
            }
        }.start()
    }

    private fun disconnect() {
        connected = false
        authenticated = false
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        inputStream = null
        outputStream = null
        listener?.onDisconnected()
    }

    private fun handleAuth(authPacket: AdbPacket): Boolean {
        val authType = authPacket.arg0  // 1=TOKEN, 2=SIGNATURE, 3=RSAPUBLICKEY
        Log.i(TAG, "ADB AUTH type=$authType")

        return when (authType) {
            1 -> {
                // TOKEN challenge: sign it and send back
                val token = authPacket.payload
                val signature = signToken(token)
                if (signature == null) {
                    // Send public key for authorization
                    sendPublicKey()
                    // Wait for another AUTH with TOKEN or CNXN
                    val next = readPacket()
                    if (next != null && next.type == "AUTH" && next.arg0 == 1) {
                        val sig2 = signToken(next.payload)
                        if (sig2 != null) {
                            sendAuthSignature(sig2)
                            val finalPacket = readPacket()
                            return finalPacket?.type == "CNXN"
                        }
                    }
                    false
                } else {
                    sendAuthSignature(signature)
                    val next = readPacket()
                    if (next?.type == "CNXN") true
                    else if (next?.type == "AUTH" && next.arg0 == 3) {
                        // Key not recognized, send public key
                        sendPublicKey()
                        val afterKey = readPacket()
                        if (afterKey?.type == "AUTH" && afterKey.arg0 == 1) {
                            val sig3 = signToken(afterKey.payload)
                            if (sig3 != null) {
                                sendAuthSignature(sig3)
                                readPacket()?.type == "CNXN"
                            } else false
                        } else afterKey?.type == "CNXN"
                    } else false
                }
            }
            3 -> {
                // adbd wants our public key
                sendPublicKey()
                val next = readPacket()
                next?.type == "CNXN" || (next?.type == "AUTH" && next.arg0 == 1)
            }
            else -> false
        }
    }

    private fun findAdbPort(): Int? {
        if (adbPort > 0) return adbPort

        // 1. System properties
        val props = readSystemProperty("service.adb.tcp.port")
        if (props != null) { val p = props.toIntOrNull(); if (p != null && p > 0) return p }

        val tlsPort = readSystemProperty("service.adb.tls_port")
        if (tlsPort != null) { val p = tlsPort.toIntOrNull(); if (p != null && p > 0) return p }

        // 2. /proc/net/tcp6 — find LISTEN socket owned by UID 2000 (shell)
        val tcpPort = scanProcNet()
        if (tcpPort != null) return tcpPort

        // 3. Try common ports
        for (port in listOf(5555, 33849, 33851, 33848, 44121, 44122, 44123)) {
            if (testPort(port)) return port
        }

        // 4. Scan a wider range (30000-40000 — dynamic port range)
        for (port in 30000..31000 step 2) {
            if (testPort(port)) return port
        }

        return null
    }

    private fun scanProcNet(): Int? {
        try {
            val files = listOf("/proc/net/tcp6", "/proc/net/tcp")
            for (path in files) {
                val proc = Runtime.getRuntime().exec(arrayOf("cat", path))
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                reader.use { r ->
                    r.readLine() // skip header
                    for (line in r.lines()) {
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size >= 8) {
                            // Match LISTEN (0A) sockets owned by UID 2000
                            val state = parts[3]
                            val uid = parts[7].toIntOrNull() ?: -1
                            if (state == "0A" && uid == 2000) {
                                val localAddr = parts[1]
                                val addrParts = localAddr.split(":")
                                if (addrParts.size == 2) {
                                    val port = addrParts[1].toIntOrNull(16)
                                    if (port != null && port > 0) return port
                                }
                            }
                        }
                    }
                }
                proc.waitFor()
            }
        } catch (_: Exception) {}
        return null
    }

    private fun testPort(port: Int): Boolean {
        return try {
            val s = Socket("127.0.0.1", port)
            s.soTimeout = 500
            // Check if it responds with ADB protocol (CNXN header)
            val `is` = DataInputStream(s.getInputStream())
            val header = ByteArray(24)
            `is`.readFully(header)
            val type = String(header, 0, 4, Charsets.UTF_8)
            s.close()
            type == "CNXN"
        } catch (_: Exception) { false }
    }

    private fun readSystemProperty(name: String): String? {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("getprop", name))
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            val line = reader.readLine()
            reader.close()
            proc.waitFor()
            if (line.isNullOrBlank()) null else line.trim()
        } catch (_: Exception) { null }
    }

    private fun readPacket(): AdbPacket? {
        return try {
            val header = ByteArray(24)
            inputStream?.readFully(header)
            val type = String(header, 0, 4, Charsets.UTF_8)
            val arg0 = le32(header, 4)
            val arg1 = le32(header, 8)
            val dataLength = le32(header, 12)
            val data = if (dataLength > 0) {
                val buf = ByteArray(dataLength)
                inputStream?.readFully(buf)
                buf
            } else ByteArray(0)

            AdbPacket(type, arg0, arg1, data)
        } catch (e: Exception) {
            Log.e(TAG, "Read packet failed: ${e.message}")
            null
        }
    }

    private fun sendPacket(type: String, arg0: Int, arg1: Int, data: ByteArray = ByteArray(0)) {
        try {
            val header = ByteArray(24)
            type.toByteArray(Charsets.UTF_8).copyInto(header, 0)
            writeLe32(header, 4, arg0)
            writeLe32(header, 8, arg1)
            writeLe32(header, 12, data.size)
            writeLe32(header, 16, data.size) // crc32 (not strictly checked by adbd)
            writeLe32(header, 20, data.size) // magic

            outputStream?.write(header)
            if (data.isNotEmpty()) {
                outputStream?.write(data)
            }
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Send packet failed: ${e.message}")
        }
    }

    private fun openShell(command: String): Boolean {
        val service = "shell:$command"
        localId++
        sendPacket("OPEN", localId, 0, service.toByteArray(Charsets.UTF_8))

        val response = readPacket()
        if (response?.type == "OKAY") {
            remoteId = response.arg0
            return true
        }
        if (response?.type == "WRTE") {
            remoteId = response.arg0
            // adbd might send initial shell output
            return true
        }
        return false
    }

    private fun closeShell() {
        sendPacket("CLSE", localId, remoteId)
    }

    override fun runShellCommandAsync(
        command: String,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: (Int) -> Unit
    ) {
        Thread {
            try {
                if (!openShell(command)) {
                    onError("ADB: failed to open shell")
                    onComplete(-1)
                    return@Thread
                }

                val output = StringBuilder()

                // Read response
                while (true) {
                    val packet = readPacket() ?: break
                    when (packet.type) {
                        "WRTE" -> {
                            val text = String(packet.payload, Charsets.UTF_8)
                            output.append(text)
                            sendPacket("OKAY", packet.arg1, packet.arg0)
                        }
                        "OKAY" -> {
                            // More data coming
                        }
                        "CLSE" -> break
                        else -> break
                    }

                    // Check if shell is done
                    val text = output.toString()
                    if (text.contains("\n") && (text.endsWith("$ ") || text.endsWith("# "))) break
                }

                val result = output.toString().trim()
                if (result.isNotBlank()) {
                    result.lines().forEach { onOutput(it) }
                }

                closeShell()
                onComplete(0)
            } catch (e: Exception) {
                onError("ADB error: ${e.message}")
                onComplete(-1)
            }
        }.start()
    }

    private fun signToken(token: ByteArray): ByteArray? {
        return try {
            val keyPair = loadOrGenerateKey()
            val signature = Signature.getInstance("SHA1withRSA")
            signature.initSign(keyPair.private)
            signature.update(token)
            signature.sign()
        } catch (e: Exception) {
            Log.e(TAG, "Sign failed: ${e.message}")
            null
        }
    }

    private fun sendAuthSignature(signature: ByteArray) {
        sendPacket("AUTH", 2, 0, signature)
        // Wait for OKAY
        readPacket()
    }

    private fun sendPublicKey() {
        val keyPair = loadOrGenerateKey()
        val pubKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val adbKey = "$pubKeyBase64 shell@android"
        sendPacket("AUTH", 3, 0, adbKey.toByteArray(Charsets.UTF_8))
        Log.i(TAG, "ADB public key sent (length=${adbKey.length})")
    }

    private fun loadOrGenerateKey(): KeyPair {
        val dir = keyDir ?: return generateKeyPair()

        val privateFile = File(dir, "adbkey")
        val publicFile = File(dir, "adbkey.pub")

        if (privateFile.exists()) {
            try {
                val privKeyBytes = privateFile.readBytes()
                val pubKeyBytes = publicFile.readBytes()
                val keyFactory = KeyFactory.getInstance("RSA")
                val privKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privKeyBytes))
                val pubKey = keyFactory.generatePublic(X509EncodedKeySpec(pubKeyBytes))
                return KeyPair(pubKey, privKey)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load existing key, generating new: ${e.message}")
                privateFile.delete()
                publicFile.delete()
            }
        }

        val keyPair = generateKeyPair()
        try {
            dir.mkdirs()
            privateFile.writeBytes(keyPair.private.encoded)
            publicFile.writeBytes(keyPair.public.encoded)
            Log.i(TAG, "Generated new ADB key pair at $dir")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save key: ${e.message}")
        }
        return keyPair
    }

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }

    // --- ADB protocol helpers ---

    data class AdbPacket(
        val type: String,
        val arg0: Int,
        val arg1: Int,
        val payload: ByteArray
    )

    companion object {
        private const val TAG = "AdbTransport"

        private fun le32(data: ByteArray, offset: Int): Int {
            return (data[offset].toInt() and 0xFF) or
                    ((data[offset + 1].toInt() and 0xFF) shl 8) or
                    ((data[offset + 2].toInt() and 0xFF) shl 16) or
                    ((data[offset + 3].toInt() and 0xFF) shl 24)
        }

        private fun writeLe32(data: ByteArray, offset: Int, value: Int) {
            data[offset] = (value and 0xFF).toByte()
            data[offset + 1] = ((value shr 8) and 0xFF).toByte()
            data[offset + 2] = ((value shr 16) and 0xFF).toByte()
            data[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
    }
}
