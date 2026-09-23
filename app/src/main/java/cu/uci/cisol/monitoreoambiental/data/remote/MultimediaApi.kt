package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.data.remote.dto.MediaFileDto
import cu.uci.cisol.monitoreoambiental.data.util.postMultipartSafe
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class MultimediaApi(private val client: HttpClient) {

    companion object {
        val BASE_URL: String get() = "${ApiConfig.API_V1_BASE}/multimedia"
    }

    suspend fun uploadFile(
        bytes: ByteArray,
        filename: String,
        contentType: String = "image/jpeg",
    ): Result<MediaFileDto, DataError.Network> {
        val parts = formData {
            append(
                key = "file",
                value = bytes,
                headers = Headers.build {
                    append(
                        HttpHeaders.ContentDisposition,
                        "form-data; name=\"file\"; filename=\"$filename\"",
                    )
                    append(HttpHeaders.ContentType, contentType)
                },
            )
        }
        return client.postMultipartSafe("$BASE_URL/") { parts }
    }
}
