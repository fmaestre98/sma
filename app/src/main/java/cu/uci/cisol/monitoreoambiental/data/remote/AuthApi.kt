package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.data.remote.dto.*
import cu.uci.cisol.monitoreoambiental.data.util.postSafe
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.http.ContentType

class AuthApi(private val client: HttpClient) {

    // You should put the actual base url here, or use it from config
    companion object {
        const val BASE_URL = "http://10.0.2.2:8000/auth"
    }

    suspend fun login(request: LoginRequestDto): io.ktor.client.statement.HttpResponse {
        return client.post("$BASE_URL/token/") {
            setBody(request)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun register(request: RegisterRequestDto): Result<MessageResponseDto, DataError.Network> {
        return client.postSafe("$BASE_URL/register/", request)
    }
    
    suspend fun logout(request: LogoutRequestDto): Result<MessageResponseDto, DataError.Network> {
        return client.postSafe("$BASE_URL/logout/", request)
    }
    
    suspend fun terminateSessions(request: TerminateSessionsRequestDto): Result<MessageResponseDto, DataError.Network> {
        return client.postSafe("$BASE_URL/token/terminate-sessions/", request)
    }
}
