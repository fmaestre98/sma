package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.data.remote.dto.TematicasResponseDto
import cu.uci.cisol.monitoreoambiental.data.util.getSafe
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient

class ManagementApi(private val client: HttpClient) {

    companion object {
        const val BASE_URL = "http://192.168.1.100:8000/api/v1/management"
    }

    suspend fun getTematicas(): Result<TematicasResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/themes/?is_active=true")
    }
}
