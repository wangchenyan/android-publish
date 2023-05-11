package top.wangchenyan.publish.utils

/**
 * Created by wangchenyan.top on 2023/5/11.
 */
data class CommonResult<T>(
    val code: Int,
    val msg: String?,
    val data: T?
) {
    fun isSuccess(): Boolean = (code == CODE_SUCCESS)

    fun isSuccessWithData(): Boolean = (code == CODE_SUCCESS && data != null)

    fun getDataOrThrow(): T = data!!

    companion object {
        private const val CODE_SUCCESS = 0

        fun <T> success(data: T): CommonResult<T> {
            return CommonResult(200, null, data)
        }

        fun <T> fail(code: Int = -1, msg: String? = null): CommonResult<T> {
            val c = if (code == 200) -1 else code
            return CommonResult(c, msg, null)
        }
    }
}