package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.data.remote.dto.LocalidadesResponseDto
import cu.uci.cisol.monitoreoambiental.data.util.getSafe
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient

class LocationsApi(private val client: HttpClient) {

    companion object {
        const val BASE_URL = "http://192.168.1.101:8000/api/v1/locations"
    }

    suspend fun getLocalities(): Result<LocalidadesResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/localities/?is_active=true")
    }
}
