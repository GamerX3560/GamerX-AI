package com.gamerx.ai.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val timedOut: Boolean = false
)

object ShellExecutor {

    private const val TAG = "ShellExecutor"
    private const val DEFAULT_TIMEOUT_MS = 15_000L
    private const val MAX_OUTPUT_CHARS = 8000 // Prevent flooding the AI context

    /**
     * Execute a shell command. If [useRoot] is true, the command is wrapped in `su -c "..."`.
     * Returns a [ShellResult] with captured stdout, stderr, exit code, and duration.
     */
    suspend fun execute(
        command: String,
        useRoot: Boolean = false,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): ShellResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        val shellCommand = if (useRoot) {
            arrayOf("su", "-c", command)
        } else {
            arrayOf("sh", "-c", command)
        }

        Log.d(TAG, "Executing${if (useRoot) " [ROOT]" else ""}: $command")

        try {
            val process = ProcessBuilder(*shellCommand)
                .redirectErrorStream(false)
                .start()

            val result = withTimeoutOrNull(timeoutMs) {
                val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
                val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

                val stdout = StringBuilder()
                val stderr = StringBuilder()

                // Read stdout
                val stdoutThread = Thread {
                    try {
                        var line: String?
                        while (stdoutReader.readLine().also { line = it } != null) {
                            if (stdout.length < MAX_OUTPUT_CHARS) {
                                stdout.appendLine(line)
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Read stderr
                val stderrThread = Thread {
                    try {
                        var line: String?
                        while (stderrReader.readLine().also { line = it } != null) {
                            if (stderr.length < MAX_OUTPUT_CHARS) {
                                stderr.appendLine(line)
                            }
                        }
                    } catch (_: Exception) {}
                }

                stdoutThread.start()
                stderrThread.start()

                val exitCode = process.waitFor()
                stdoutThread.join(2000)
                stderrThread.join(2000)

                val duration = System.currentTimeMillis() - startTime

                var stdoutStr = stdout.toString().trim()
                var stderrStr = stderr.toString().trim()

                if (stdoutStr.length >= MAX_OUTPUT_CHARS) {
                    stdoutStr = stdoutStr.take(MAX_OUTPUT_CHARS) + "\n... [output truncated]"
                }
                if (stderrStr.length >= MAX_OUTPUT_CHARS) {
                    stderrStr = stderrStr.take(MAX_OUTPUT_CHARS) + "\n... [output truncated]"
                }

                Log.d(TAG, "Exit code: $exitCode, Duration: ${duration}ms, Stdout: ${stdoutStr.length} chars")

                ShellResult(
                    exitCode = exitCode,
                    stdout = stdoutStr,
                    stderr = stderrStr,
                    durationMs = duration
                )
            }

            if (result == null) {
                process.destroyForcibly()
                val duration = System.currentTimeMillis() - startTime
                Log.w(TAG, "Command timed out after ${duration}ms: $command")
                ShellResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "Command timed out after ${timeoutMs}ms",
                    durationMs = duration,
                    timedOut = true
                )
            } else {
                result
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Shell execution failed: ${e.message}", e)
            ShellResult(
                exitCode = -1,
                stdout = "",
                stderr = "Execution failed: ${e.message}",
                durationMs = duration
            )
        }
    }
}
