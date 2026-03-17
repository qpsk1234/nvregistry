package com.example.nvregistry.util

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object ShellUtils {

    private const val TAG = "ShellUtils"

    private fun logD(msg: String) {
        if (DebugConfig.isEnabled) Log.d(TAG, msg)
    }

    private fun logE(msg: String, e: Throwable? = null) {
        if (DebugConfig.isEnabled) {
            if (e != null) Log.e(TAG, msg, e) else Log.e(TAG, msg)
        }
    }

    private fun logW(msg: String) {
        if (DebugConfig.isEnabled) Log.w(TAG, msg)
    }

    /**
     * 通常のrootコマンド実行（SET等に使用）
     * 別スレッドで出力を読みながらプロセス完了を待つ（デッドロック防止）
     */
    fun executeRootCommand(command: String): String {
        return try {
            logD("executeRootCommand: $command")
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)

            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            val outputBuffer = StringBuilder()
            val readerThread = Thread {
                try {
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        outputBuffer.append(line).append("\n")
                        logD("CMD out: $line")
                    }
                } catch (e: Exception) {
                    logE("Error reading CMD output", e)
                }
            }
            readerThread.start()

            val finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            readerThread.join(1000)
            if (!finished) process.destroyForcibly()

            logD("CMD full output:\n$outputBuffer")
            outputBuffer.toString()
        } catch (e: Exception) {
            logE("executeRootCommand exception", e)
            "Error: ${e.message}"
        }
    }

    /**
     * tail方式のGETコマンド実行。
     * 別スレッドで出力を読みながらwaitForすることでデッドロックを防止し、
     * 全出力を確実にキャプチャする。
     */
    fun executeGetNv(registryName: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)

            val script = buildString {
                appendLine("tail -f /dev/umts_router &")
                appendLine("TAIL_PID=\$!")
                appendLine("sleep 0.3")
                appendLine("echo 'AT+GOOGGETNV=\"$registryName\"\\r' > /dev/umts_router")
                appendLine("sleep 1.5")
                appendLine("kill \$TAIL_PID 2>/dev/null")
                appendLine("exit")
            }
            logD("GET script:\n$script")

            os.writeBytes(script)
            os.flush()
            os.close()

            val outputBuffer = StringBuilder()
            val readerThread = Thread {
                try {
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        outputBuffer.append(line).append("\n")
                        logD("GET line: [$line]")
                    }
                } catch (e: Exception) {
                    logE("Error reading GET output", e)
                }
            }
            readerThread.start()

            val finished = process.waitFor(6, java.util.concurrent.TimeUnit.SECONDS)
            readerThread.join(2000)
            if (!finished) {
                logW("GET process timed out, destroying")
                process.destroyForcibly()
            }

            val result = outputBuffer.toString()
            logD("GET complete output (${result.length} chars):\n$result")
            result.ifEmpty { "No response received" }
        } catch (e: Exception) {
            logE("executeGetNv exception", e)
            "Error executing GET: ${e.message}"
        }
    }

    /**
     * 指定インデックスへのSET + 直後のGETを一括実行
     */
    fun executeSetNvAtIndex(registryName: String, index: Int, byteStr: String): Pair<String, String> {
        logD("SET [$registryName] idx=$index val=$byteStr")
        val setCommand = "echo 'AT+GOOGSETNV=\"$registryName\",$index,\"$byteStr\"\\r' > /dev/umts_router & cat /dev/umts_router"
        val setResult = executeRootCommand(setCommand)
        logD("SET result: $setResult")
        val getResult = executeGetNv(registryName)
        return Pair(setResult, getResult)
    }

    fun executeSetNv(registryName: String, newValue: String): Pair<String, String> {
        return executeSetNvAtIndex(registryName, 0, newValue)
    }
}
