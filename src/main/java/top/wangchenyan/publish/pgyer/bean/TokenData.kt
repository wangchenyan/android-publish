package top.wangchenyan.publish.pgyer.bean

import com.google.gson.annotations.SerializedName

/**
 * Created by wangchenyan.top on 2023/5/18.
 */
data class TokenData(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: Data? = null,
) {
    fun isSuccessWithData(): Boolean = (code == 0 && data != null)

    data class Data(
        @SerializedName("endpoint")
        val endpoint: String = "",
        @SerializedName("params")
        val params: Params = Params(),
    ) {
        data class Params(
            @SerializedName("key")
            val key: String = "",
            @SerializedName("signature")
            val signature: String = "",
            @SerializedName("x-cos-security-token")
            val xCosSecurityToken: String = "",
        )
    }
}
