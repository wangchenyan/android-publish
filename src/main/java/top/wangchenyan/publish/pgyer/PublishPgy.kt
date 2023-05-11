package top.wangchenyan.publish.pgyer

import top.wangchenyan.publish.IPublishClient
import top.wangchenyan.publish.http.HttpClient
import top.wangchenyan.publish.http.ProgressListener
import top.wangchenyan.publish.utils.Log
import java.io.File

/**
 * 接口说明：https://www.pgyer.com/doc/view/api#uploadApp
 */
class PublishPgy(private val apiKey: String) : IPublishClient {

    override fun publish(apkFile: File, changeLog: String, password: String?): String? {
        val pgyApk = uploadPgy(apkFile, changeLog, password)
        return pgyApk?.data?.getFullUrl()
    }

    private fun uploadPgy(apkFile: File, changeLog: String, password: String?): PygApk? {
        val pygApk = HttpClient("https://www.pgyer.com/apiv2/app/upload")
            .post()
            .addParams("_api_key", apiKey)
            .apply {
                if (password?.isNotEmpty() == true) {
                    // (选填)应用安装方式，值为(1,2,3，默认为1 公开安装)。1：公开安装，2：密码安装，3：邀请安装
                    addParams("buildInstallType", "2")
                    // (选填) 设置App安装密码，密码为空时默认公开安装
                    addParams("buildPassword", password)
                }
            }
            .addParams("buildUpdateDescription", changeLog)
            .addFile("file", apkFile, object : ProgressListener {
                override fun onProgress(percent: Int) {
                    Log.i("上传进度：$percent%")
                }
            })
            .start(PygApk::class.java)
        if (pygApk?.data != null && pygApk.code == 0) {
            Log.i("上传蒲公英成功！")
        } else {
            throw RuntimeException("上传蒲公英失败：" + pygApk?.message)
        }
        return pygApk
    }
}