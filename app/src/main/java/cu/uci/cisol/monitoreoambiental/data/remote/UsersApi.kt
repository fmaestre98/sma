package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.data.remote.dto.UserResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.UpdateUserRequestDto
import cu.uci.cisol.monitoreoambiental.data.util.getSafe
import cu.uci.cisol.monitoreoambiental.data.util.patchSafe
import cu.uci.cisol.monitoreoambiental.data.util.patchMultipartSafe
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.http.content.PartData

class UsersApi(private val client: HttpClient) {

    companion object {
        const val BASE_URL = "${ApiConfig.BASE_URL}/api/v1/users"
    }

    suspend fun getMe(): Result<UserResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/me/")
    }

    suspend fun updateUser(
        id: String,
        body: UpdateUserRequestDto
    ): Result<UserResponseDto, DataError.Network> {
        return client.patchSafe("$BASE_URL/$id/", body)
    }

    suspend fun updateUserMultipart(
        id: String,
        formParts: List<PartData>
    ): Result<UserResponseDto, DataError.Network> {
        return client.patchMultipartSafe("$BASE_URL/$id/") { formParts }
    }
}
