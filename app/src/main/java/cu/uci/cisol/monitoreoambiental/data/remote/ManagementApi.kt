package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.data.remote.dto.EstadosResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.TematicasResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia.CreateIncidenciaResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia.IncidenciasListResponseDto
import cu.uci.cisol.monitoreoambiental.data.util.getSafe
import cu.uci.cisol.monitoreoambiental.data.util.postMultipartSafe
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.http.content.PartData

class ManagementApi(private val client: HttpClient) {

    companion object {
        const val BASE_URL = "http://192.168.1.101:8000/api/v1/management"
    }

    suspend fun getTematicas(): Result<TematicasResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/themes/?is_active=true")
    }

    suspend fun getEstados(): Result<EstadosResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/states/?is_active=true")
    }

    suspend fun getIncidencias(): Result<IncidenciasListResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/incidences/")
    }

    suspend fun createIncidencia(
        formParts: List<PartData>,
    ): Result<CreateIncidenciaResponseDto, DataError.Network> {
        return client.postMultipartSafe("$BASE_URL/incidences/") { formParts }
    }
}
