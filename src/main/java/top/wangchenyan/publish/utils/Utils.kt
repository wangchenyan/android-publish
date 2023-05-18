package top.wangchenyan.publish.utils

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader

object Utils {

    fun getGitLog(projectDir: File): String {
        val res = execCommand("git -C ${projectDir.path} log --oneline -5")
        if (res.isSuccess()) {
            val log = res.msg ?: ""
            return log.lines()
                .map {
                    val blankIndex = it.indexOf(' ')
                    "- ${it.substring(blankIndex + 1)}"
                }
                .joinToString("\n")
        } else {
            return ""
        }
    }

    fun execCommand(command: String?): CommonResult<Any> {
        var process: Process? = null
        var code = -1
        var message = ""
        Log.i("执行命令: \n==========>\n$command\n<==========")
        kotlin.runCatching {
            process = Runtime.getRuntime().exec(command)
            BufferedReader(InputStreamReader(process!!.inputStream)).useLines { lines ->
                message = lines.joinToString("\n")
                Log.i("输出: \n==========>\n$message\n<==========")
            }
            BufferedReader(InputStreamReader(process!!.errorStream)).useLines { lines ->
                val error = lines.joinToString("\n")
                Log.i("错误: \n==========>\n$error\n<==========")
            }
            code = process!!.waitFor()
        }.onSuccess {
            Log.i("执行成功")
        }.onFailure { e ->
            Log.i("执行失败, ${e.message}")
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

    fun md5(file: File): String {
        if (file.exists().not()) return ""
        val fis = java.io.FileInputStream(file)
        val md5: String = DigestUtils.md5Hex(IOUtils.toByteArray(fis))
        IOUtils.closeQuietly(fis)
        return md5
    }

    fun deleteFile(file: File) {
        if (file.exists().not()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                deleteFile(it)
            }
            file.delete()
        } else {
            file.delete()
        }
    }
}