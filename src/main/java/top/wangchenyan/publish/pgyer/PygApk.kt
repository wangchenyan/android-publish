package top.wangchenyan.publish.pgyer

import com.google.gson.annotations.SerializedName

data class PygApk(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: PygApkData? = null,
) {
    data class PygApkData(
        @SerializedName("buildShortcutUrl")
        val buildShortcutUrl: String? = null
    ) {
        fun getFullUrl(): String = "https://www.pgyer.com/$buildShortcutUrl"
    }
}