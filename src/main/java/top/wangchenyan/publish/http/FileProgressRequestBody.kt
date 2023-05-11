package top.wangchenyan.publish.http

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.internal.closeQuietly
import okio.BufferedSink
import okio.Source
import okio.source
import java.io.File
import java.io.IOException

open class FileProgressRequestBody(
    private var file: File,
    private var listener: ProgressListener?
) : RequestBody() {
    private var fileSize: Long = file.length()
    private var currentPercent: Int = 0
    private var contentType: String = "application/octet-stream"

    override fun contentLength(): Long {
        return file.length()
    }

    override fun contentType(): MediaType? {
        return contentType.toMediaTypeOrNull()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        var source: Source? = null
        try {
            source = file.source()
            var total: Long = 0
            var read: Long
            while (source.read(sink.buffer(), SEGMENT_SIZE).also { read = it } != -1L) {
                total += read
                sink.flush()
                val percent = total * 100 / fileSize
                if (percent.toInt() != currentPercent) {
                    currentPercent = percent.toInt()
                    listener?.onProgress(currentPercent)
                }
            }
        } finally {
            source!!.closeQuietly()
        }
    }

    companion object {
        const val SEGMENT_SIZE: Long = 4 * 1024 // okio.Segment.SIZE
    }
}