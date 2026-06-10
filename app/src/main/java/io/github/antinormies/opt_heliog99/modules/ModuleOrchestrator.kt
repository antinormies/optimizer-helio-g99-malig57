package io.github.antinormies.opt_heliog99.modules

import android.content.Context
import android.util.Base64
import android.util.Log
import io.github.antinormies.opt_heliog99.shizuku.CommandTransport
import java.io.File

class ModuleOrchestrator(
    private val context: Context,
    private val transport: CommandTransport
) {
    private var extracted = false

    private fun listAssetFiles(): List<String> {
        val am = context.assets
        val files = mutableListOf<String>()
        fun walk(ap: String) {
            val entries = try { am.list(ap) } catch (_: Exception) { null }
            if (entries.isNullOrEmpty()) {
                files.add(ap)
            } else {
                for (e in entries) {
                    walk(if (ap.isEmpty()) e else "$ap/$e")
                }
            }
        }
        walk("")
        return files
    }

    private fun readAsset(path: String): ByteArray {
        return try {
            context.assets.open(path).use { it.readBytes() }
        } catch (_: Exception) {
            ByteArray(0)
        }
    }

    private fun ensureExtracted(onReady: () -> Unit) {
        if (extracted) {
            onReady()
            return
        }

        val files = listAssetFiles()
        val dir = SCRIPTS_DIR
        var idx = 0
        var ok = true

        fun next() {
            if (!ok || idx >= files.size) {
                extracted = true
                onReady()
                return
            }

            val ap = files[idx++]
            val content = readAsset(ap)
            if (content.isEmpty()) {
                next()
                return
            }

            val outFile = "$dir/$ap"
            val parent = File(outFile).parent!!
            val mkdir = "mkdir -p $parent; "
            val b64 = Base64.encodeToString(content, Base64.NO_WRAP)
            val cmd = "${mkdir}printf '%s' '$b64' | base64 -d > $outFile"

            transport.runShellCommandAsync(
                command = cmd,
                onOutput = {},
                onError = { line ->
                    if (line.isNotBlank()) Log.e(TAG, "extract stderr: $line")
                },
                onComplete = { exitCode ->
                    if (exitCode != 0) {
                        Log.e(TAG, "extract FAILED: $ap (exit $exitCode)")
                        ok = false
                    }
                    next()
                }
            )
        }

        transport.runShellCommandAsync(
            command = "mkdir -p $dir/modules $dir/config/profiles",
            onOutput = {},
            onError = { line ->
                if (line.isNotBlank()) Log.e(TAG, "mkdir stderr: $line")
            },
            onComplete = {
                idx = 0
                next()
            }
        )
    }

    fun runAll(
        profile: String,
        onLogLine: (String) -> Unit,
        onComplete: (Int) -> Unit
    ) {
        onLogLine("=== OptHelioG99 Optimizer ===")
        onLogLine("Profile: $profile")
        onLogLine("")

        ensureExtracted {
            val cmd = "ADB=\"\" sh $SCRIPTS_DIR/optimize.sh $profile 2>&1"

            transport.runShellCommandAsync(
                command = cmd,
                onOutput = { line ->
                    if (line.isNotBlank()) onLogLine("  $line")
                },
                onError = { line ->
                    if (line.isNotBlank()) onLogLine("  ERROR: $line")
                },
                onComplete = { exitCode ->
                    onLogLine("")
                    onLogLine(if (exitCode == 0) "=== Done ===" else "=== Failed (exit $exitCode) ===")
                    onComplete(exitCode)
                }
            )
        }
    }

    fun clearAll(
        onLogLine: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        onLogLine("=== OptHelioG99 Clear Optimizations ===")
        onLogLine("")

        ensureExtracted {
            val cmd = "ADB=\"\" sh $SCRIPTS_DIR/clear.sh 2>&1"

            transport.runShellCommandAsync(
                command = cmd,
                onOutput = { line ->
                    if (line.isNotBlank()) onLogLine("  $line")
                },
                onError = { line ->
                    if (line.isNotBlank()) onLogLine("  ERROR: $line")
                },
                onComplete = { exitCode ->
                    onLogLine("")
                    onLogLine(if (exitCode == 0) "=== Clear done ===" else "=== Clear failed (exit $exitCode) ===")
                    onComplete()
                }
            )
        }
    }

    companion object {
        private const val TAG = "ModuleOrchestrator"
        private const val SCRIPTS_DIR = "/data/local/tmp/opt_heliog99/scripts"
    }
}
