package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.data.remote.dto.UserResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.UpdateUserRequestDto
import cu.uci.cisol.monitoreoambiental.data.util.getSafe
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.http.content.PartData
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.statement.HttpResponse

class UsersApi(private val client: HttpClient) {

    companion object {
        val BASE_URL: String get() = "${ApiConfig.BASE_URL}/api/v1/users"
    }

    suspend fun getMe(): Result<UserResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/me/")
    }

    suspend fun updateUser(
        id: String,
        body: UpdateUserRequestDto
    ): HttpResponse {
        return client.patch("$BASE_URL/$id/") {
            setBody(body)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun updateUserMultipart(
        id: String,
        formParts: List<PartData>
    ): HttpResponse {
        return client.patch("$BASE_URL/$id/") {
            setBody(io.ktor.client.request.forms.MultiPartFormDataContent(formParts))
        }
    }

    suspend fun changePassword(id: String, body: cu.uci.cisol.monitoreoambiental.data.remote.dto.ChangePasswordRequestDto): HttpResponse {
        return client.patch("$BASE_URL/$id/") {
            setBody(body)
            contentType(ContentType.Application.Json)
        }
    }
}
