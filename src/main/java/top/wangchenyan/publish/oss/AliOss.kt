package top.wangchenyan.publish.oss

import com.aliyun.oss.ClientBuilderConfiguration
import com.aliyun.oss.OSS
import com.aliyun.oss.OSSClientBuilder
import com.aliyun.oss.common.comm.Protocol
import com.aliyun.oss.event.ProgressEvent
import com.aliyun.oss.event.ProgressEventType
import com.aliyun.oss.event.ProgressListener
import com.aliyun.oss.model.PutObjectRequest
import top.wangchenyan.publish.utils.Log
import java.io.File
import java.util.Date

/**
 * 文档: https://help.aliyun.com/document_detail/32009.html
 * Created by wangchenyan.top on 2023/6/7.
 */
class AliOss(
    private val endpoint: String,
    private val accessKeyId: String,
    private val accessKeySecret: String,
    private val bucketName: String
) {

    fun uploadFile(path: String, file: File, validTime: Long): String {
        Log.i("上传文件: $file")
        if (file.exists().not()) {
            throw IllegalArgumentException("文件不存在")
        }
        if (file.isFile.not()) {
            throw IllegalArgumentException("仅支持文件类型")
        }
        val ossClient = getOssClient()
        try {
            val key = File(path, file.name).path
            val request = PutObjectRequest(bucketName, key, file)
                .withProgressListener<PutObjectRequest>(UploadProgressListener())
            ossClient.putObject(request)
            val expiration = Date(System.currentTimeMillis() + validTime)
            val downloadUrl = ossClient.generatePresignedUrl(bucketName, key, expiration)
            Log.i("上传成功, url: $downloadUrl")
            return downloadUrl.toString()
        } finally {
            ossClient.shutdown()
        }
    }

    private fun getOssClient(): OSS {
        val conf = ClientBuilderConfiguration()
        // 设置OSSClient允许打开的最大HTTP连接数，默认为1024个。
        conf.maxConnections = 200
        // 设置Socket层传输数据的超时时间，默认为50000毫秒。
        conf.socketTimeout = 10000
        // 设置建立连接的超时时间，默认为50000毫秒。
        conf.connectionTimeout = 10000
        // 设置从连接池中获取连接的超时时间（单位：毫秒），默认不超时。
        conf.connectionRequestTimeout = 1000
        // 设置连接空闲超时时间。超时则关闭连接，默认为60000毫秒。
        conf.idleConnectionTime = 10000
        // 设置失败请求重试次数，默认为3次。
        conf.maxErrorRetry = 5
        // 设置是否支持将自定义域名作为Endpoint，默认支持。
        conf.isSupportCname = true
        // 设置是否开启二级域名的访问方式，默认不开启。
        conf.isSLDEnabled = false
        // 设置连接OSS所使用的协议（HTTP或HTTPS），默认为HTTP。
        conf.protocol = Protocol.HTTP
        // 设置用户代理，指HTTP的User-Agent头，默认为aliyun-sdk-java。
        conf.userAgent = "aliyun-sdk-java"
        // 设置是否开启HTTP重定向，默认开启。
        conf.isRedirectEnable = true
        // 设置是否开启SSL证书校验，默认开启。
        conf.isVerifySSLEnable = true
        return OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf)
    }

    class UploadProgressListener : ProgressListener {
        private var totalBytes: Long = 0
        private var bytesWritten: Long = 0
        private var progress: Int = 0

        override fun progressChanged(progressEvent: ProgressEvent) {
            val bytes = progressEvent.bytes
            when (progressEvent.eventType) {
                ProgressEventType.TRANSFER_STARTED_EVENT -> {
                    Log.i("开始上传")
                }

                ProgressEventType.REQUEST_CONTENT_LENGTH_EVENT -> {
                    totalBytes = bytes
                    Log.i("文件大小: $totalBytes")
                }

                ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT -> {
                    bytesWritten += bytes
                    val currentProgress = (bytesWritten * 100f / totalBytes).toInt()
                    if (currentProgress != progress) {
                        progress = currentProgress
                        Log.i("上传进度: $progress%")
                    }
                }

                ProgressEventType.TRANSFER_COMPLETED_EVENT -> {
                    Log.i("上传完成")
                }

                ProgressEventType.TRANSFER_FAILED_EVENT -> {
                    Log.i("上传失败")
                }

                else -> {}
            }
        }
    }
}