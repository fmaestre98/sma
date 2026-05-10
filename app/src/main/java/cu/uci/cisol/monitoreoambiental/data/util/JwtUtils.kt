package cu.uci.cisol.monitoreoambiental.data.util

import android.util.Base64
import org.json.JSONObject

object JwtUtils {
    fun isTokenExpired(token: String?): Boolean {
        if (token == null) return true
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val jsonObject = JSONObject(payload)
            val exp = jsonObject.getLong("exp")
            val currentTime = System.currentTimeMillis() / 1000
            // Return true if expired or expiring in the next 30 seconds
            exp <= currentTime + 30
        } catch (e: Exception) {
            true
        }
    }
}
