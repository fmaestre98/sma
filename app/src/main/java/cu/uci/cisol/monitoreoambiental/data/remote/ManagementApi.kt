package cu.uci.cisol.monitoreoambiental.data.remote

import cu.uci.cisol.monitoreoambiental.data.remote.dto.EstadosResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.OrganizationsResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.TematicasResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia.CreateIncidenciaResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia.IncidenciasListResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia.UpdateIncidenciaRequestDto
import cu.uci.cisol.monitoreoambiental.data.util.getSafe
import cu.uci.cisol.monitoreoambiental.data.util.postMultipartSafe
import cu.uci.cisol.monitoreoambiental.data.util.patchSafe
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.http.content.PartData

class ManagementApi(private val client: HttpClient) {

    companion object {
        const val BASE_URL = "${ApiConfig.BASE_URL}/api/v1/management"
    }

    suspend fun getTematicas(): Result<TematicasResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/themes/?is_active=true")
    }

    suspend fun getEstados(): Result<EstadosResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/states/?is_active=true")
    }

    suspend fun getIncidencias(
        ordering: String? = "-updated_at",
        page: Int? = null,
        pageSize: Int? = null,
        search: String? = null,
    ): Result<IncidenciasListResponseDto, DataError.Network> {
        val params = mutableMapOf<String, Any?>()
        if (ordering != null) params["ordering"] = ordering
        if (page != null) params["p"] = page
        if (pageSize != null) params["page_size"] = pageSize
        if (search != null) params["search"] = search
        return client.getSafe("$BASE_URL/incidences/", params)
    }

    suspend fun getIncidenciaDetail(
        id: String,
    ): Result<CreateIncidenciaResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/incidences/$id/")
    }

    suspend fun updateIncidencia(
        id: String,
        body: UpdateIncidenciaRequestDto,
    ): Result<CreateIncidenciaResponseDto, DataError.Network> {
        return client.patchSafe("$BASE_URL/incidences/$id/", body)
    }

    suspend fun createIncidencia(
        formParts: List<PartData>,
    ): Result<CreateIncidenciaResponseDto, DataError.Network> {
        return client.postMultipartSafe("$BASE_URL/incidences/") { formParts }
    }

    suspend fun getOrganizations(): Result<OrganizationsResponseDto, DataError.Network> {
        return client.getSafe("$BASE_URL/organization/", mapOf("page_size" to 1000))
    }
}
