package top.wangchenyan.publish.utils

import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader

object Utils {
    fun getGitLog(projectDir: File): String {
        val res = execCommand("git -C ${projectDir.path} log --oneline -5")
        return if (res.isSuccess()) {
            val log = res.msg ?: ""
            log.lines().joinToString("\n") {
                val blankIndex = it.indexOf(' ')
                "- ${it.substring(blankIndex + 1)}"
            }
        } else {
            ""
        }
    }

    fun execCommand(command: String?): CommonResult<Any> {
        val tag = "exec"
        var process: Process? = null
        var code = -1
        var message = ""
        Log.i(tag, "执行命令: \n==========>\n$command\n<==========")
        kotlin.runCatching {
            process = Runtime.getRuntime().exec(command)
            BufferedReader(InputStreamReader(process!!.inputStream)).useLines { lines ->
                message = lines.joinToString("\n")
                Log.i(tag, "输出: \n==========>\n$message\n<==========")
            }
            BufferedReader(InputStreamReader(process!!.errorStream)).useLines { lines ->
                val error = lines.joinToString("\n")
                Log.i(tag, "错误: \n==========>\n$error\n<==========")
            }
            code = process!!.waitFor()
        }.onSuccess {
            Log.i(tag, "执行成功")
        }.onFailure { e ->
            Log.i(tag, "执行失败, ${e.message}")
            e.printStackTrace()
        }
        process?.destroy()
        return CommonResult(code, message, 1)
    }

    fun closeIO(vararg closeables: Closeable?) {
        closeables.forEach {
            kotlin.runCatching {
                it?.close()
            }
        }
    }

    fun findApkFile(buildDir: File): File? {
        return findApkFiles(buildDir).firstOrNull()
    }

    fun findApkFiles(buildDir: File): List<File> {
        return findFilesByExt(File(buildDir, "outputs"), "apk")
    }

    fun findAabFile(buildDir: File): File? {
        return findFilesByExt(File(buildDir, "outputs"), "aab").firstOrNull()
    }

    fun findFilesByExt(path: File, ext: String): List<File> {
        val resultList = mutableListOf<File>()
        findFilesByExt(path, ext, resultList)
        return resultList
    }

    private fun findFilesByExt(path: File, ext: String, resultList: MutableList<File>) {
        if (path.isDirectory) {
            path.listFiles()?.forEach {
                findFilesByExt(it, ext, resultList)
            }
        } else {
            if (path.extension == ext) {
                resultList.add(path)
            }
        }
    }
}