package io.github.antinormies.opt_heliog99.shizuku

import android.content.Context
import android.util.Log

class TransportManager(private val context: Context) : CommandTransport {

    val shizuku = ShizukuManager()
    val adb = AdbTransport()

    private var active: CommandTransport? = null
    private var listener: CommandTransport.Listener? = null

    val activeTransport: CommandTransport? get() = active
    val isReady: Boolean get() = active?.isAvailable == true && active?.hasPermission == true
    val transportName: String get() = active?.name ?: "none"

    override val isAvailable: Boolean get() = active?.isAvailable ?: false
    override val hasPermission: Boolean get() = active?.hasPermission ?: false
    override val name: String get() = active?.name ?: "none"

    override fun init(listener: CommandTransport.Listener) {
        this.listener = listener

        shizuku.init(object : CommandTransport.Listener {
            override fun onConnected() {
                if (shizuku.hasPermission) {
                    if (active !== adb) {
                        active = shizuku
                        Log.i(TAG, "Using Shizuku transport")
                        listener?.onConnected()
                    }
                } else {
                    shizuku.requestPermission()
                }
            }

            override fun onDisconnected() {
                if (active === shizuku) {
                    Log.w(TAG, "Shizuku disconnected, trying ADB...")
                    tryAdbFallback()
                }
                listener?.onDisconnected()
            }

            override fun onError(message: String) {
                if (active === shizuku) {
                    Log.w(TAG, "Shizuku error: $message, trying ADB...")
                    tryAdbFallback()
                }
            }
        })

        // Proactive: if Shizuku is unavailable, try ADB immediately
        if (!shizuku.isAvailable) {
            Log.i(TAG, "Shizuku not available, trying ADB immediately")
            tryAdbFallback()
        } else if (shizuku.hasPermission) {
            active = shizuku
            Log.i(TAG, "Using Shizuku transport (synchronous init)")
            listener.onConnected()
        }
    }

    private fun tryAdbFallback() {
        adb.initWithKeyDir(object : CommandTransport.Listener {
            override fun onConnected() {
                if (adb.hasPermission) {
                    active = adb
                    Log.i(TAG, "Using ADB transport")
                    listener?.onConnected()
                }
            }

            override fun onDisconnected() {
                active = null
                listener?.onDisconnected()
            }

            override fun onError(message: String) {
                active = null
                listener?.onError("ADB fallback: $message")
            }
        }, context.filesDir)
    }

    override fun runShellCommandAsync(
        command: String,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: (Int) -> Unit
    ) {
        val transport = active
        if (transport != null) {
            transport.runShellCommandAsync(command, onOutput, onError, onComplete)
        } else {
            onError("No active transport")
            onComplete(-1)
        }
    }

    override fun destroy() {
        shizuku.destroy()
        adb.destroy()
        active = null
        listener = null
    }

    fun refreshTransport() {
        if (shizuku.isAvailable && shizuku.hasPermission) {
            active = shizuku
        } else if (adb.isAvailable && adb.hasPermission) {
            active = adb
        } else {
            active = null
        }
    }

    companion object {
        private const val TAG = "TransportManager"
    }
}
