package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.data.remote.dto.LocalidadesResponseDto
import cu.uci.cisol.monitoreoambiental.data.util.getSafe
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient

class LocationsApi(private val client: HttpClient) {

    companion object {
        val BASE_URL: String get() = "${ApiConfig.BASE_URL}/api/v1/locations"
    }

    suspend fun getLocalities(): Result<LocalidadesResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/localities/?is_active=true")
    }
}
