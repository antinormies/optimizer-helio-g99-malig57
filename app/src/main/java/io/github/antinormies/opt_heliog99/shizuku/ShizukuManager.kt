package io.github.antinormies.opt_heliog99.shizuku

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

class ShizukuManager {

    interface Listener {
        fun onBinderReady()
        fun onBinderDead()
        fun onPermissionResult(granted: Boolean)
    }

    private var listener: Listener? = null
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        listener?.onBinderReady()
        checkAndRequestPermission()
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        listener?.onBinderDead()
    }
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE) {
            listener?.onPermissionResult(grantResult == PackageManager.PERMISSION_GRANTED)
        }
    }

    val isShizukuAvailable: Boolean get() = try {
        Shizuku.getBinder()
        true
    } catch (_: Exception) {
        false
    }

    val hasPermission: Boolean get() = runCatching {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun init(l: Listener) {
        listener = l
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    fun destroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        listener = null
    }

    private fun checkAndRequestPermission() {
        if (hasPermission) {
            listener?.onPermissionResult(true)
        } else if (!Shizuku.shouldShowRequestPermissionRationale()) {
            requestPermission()
        }
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request permission: ${e.message}")
        }
    }

    fun handlePermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == REQUEST_CODE) {
            listener?.onPermissionResult(grantResult == PackageManager.PERMISSION_GRANTED)
        }
    }

    fun runShellCommandAsync(
        command: String,
        onOutput: (String) -> Unit = {},
        onError: (String) -> Unit = {},
        onComplete: (Int) -> Unit = {}
    ) {
        Thread {
            try {
                val process = Shizuku.newProcess(
                    arrayOf("sh", "-c", command),
                    null, null
                )

                val stdout = Thread {
                    process.inputStream.bufferedReader().use { reader ->
                        reader.lines().forEach { line -> onOutput(line) }
                    }
                }
                val stderr = Thread {
                    process.errorStream.bufferedReader().use { reader ->
                        reader.lines().forEach { line -> onError(line) }
                    }
                }

                stdout.start()
                stderr.start()
                stdout.join()
                stderr.join()

                val exitCode = process.waitFor()
                onComplete(exitCode)
            } catch (e: Exception) {
                onError("Error: ${e.message}")
                onComplete(-1)
            }
        }.start()
    }

    companion object {
        private const val TAG = "ShizukuManager"
        const val REQUEST_CODE = 1001
    }
}
