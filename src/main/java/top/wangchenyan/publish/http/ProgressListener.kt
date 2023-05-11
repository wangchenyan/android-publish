package top.wangchenyan.publish.http

interface ProgressListener {
    fun onProgress(percent: Int)
}