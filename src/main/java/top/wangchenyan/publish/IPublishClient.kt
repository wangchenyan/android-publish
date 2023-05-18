package top.wangchenyan.publish

import top.wangchenyan.publish.utils.CommonResult
import java.io.File

interface IPublishClient {
    fun publish(apkFile: File, changeLog: String, password: String? = null): CommonResult<String>
}