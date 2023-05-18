package top.wangchenyan.publish.pgyer.bean

import com.google.gson.annotations.SerializedName

/**
 * Created by wangchenyan.top on 2023/5/18.
 */
data class PgyApkData(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: Data? = null,
) {
    fun isSuccessWithData(): Boolean = (code == 0 && data != null)

    data class Data(
        @SerializedName("buildShortcutUrl")
        val buildShortcutUrl: String? = null
    ) {
        fun getFullUrl(): String = "https://www.pgyer.com/$buildShortcutUrl"
    }
}
