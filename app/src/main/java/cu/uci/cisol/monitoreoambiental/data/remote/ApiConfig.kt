package cu.uci.cisol.monitoreoambiental.data.remote

object ApiConfig {
    const val ENABLE_DYNAMIC_URL = true
    var BASE_URL = "http://192.168.1.101:8000"

    // Este valor debe coincidir con el APK_SECRET_KEY del backend (ver .env)
    const val APP_SECRET = "apk_secret_key_1234567890"

    val AUTH_BASE: String get() = "$BASE_URL/api/v1/auth"
    val API_V1_BASE: String get() = "$BASE_URL/api/v1"

    /**
     * Media files in local Docker setup are served by nginx on :8080.
     * Absolute URLs from the API are used as-is; relative paths fall back to BASE_URL.
     */
    var MEDIA_BASE_URL: String? = null

    fun resolveMediaUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val base = MEDIA_BASE_URL ?: BASE_URL
        val path = if (url.startsWith("/")) url else "/$url"
        return "$base$path"
    }

    /** Paths that must not send Bearer (login/refresh/register/OTP bootstrap). */
    fun isAuthPublicPath(encodedPath: String): Boolean {
        return encodedPath.endsWith("/api/v1/auth/token/") ||
            encodedPath.contains("/api/v1/auth/token/refresh/") ||
            encodedPath.contains("/api/v1/auth/register/") ||
            encodedPath.contains("/api/v1/auth/verify-registration/") ||
            encodedPath.contains("/api/v1/auth/resend-otp/")
    }
}
