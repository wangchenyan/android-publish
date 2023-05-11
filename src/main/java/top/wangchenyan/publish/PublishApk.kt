package top.wangchenyan.publish

import top.wangchenyan.publish.pgyer.PublishPgy
import top.wangchenyan.publish.utils.Log
import java.io.File

class PublishApk(
    private val type: ClientType,
    private val apiKey: String,
    private val password: String? = null,
) {
    private val publishClient: IPublishClient by lazy {
        when (type) {
            ClientType.Pgy -> {
                PublishPgy(apiKey)
            }
        }
    }

    fun publish(apkFile: File?, changeLog: String) {
        if (apkFile == null || apkFile.exists().not()) {
            throw IllegalArgumentException("Apk文件不存在！")
        }
        Log.i("开始上传Apk文件：${apkFile}")
        val downloadUrl = publishClient.publish(apkFile, changeLog, password)
        Log.i("上传成功！")
        Log.i("下载地址：$downloadUrl")
    }
}