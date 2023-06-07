package top.wangchenyan.publish.jiagu

import com.tencentcloudapi.common.Credential
import com.tencentcloudapi.common.profile.ClientProfile
import com.tencentcloudapi.common.profile.HttpProfile
import com.tencentcloudapi.ms.v20180408.MsClient
import com.tencentcloudapi.ms.v20180408.models.CreateShieldInstanceRequest
import com.tencentcloudapi.ms.v20180408.models.DescribeShieldResultRequest
import top.wangchenyan.publish.http.HttpClient
import top.wangchenyan.publish.utils.Log
import top.wangchenyan.publish.utils.Utils
import java.io.File

/**
 * 教程: https://cloud.tencent.com/developer/article/1376347
 * Created by wangchenyan.top on 2023/6/7.
 */
class TencentJiagu(
    private val secretId: String,
    private val secretKey: String
) {

    private val client by lazy {
        MsClient(
            Credential(secretId, secretKey),
            "",
            ClientProfile().apply {
                httpProfile = HttpProfile().apply {
                    endpoint = "ms.tencentcloudapi.com"
                }
            }
        )
    }

    fun jiagu(file: File, url: String): File {
        Log.i(TAG, "开始加固")
        val md5 = Utils.md5(file)
        Log.i(TAG, "原始文件MD5: $md5")
        val itemId = uploadFile(url, md5)
        val result = getResult(itemId)
        val outFile = File(file.parent, "${file.nameWithoutExtension}-legu.apk")
        download(result.first, result.second, outFile)
        return outFile
    }

    /**
     * 文档: https://cloud.tencent.com/document/product/283/17753
     */
    private fun uploadFile(url: String, md5: String): String {
        val params = "{" +
                "\"AppInfo\":" +
                "{\"AppUrl\":\"${url}\",\"AppMd5\":\"${md5}\"}" + "," +
                "\"ServiceInfo\":" +
                "{\"ServiceEdition\":\"basic\",\"CallbackUrl\":\"\",\"SubmitSource\":\"api\",\"PlanId\":0}" +
                "}"
        val response = client.CreateShieldInstance(
            CreateShieldInstanceRequest.fromJsonString(
                params,
                CreateShieldInstanceRequest::class.java
            )
        )
        val itemId = response.itemId
        if (itemId.isNullOrEmpty()) {
            throw IllegalStateException("itemId为空")
        }
        Log.i(TAG, "itemId: $itemId")
        return itemId
    }

    /**
     * 文档: https://cloud.tencent.com/document/product/283/17750
     * - status: 任务状态: 0-请返回,1-已完成,2-处理中,3-处理出错,4-处理超时
     */
    private fun getResult(itemId: String): Pair<String, String> {
        val params = "{\"ItemId\":\"${itemId}\"}"
        while (true) {
            val res = client.DescribeShieldResult(
                DescribeShieldResultRequest.fromJsonString(
                    params,
                    DescribeShieldResultRequest::class.java
                )
            )
            when (res.taskStatus) {
                1L -> {
                    val pair = Pair(res.shieldInfo.appUrl, res.shieldInfo.shieldMd5)
                    Log.i(TAG, "加固成功, url: ${pair.first}, md5: ${pair.second}")
                    return pair
                }

                0L, 2L -> {
                    Log.i(TAG, "加固中，等待10s后再次查询")
                    Thread.sleep(10000)
                }

                else -> {
                    throw IllegalStateException("加固失败, status: ${res.taskStatus}")
                }
            }
        }
    }

    private fun download(url: String, md5: String, file: File) {
        Log.i(TAG, "下载加固包")
        val res = HttpClient.get(url).download(file)
        if (res.isSuccess()) {
            val fileMd5 = Utils.md5(file)
            if (fileMd5 != md5) {
                throw IllegalStateException("文件MD5不匹配")
            }
            Log.i(TAG, "下载成功: $file")
        } else {
            throw IllegalStateException("下载失败")
        }
    }

    companion object {
        private const val TAG = "TencentJiagu"
    }
}