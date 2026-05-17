package cu.uci.cisol.monitoreoambiental.data.repository

import android.content.Context
import cu.uci.cisol.monitoreoambiental.data.local.db.EstadoDao
import cu.uci.cisol.monitoreoambiental.data.local.db.IncidenciaDao
import cu.uci.cisol.monitoreoambiental.data.local.db.LocalidadDao
import cu.uci.cisol.monitoreoambiental.data.local.db.TematicaDao
import cu.uci.cisol.monitoreoambiental.data.local.entity.IncidenciaEntity
import cu.uci.cisol.monitoreoambiental.data.remote.ManagementApi
import cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia.IncidenciaListItemDto
import cu.uci.cisol.monitoreoambiental.data.util.CatalogResolver
import cu.uci.cisol.monitoreoambiental.data.util.IncidenciaMultipartBuilder
import cu.uci.cisol.monitoreoambiental.data.util.IsoInstantFormat
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.util.Result as NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class IncidenciaRepositoryImpl(
    private val incidenciaDao: IncidenciaDao,
    private val managementApi: ManagementApi,
    private val tematicaDao: TematicaDao,
    private val estadoDao: EstadoDao,
    private val localidadDao: LocalidadDao,
    private val context: Context,
) : IncidenciaRepository {

    override suspend fun guardar(incidencia: Incidencia): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { incidenciaDao.insertar(incidencia.toEntity()) }
    }

    override fun observarTodas(): Flow<List<Incidencia>> {
        return incidenciaDao.observarTodas().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun obtenerPendientes(): List<Incidencia> {
        return withContext(Dispatchers.IO) {
            incidenciaDao.obtenerPendientes().map { it.toDomain() }
        }
    }

    override suspend fun marcarComoEnviada(id: String, estadoId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            incidenciaDao.actualizarEstado(
                id = id,
                estadoId = estadoId,
                esBorrador = false,
                imagenResolucionPath = null,
                sincronizado = true,
            )
        }
    }

    override suspend fun actualizarEstado(
        id: String,
        estadoId: String,
        esBorrador: Boolean,
        imagenResolucionPath: String?,
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            incidenciaDao.actualizarEstado(
                id = id,
                estadoId = estadoId,
                esBorrador = esBorrador,
                imagenResolucionPath = imagenResolucionPath,
                sincronizado = !esBorrador,
            )
        }
    }

    override suspend fun enviarAlServidor(incidencia: Incidencia): Result<Unit> {
        val formParts = IncidenciaMultipartBuilder.buildCreateForm(context, incidencia)
        return when (val result = managementApi.createIncidencia(formParts)) {
            is NetworkResult.Success -> {
                val response = result.data
                withContext(Dispatchers.IO) {
                    val updated =
                        incidencia.copy(
                            serverId = response.id,
                            sincronizado = true,
                            esBorrador = false,
                        )
                    incidenciaDao.insertar(updated.toEntity())
                }
                kotlin.Result.success(Unit)
            }
            is NetworkResult.Error ->
                kotlin.Result.failure(
                    Exception("Error de red al enviar incidencia: ${result.error}"),
                )
        }
    }

    override suspend fun syncDesdeServidor(): Result<Unit> {
        return when (val result = managementApi.getIncidencias()) {
            is NetworkResult.Success -> {
                withContext(Dispatchers.IO) {
                    val tematicas = tematicaDao.getAllActive()
                    val estados = estadoDao.getAllActive()
                    val localidades = localidadDao.getAllActive()

                    result.data.results.forEach { remote ->
                        mergeRemoteIncidencia(remote, tematicas, estados, localidades)
                    }
                }
                kotlin.Result.success(Unit)
            }
            is NetworkResult.Error ->
                kotlin.Result.failure(Exception("Error al sincronizar incidencias"))
        }
    }

    private suspend fun mergeRemoteIncidencia(
        remote: IncidenciaListItemDto,
        tematicas: List<cu.uci.cisol.monitoreoambiental.data.local.entity.TematicaEntity>,
        estados: List<cu.uci.cisol.monitoreoambiental.data.local.entity.EstadoEntity>,
        localidades: List<cu.uci.cisol.monitoreoambiental.data.local.entity.LocalidadEntity>,
    ) {
        val existing = incidenciaDao.obtenerPorServerId(remote.id)
        val tematicaId = CatalogResolver.resolveTematicaId(remote.theme, tematicas)
            ?: existing?.tematica
            ?: remote.theme
        val estadoId = CatalogResolver.resolveEstadoId(remote.status, estados)
            ?: existing?.estadoId
            ?: remote.status
        val localidadId = CatalogResolver.resolveLocalidadId(remote.locality, localidades)
            ?: existing?.localidadId

        val fechaHora = parseOcurredAt(remote.ocurredAt)

        val entity =
            if (existing != null) {
                existing.copy(
                    tematica = tematicaId,
                    localidadId = localidadId,
                    estadoId = estadoId,
                    fechaHora = fechaHora,
                    esBorrador = false,
                    sincronizado = true,
                    ticketNumber = remote.ticketNumber,
                )
            } else {
                IncidenciaEntity(
                    id = remote.id,
                    serverId = remote.id,
                    tematica = tematicaId,
                    localidadId = localidadId,
                    descripcion = "",
                    imagenesPaths = emptyList(),
                    imagenResolucionPath = null,
                    latitud = null,
                    longitud = null,
                    fechaHora = fechaHora,
                    estadoId = estadoId,
                    esBorrador = false,
                    sincronizado = true,
                    ticketNumber = remote.ticketNumber,
                )
            }

        incidenciaDao.insertar(entity)
    }

    override suspend fun obtenerPorId(id: String): Incidencia? {
        return withContext(Dispatchers.IO) { incidenciaDao.obtenerPorId(id)?.toDomain() }
    }

    private fun parseOcurredAt(iso: String): Long =
        IsoInstantFormat.parseToEpochMilli(iso) ?: System.currentTimeMillis()

    private fun Incidencia.toEntity() =
        IncidenciaEntity(
            id = id,
            serverId = serverId,
            tematica = tematicaId,
            localidadId = localidadId,
            descripcion = descripcion,
            imagenesPaths = imagenesPaths,
            imagenResolucionPath = imagenResolucionPath,
            latitud = latitud,
            longitud = longitud,
            fechaHora = fechaHora,
            estadoId = estadoId,
            esBorrador = esBorrador,
            sincronizado = sincronizado,
            ticketNumber = ticketNumber,
        )

    private fun IncidenciaEntity.toDomain() =
        Incidencia(
            id = id,
            serverId = serverId,
            tematicaId = tematica,
            localidadId = localidadId,
            descripcion = descripcion,
            imagenesPaths = imagenesPaths,
            imagenResolucionPath = imagenResolucionPath,
            latitud = latitud,
            longitud = longitud,
            fechaHora = fechaHora,
            estadoId = estadoId,
            esBorrador = esBorrador,
            sincronizado = sincronizado,
            ticketNumber = ticketNumber,
        )
}
