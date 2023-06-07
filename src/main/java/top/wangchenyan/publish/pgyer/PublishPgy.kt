package top.wangchenyan.publish.pgyer

import top.wangchenyan.publish.IPublishClient
import top.wangchenyan.publish.http.FileUploader
import top.wangchenyan.publish.http.HttpClient
import top.wangchenyan.publish.pgyer.bean.PgyApkData
import top.wangchenyan.publish.pgyer.bean.TokenData
import top.wangchenyan.publish.utils.CommonResult
import top.wangchenyan.publish.utils.Log
import java.io.File

/**
 * 接口说明: https://www.pgyer.com/doc/view/api#fastUploadApp
 */
class PublishPgy(private val apiKey: String) : IPublishClient {
    private val fileUploader = FileUploader()

    override fun publish(
        apkFile: File,
        changeLog: String,
        password: String?
    ): CommonResult<String> {
        Log.i(TAG, "开始发布蒲公英")
        val tokenRes = getToken(changeLog, password)
        val token = tokenRes.data
        return if (tokenRes.isSuccessWithData() && token!!.isSuccessWithData()) {
            Log.i(TAG, "获取上传 token 成功，开始上传文件")
            val uploadRes = uploadApk(token.data!!, apkFile)
            if (uploadRes.isSuccess()) {
                Log.i(TAG, "上传成功，等待5s后获取上传结果")
                Thread.sleep(5000)
                val buildInfoRes = getBuildInfo(token.data.params.key)
                val buildInfo = buildInfoRes.data
                if (buildInfoRes.isSuccessWithData() && buildInfo!!.isSuccessWithData()) {
                    CommonResult.success(buildInfo.data!!.getFullUrl())
                } else {
                    Log.i(TAG, "发布失败, code: ${buildInfoRes.code}, message: ${buildInfoRes.msg}")
                    CommonResult.fail(buildInfoRes.code, buildInfoRes.msg)
                }
            } else {
                Log.i(TAG, "文件上传失败, code: ${uploadRes.code}, message: ${uploadRes.msg}")
                CommonResult.fail(uploadRes.code, uploadRes.msg)
            }
        } else {
            Log.i(TAG, "获取上传 token 失败, code: ${tokenRes.code}, message: ${tokenRes.msg}")
            CommonResult.fail(tokenRes.code, tokenRes.msg)
        }
    }

    private fun getToken(
        changeLog: String,
        password: String?
    ): CommonResult<TokenData> {
        return HttpClient.post(GET_TOKEN_URL)
            .addFormParams("_api_key", apiKey)
            .addFormParams("buildType", "apk")
            .apply {
                if (password?.isNotEmpty() == true) {
                    // (选填)应用安装方式，值为(1,2,3，默认为1 公开安装)。1: 公开安装，2: 密码安装，3: 邀请安装
                    addFormParams("buildInstallType", "2")
                    // (选填) 设置App安装密码，密码为空时默认公开安装
                    addFormParams("buildPassword", password)
                }
            }
            .addFormParams("buildUpdateDescription", changeLog)
            .request()
    }

    private fun uploadApk(token: TokenData.Data, apkFile: File): CommonResult<Any> {
        val params = mapOf(
            Pair("key", token.params.key),
            Pair("signature", token.params.signature),
            Pair("x-cos-security-token", token.params.xCosSecurityToken),
        )
        return fileUploader.uploadFile(
            token.endpoint,
            params,
            apkFile,
            object : FileUploader.ProgressListener {
                override fun onProgress(progress: Int) {
                    Log.i(TAG, "上传进度: $progress%")
                }
            }
        )
    }

    private fun getBuildInfo(key: String): CommonResult<PgyApkData> {
        return HttpClient.get(GET_BUILD_INFO_URL)
            .addQueryParams("_api_key", apiKey)
            .addQueryParams("buildKey", key)
            .request()
    }

    companion object {
        private const val TAG = "PublishPgy"
        private const val BASE_URL = "https://www.pgyer.com/apiv2/app"
        private const val GET_TOKEN_URL = "$BASE_URL/getCOSToken"
        private const val GET_BUILD_INFO_URL = "$BASE_URL/buildInfo"
    }
}