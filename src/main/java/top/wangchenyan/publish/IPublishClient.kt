package top.wangchenyan.publish

import java.io.File

interface IPublishClient {
    fun publish(apkFile: File, changeLog: String, password: String? = null): String?
}