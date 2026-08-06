package com.wxregion.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

object ShizukuShell {
    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Exception) {
        false
    }

    suspend fun exec(command: String): ShellResult {
        if (!isAvailable()) {
            return ShellResult(false, "", "Shizuku 未运行")
        }
        return suspendCancellableCoroutine { cont ->
            try {
                val process = newShellProcess(command)
                val stdout = process.inputStream.bufferedReader().readText()
                val stderr = process.errorStream.bufferedReader().readText()
                val code = process.waitFor()
                cont.resume(ShellResult(code == 0, stdout.trim(), stderr.trim()))
            } catch (e: Exception) {
                cont.resume(ShellResult(false, "", e.message ?: "Shizuku 执行失败"))
            }
        }
    }

    @Suppress("DiscouragedPrivateApi")
    private fun newShellProcess(command: String): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(
            null,
            arrayOf("sh", "-c", command),
            null,
            null,
        ) as Process
    }
}
