package top.wangchenyan.publish.sign

import top.wangchenyan.publish.utils.Log
import top.wangchenyan.publish.utils.Utils
import java.io.File

/**
 * 文档: https://developer.android.com/studio/command-line/apksigner?hl=zh-cn
 * Created by wangchenyan.top on 2023/6/7.
 */
class ApkSigner(
    private val keyFilePath: String,
    private val alias: String,
    private val password: String
) {

    fun sign(file: File): File {
        val buildTools = System.getenv("ANDROID_BUILD_TOOLS_HOME")
        if (buildTools.isNullOrEmpty()) {
            throw IllegalStateException("Can not get env: ANDROID_BUILD_TOOLS_HOME, Please set it first")
        }
        Log.i("开始签名: $file")
        val outPath = File(file.parentFile, "${file.nameWithoutExtension}-sign.apk").path
        val command = StringBuilder().apply {
            append("${buildTools}${File.separator}apksigner sign")
            append(" --ks $keyFilePath")
            append(" --ks-key-alias $alias")
            append(" --ks-pass pass:$password")
            append(" --v4-signing-enabled false")
            append(" --out $outPath")
            append(" $file")
        }
        val res = Utils.execCommand(command.toString())
        if (res.isSuccess()) {
            Log.i("签名成功: $outPath")
            return File(outPath)
        } else {
            throw IllegalStateException("apksigner exec fail: ${res.msg}")
        }
    }
}