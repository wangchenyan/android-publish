package top.wangchenyan.publish.http

import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import top.wangchenyan.publish.utils.CommonResult
import top.wangchenyan.publish.utils.Log
import top.wangchenyan.publish.utils.Utils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class HttpClient private constructor(
    private val url: String,
    private val method: RequestMethod
) {
    private val queryParams: MutableMap<String, Any> = mutableMapOf()
    private val formParams: MutableMap<String, Any> = mutableMapOf()
    private val files: MutableMap<String, Pair<File, ProgressListener?>> = mutableMapOf()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.MINUTES)
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .build()
    }
    val gson: Gson by lazy { Gson() }

    fun addQueryParams(key: String, value: Any) = apply {
        queryParams[key] = value
    }

    fun addFormParams(key: String, value: Any) = apply {
        formParams[key] = value
    }

    fun addFile(key: String, file: File, listener: ProgressListener? = null) = apply {
        files[key] = Pair(file, listener)
    }

    fun download(file: File): CommonResult<Any> {
        val url = getUrlWithQueryParams()
        Log.i("Start download url: $url")
        val builder = Request.Builder()
        builder.url(url)
        var response: Response? = null
        return try {
            response = client.newCall(builder.build()).execute()
            if (response.isSuccessful) {
                saveFile(response, file)
            } else {
                CommonResult.fail(response.code, response.message)
            }
        } catch (e: Exception) {
            Log.i("download error, ${e.message}")
            e.printStackTrace()
            CommonResult.fail(msg = e.message)
        } finally {
            response?.close()
        }
    }

    fun requestRaw(): CommonResult<String> {
        val url = getUrlWithQueryParams()
        Log.i("Start request url: $url")
        val builder = Request.Builder()
        builder.url(url)
        when (method) {
            RequestMethod.PUT -> {
                builder.put(createRequestBody())
            }

            RequestMethod.POST -> {
                builder.post(createRequestBody())
            }

            else -> {
                builder.get()
            }
        }
        var response: Response? = null
        return try {
            response = client.newCall(builder.build()).execute()
            val body = response.body?.string()
            Log.i("Response code: ${response.code}, body: $body")
            CommonResult(response.code, "", body)
        } catch (e: Exception) {
            Log.i("request error, ${e.message}")
            e.printStackTrace()
            CommonResult.fail(msg = e.message)
        } finally {
            response?.close()
        }
    }

    inline fun <reified T> request(): CommonResult<T> {
        val res = requestRaw()
        return if (res.code in 200..299 && res.data != null) {
            val data = gson.fromJson(res.data, T::class.java)
            CommonResult.success(data)
        } else {
            CommonResult.fail(res.code, res.msg)
        }
    }

    private fun getUrlWithQueryParams(): String {
        val sb = StringBuilder(url)
        queryParams.forEach { (key, value) ->
            if (sb.contains("?")) {
                sb.append("&")
            } else {
                sb.append("?")
            }
            sb.append(
                "${key}=${
                    URLEncoder.encode(
                        value.toString(),
                        StandardCharsets.UTF_8.toString()
                    )
                }"
            )
        }
        return sb.toString()
    }

    private fun createRequestBody(): RequestBody {
        val body = MultipartBody.Builder()
        body.setType(MultipartBody.FORM)
        files.forEach { (key, value) ->
            body.addFormDataPart(
                key,
                value.first.name,
                FileProgressRequestBody(value.first, value.second)
            )
        }
        formParams.forEach { (key, value) ->
            body.addFormDataPart(key, value.toString())
        }
        return body.build()
    }

    private fun saveFile(response: Response, file: File): CommonResult<Any> {
        var inputStream: InputStream? = null
        var current = 0L
        val buf = ByteArray(4096)
        var fos: FileOutputStream? = null
        try {
            inputStream = response.body!!.byteStream()
            val total = response.body!!.contentLength()
            var progress = 0
            fos = FileOutputStream(file)
            var len: Int
            while (inputStream.read(buf).also { len = it } != -1) {
                current += len.toLong()
                fos.write(buf, 0, len)
                if (total > 0) {
                    val currentProgress = (current * 100f / total).toInt()
                    if (currentProgress != progress) {
                        progress = currentProgress
                        Log.i("下载进度: $progress%")
                    }
                }
            }
            fos.flush()
            return CommonResult.success(0)
        } catch (e: IOException) {
            Log.i("saveFile error, ${e.message}")
            e.printStackTrace()
            return CommonResult.fail(msg = e.message)
        } finally {
            Utils.closeIO(inputStream, fos)
        }
    }

    companion object {
        fun get(url: String): HttpClient {
            return HttpClient(url, RequestMethod.GET)
        }

        fun post(url: String): HttpClient {
            return HttpClient(url, RequestMethod.POST)
        }

        fun put(url: String): HttpClient {
            return HttpClient(url, RequestMethod.PUT)
        }
    }

    sealed class RequestMethod(val value: String) {
        object GET : RequestMethod("GET")
        object POST : RequestMethod("POST")
        object PUT : RequestMethod("PUT")
    }
}