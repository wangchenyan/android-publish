package top.wangchenyan.publish.http

import com.google.gson.Gson
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import top.wangchenyan.publish.utils.Log
import top.wangchenyan.publish.utils.Utils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class HttpClient(private val url: String) {
    private val params: MutableMap<String, Any> = mutableMapOf()
    private val files: MutableMap<String, Pair<File, ProgressListener?>> = mutableMapOf()
    private var method: RequestMethod = RequestMethod.GET

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.MINUTES)
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .build()
    }
    private val gson: Gson by lazy { Gson() }

    fun get(): HttpClient = apply {
        method = RequestMethod.GET
    }

    fun post(): HttpClient = apply {
        method = RequestMethod.POST
    }

    fun put(): HttpClient = apply {
        method = RequestMethod.PUT
    }

    fun addParams(key: String, value: Any): HttpClient {
        params[key] = value
        return this
    }

    fun addFile(key: String, file: File, listener: ProgressListener? = null): HttpClient {
        files[key] = Pair(file, listener)
        return this
    }

    @Throws(RuntimeException::class)
    fun download(file: File) {
        Log.i("Start download url: $url")
        val builder = Request.Builder()
        builder.url(url)
        var response: Response? = null
        try {
            response = client.newCall(builder.build()).execute()
            if (response.isSuccessful) {
                saveFile(response, file)
            } else {
                throw RuntimeException("Download fail, code=${response.code}, msg=${response.message}")
            }
        } finally {
            response?.close()
        }
    }

    @Throws(RuntimeException::class)
    fun <T> start(clazz: Class<T>): T? {
        Log.i("Start request url: $url")
        val builder = Request.Builder()
        builder.url(url)
        if (method == RequestMethod.PUT) {
            builder.put(createRequestBody())
        } else if (method == RequestMethod.POST) {
            builder.post(createRequestBody())
        } else {
            builder.get()
        }
        var response: Response? = null
        try {
            response = client.newCall(builder.build()).execute()
            if (response.isSuccessful) {
                val result = response.body!!.string()
                Log.i("Response: $result")
                return gson.fromJson(result, clazz)
            } else {
                throw RuntimeException("Request fail, code=${response.code}, msg=${response.message}")
            }
        } finally {
            response?.close()
        }
    }

    private fun createRequestBody(): RequestBody {
        val body = MultipartBody.Builder()
        body.setType(MultipartBody.FORM)
        files.forEach { key, value ->
            body.addFormDataPart(
                key,
                value.first.name,
                FileProgressRequestBody(value.first, value.second)
            )
        }
        params.forEach { key, value ->
            body.addFormDataPart(key, value.toString())
        }
        return body.build()
    }

    private fun saveFile(response: Response, file: File): String? {
        var filePath: String? = null
        var inputStream: InputStream? = null
        var current = 0L
        val buf = ByteArray(3072)
        var fos: FileOutputStream? = null
        try {
            inputStream = response.body!!.byteStream()
            val total = response.body!!.contentLength()
            fos = FileOutputStream(file)
            var len: Int
            while (inputStream.read(buf).also { len = it } != -1) {
                current += len.toLong()
                fos.write(buf, 0, len)
                val percent =
                    if (total == 0L) 0.0f else current.toFloat() * 100.0f / total.toFloat()
                if (percent % 10 == 0f) {
                    Log.i("下载进度：$percent%")
                }
            }
            fos.flush()
            filePath = file.absolutePath
        } catch (e: Exception) {
            RuntimeException(e)
        } finally {
            Utils.closeIO(inputStream, fos)
            return filePath
        }
    }

    sealed class RequestMethod(val value: String) {
        object GET : RequestMethod("GET")
        object POST : RequestMethod("POST")
        object PUT : RequestMethod("PUT")
    }
}