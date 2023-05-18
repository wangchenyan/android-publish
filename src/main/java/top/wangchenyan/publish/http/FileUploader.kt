package top.wangchenyan.publish.http

import org.apache.http.HttpEntity
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.HttpEntityWrapper
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClientBuilder
import top.wangchenyan.publish.utils.CommonResult
import java.io.File
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Copy from https://github.com/PGYER/pgyer_api_example/blob/main/java-demo/AppUploadDemo.java
 * Created by wangchenyan.top on 2023/5/18.
 */
class FileUploader {

    @Throws(IOException::class)
    fun uploadFile(
        url: String,
        params: Map<String, String>,
        file: File,
        callback: ProgressListener,
    ): CommonResult<Any> {
        // 开启一个客户端 HTTP 请求
        val client: HttpClient = HttpClientBuilder.create().build()
        // 创建 HTTP POST 请求
        val post = HttpPost(url)
        val builder = MultipartEntityBuilder.create()
        // 设置浏览器兼容模式
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        for ((key, value) in params) {
            // 设置请求参数
            builder.addTextBody(key, value)
        }
        builder.addBinaryBody("file", file)
        // 生成 HTTP POST 实体
        val entity = builder.build()
        // 设置请求参数
        post.entity = ProgressHttpEntityWrapper(entity, callback)
        // 发起请求 并返回请求的响应
        val response = client.execute(post)
        val httpCode = response.statusLine.statusCode
        return if (httpCode == 204) {
            CommonResult.success(0)
        } else {
            CommonResult.fail(httpCode, response.statusLine.reasonPhrase)
        }
    }

    class ProgressHttpEntityWrapper(
        entity: HttpEntity,
        private val callback: ProgressListener
    ) : HttpEntityWrapper(entity) {

        @Throws(IOException::class)
        override fun writeTo(out: OutputStream) {
            wrappedEntity.writeTo(
                out as? ProgressFilterOutputStream
                    ?: ProgressFilterOutputStream(out, callback, contentLength)
            )
        }

        class ProgressFilterOutputStream internal constructor(
            out: OutputStream?,
            private val callback: ProgressListener,
            private val totalBytes: Long
        ) : FilterOutputStream(out) {
            private var transferred: Long = 0
            private var currentProgress = -1

            @Throws(IOException::class)
            override fun write(b: ByteArray, off: Int, len: Int) {
                out.write(b, off, len)
                transferred += len.toLong()
                val progress = getCurrentProgress()
                if (progress != currentProgress) {
                    currentProgress = progress
                    callback.onProgress(currentProgress)
                }
            }

            @Throws(IOException::class)
            override fun write(b: Int) {
                out.write(b)
                transferred++
                val progress = getCurrentProgress()
                if (progress != currentProgress) {
                    currentProgress = progress
                    callback.onProgress(currentProgress)
                }
            }

            private fun getCurrentProgress(): Int {
                return (transferred.toFloat() / totalBytes * 100).toInt()
            }
        }
    }

    interface ProgressListener {
        fun onProgress(progress: Int)
    }
}