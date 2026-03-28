package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.domain.model.AuthCredentials
import cu.uci.cisol.monitoreoambiental.domain.model.RegistrationRequest
import cu.uci.cisol.monitoreoambiental.domain.model.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApi(private val client: HttpClient) {

    // Using test endpoints
    companion object {
        const val BASE_URL = "https://api.test-monitoreo.com/auth"
    }

    suspend fun login(credentials: AuthCredentials): TokenResponse {
        return client
                .post("$BASE_URL/login") {
                    contentType(ContentType.Application.Json)
                    setBody(credentials)
                }
                .body()
    }

    suspend fun register(request: RegistrationRequest) {
        client.post("$BASE_URL/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
