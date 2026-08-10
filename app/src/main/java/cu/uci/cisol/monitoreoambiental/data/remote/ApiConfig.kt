package cu.uci.cisol.monitoreoambiental.data.remote

object ApiConfig {
    const val ENABLE_DYNAMIC_URL = true
    var BASE_URL = "http://192.168.1.101:8000"
    
    // Este valor debe coincidir con el APK_SECRET_KEY del backend (ver .env)
    const val APP_SECRET = "apk_secret_key_1234567890" 
}
