package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

class MockIncidenciaRepositoryImpl : IncidenciaRepository {

    private val now = System.currentTimeMillis()
    private val oneDay = 24 * 60 * 60 * 1000L

    private val mockData = mutableListOf(
        Incidencia(
            id = UUID.randomUUID().toString(),
            tematicaId = "mock-desechos-solidos",
            localidadId = null,
            descripcion = "Se ha detectado un vertedero de basura ilegal cerca del parque central.",
            imagenesPaths = emptyList(),
            imagenResolucionPath = null,
            latitud = 23.113592,
            longitud = -82.366592,
            fechaHora = now,
            estadoId = "mock-base-estado",
            esBorrador = true,
            sincronizado = false,
        ),
        Incidencia(
            id = UUID.randomUUID().toString(),
            tematicaId = "mock-focos-contaminantes",
            localidadId = null,
            descripcion = "Humo denso proveniente de una fábrica a las afueras de la ciudad, olor fuerte.",
            imagenesPaths = emptyList(),
            imagenResolucionPath = null,
            latitud = 23.123592,
            longitud = -82.376592,
            fechaHora = now - (oneDay * 1),
            estadoId = "mock-base-estado",
            esBorrador = false,
            sincronizado = false,
        ),
        Incidencia(
            id = UUID.randomUUID().toString(),
            serverId = UUID.randomUUID().toString(),
            tematicaId = "mock-arboles",
            localidadId = null,
            descripcion = "Tala indiscriminada de árboles en la zona forestal norte, sin permiso aparente.",
            imagenesPaths = emptyList(),
            imagenResolucionPath = null,
            latitud = 23.133592,
            longitud = -82.386592,
            fechaHora = now - (oneDay * 2),
            estadoId = "mock-base-estado",
            esBorrador = false,
            sincronizado = true,
        ),
    )

    private val _incidencias = MutableStateFlow(mockData.toList())

    override suspend fun guardar(incidencia: Incidencia): Result<Unit> {
        val index = mockData.indexOfFirst { it.id == incidencia.id }
        if (index != -1) {
            mockData[index] = incidencia
        } else {
            mockData.add(0, incidencia)
        }
        _incidencias.update { mockData.toList() }
        return Result.success(Unit)
    }

    override fun observarTodas(): Flow<List<Incidencia>> {
        return _incidencias.map { it.sortedByDescending { inc -> inc.fechaHora } }
    }

    override suspend fun obtenerPendientes(): List<Incidencia> {
        return mockData.filter { !it.sincronizado && !it.esBorrador }
    }

    override suspend fun marcarComoEnviada(id: String, estadoId: String): Result<Unit> {
        val index = mockData.indexOfFirst { it.id == id }
        if (index != -1) {
            mockData[index] =
                mockData[index].copy(estadoId = estadoId, esBorrador = false, sincronizado = true)
            _incidencias.update { mockData.toList() }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Incidencia no encontrada"))
    }

    override suspend fun enviarAlServidor(incidencia: Incidencia): Result<Unit> {
        val index = mockData.indexOfFirst { it.id == incidencia.id }
        if (index != -1) {
            mockData[index] =
                incidencia.copy(
                    serverId = incidencia.serverId ?: UUID.randomUUID().toString(),
                    sincronizado = true,
                    esBorrador = false,
                )
            _incidencias.update { mockData.toList() }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Incidencia no encontrada"))
    }

    override suspend fun syncDesdeServidor(
        page: Int?,
        pageSize: Int?,
        search: String?,
        ordering: String?
    ): Result<Int> = Result.success(mockData.size)

    override suspend fun obtenerPorId(id: String): Incidencia? {
        return mockData.find { it.id == id }
    }

    override suspend fun sincronizarDetalle(id: String): Result<Unit> {
        return if (mockData.any { it.id == id }) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Incidencia no encontrada"))
        }
    }

    override suspend fun actualizarEstado(
        id: String,
        estadoId: String,
        esBorrador: Boolean,
        imagenResolucionPath: String?,
    ): Result<Unit> {
        val index = mockData.indexOfFirst { it.id == id }
        if (index != -1) {
            mockData[index] =
                mockData[index].copy(
                    estadoId = estadoId,
                    esBorrador = esBorrador,
                    imagenResolucionPath =
                        imagenResolucionPath ?: mockData[index].imagenResolucionPath,
                    sincronizado = !esBorrador,
                )
            _incidencias.update { mockData.toList() }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Incidencia no encontrada"))
    }

    override fun observarPaginado(
        filtro: String,
        search: String,
        limit: Int,
        offset: Int
    ): Flow<List<Incidencia>> {
        return _incidencias.map { list ->
            list.filter { inc ->
                val matchesFilter = when (filtro) {
                    "BORRADORES" -> inc.esBorrador
                    "ENVIADAS" -> !inc.esBorrador && inc.sincronizado
                    else -> true
                }
                val matchesSearch = inc.descripcion.contains(search, ignoreCase = true) ||
                        inc.tematicaId.contains(search, ignoreCase = true) ||
                        (inc.themeName?.contains(search, ignoreCase = true) == true)
                matchesFilter && matchesSearch
            }.sortedByDescending { it.fechaHora }.drop(offset).take(limit)
        }
    }

    override fun obtenerCantidad(filtro: String, search: String): Flow<Int> {
        return _incidencias.map { list ->
            list.count { inc ->
                val matchesFilter = when (filtro) {
                    "BORRADORES" -> inc.esBorrador
                    "ENVIADAS" -> !inc.esBorrador && inc.sincronizado
                    else -> true
                }
                val matchesSearch = inc.descripcion.contains(search, ignoreCase = true) ||
                        inc.tematicaId.contains(search, ignoreCase = true) ||
                        (inc.themeName?.contains(search, ignoreCase = true) == true)
                matchesFilter && matchesSearch
            }
        }
    }

    override suspend fun actualizarRemoto(
        id: String,
        theme: String,
        locality: String,
        description: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        occurredAt: Long,
        evidences: List<String>,
        assignedTo: String?,
        isActive: Boolean,
        language: String
    ): Result<Unit> {
        val index = mockData.indexOfFirst { it.id == id }
        if (index != -1) {
            mockData[index] = mockData[index].copy(
                tematicaId = theme,
                localidadId = locality,
                descripcion = description,
                address = address,
                latitud = latitude,
                longitud = longitude,
                fechaHora = occurredAt,
                assignedToId = assignedTo,
                isActive = isActive,
                language = language,
                sincronizado = true,
                esBorrador = false
            )
            _incidencias.update { mockData.toList() }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Incidencia no encontrada"))
    }
}
