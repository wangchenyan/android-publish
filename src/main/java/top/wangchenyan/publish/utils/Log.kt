package top.wangchenyan.publish.utils

/**
 * Created by wangchenyan.top on 2022/6/17.
 */
object Log {

    fun i(tag: String, message: String?) {
        i("[$tag] $message")
    }

    private fun i(message: String?) {
        println("[Publish] $message")
    }
}