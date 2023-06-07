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
        Log.i(TAG, "开始发布Apk文件: $apkFile")
        val res = publishClient.publish(apkFile, changeLog, password)
        if (res.isSuccessWithData()) {
            Log.i(TAG, "发布成功！下载地址: ${res.getDataOrThrow()}")
        } else {
            throw IllegalStateException(res.msg)
        }
    }

    companion object {
        private const val TAG = "PublishApk"
    }
}