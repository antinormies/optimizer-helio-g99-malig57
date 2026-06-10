package io.github.antinormies.opt_heliog99.shizuku

interface CommandTransport {
    interface Listener {
        fun onConnected()
        fun onDisconnected()
        fun onError(message: String)
    }

    val isAvailable: Boolean
    val hasPermission: Boolean
    val name: String  // "Shizuku" or "ADB"

    fun init(listener: Listener)
    fun destroy()

    fun runShellCommandAsync(
        command: String,
        onOutput: (String) -> Unit = {},
        onError: (String) -> Unit = {},
        onComplete: (Int) -> Unit = {}
    )
}
