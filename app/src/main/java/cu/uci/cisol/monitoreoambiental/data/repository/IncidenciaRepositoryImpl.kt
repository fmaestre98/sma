package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.data.local.db.EstadoDao
import cu.uci.cisol.monitoreoambiental.data.local.db.IncidenciaDao
import cu.uci.cisol.monitoreoambiental.data.local.db.LocalidadDao
import cu.uci.cisol.monitoreoambiental.data.local.db.TematicaDao
import cu.uci.cisol.monitoreoambiental.data.local.entity.IncidenciaEntity
import cu.uci.cisol.monitoreoambiental.data.remote.ManagementApi
import cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia.CreateIncidenciaRequestDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia.CreateIncidenciaResponseDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia.IncidenciaListItemDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.incidencia.UpdateIncidenciaRequestDto
import cu.uci.cisol.monitoreoambiental.data.util.CatalogResolver
import cu.uci.cisol.monitoreoambiental.data.util.EvidenceUploadHelper
import cu.uci.cisol.monitoreoambiental.data.util.IsoInstantFormat
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.model.SyncUploadResult
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
    private val evidenceUploadHelper: EvidenceUploadHelper,
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
        return if (incidencia.serverId != null) {
            val result = actualizarRemoto(
                id = incidencia.serverId,
                theme = incidencia.tematicaId,
                locality = incidencia.localidadId ?: "",
                description = incidencia.descripcion,
                address = incidencia.address,
                latitude = incidencia.latitud,
                longitude = incidencia.longitud,
                occurredAt = incidencia.fechaHora,
                evidences = incidencia.imagenesPaths,
                assignedTo = incidencia.assignedToId,
                isActive = incidencia.isActive,
                language = incidencia.language ?: "es"
            )
            if (result.isSuccess) {
                kotlin.Result.success(Unit)
            } else {
                kotlin.Result.failure(
                    Exception("Error de red al actualizar incidencia: ${result.exceptionOrNull()?.message}"),
                )
            }
        } else {
            when (val upload = evidenceUploadHelper.resolveEvidenceIds(incidencia.imagenesPaths)) {
                is NetworkResult.Error ->
                    return kotlin.Result.failure(
                        Exception("Error al subir evidencias: ${upload.error}"),
                    )
                is NetworkResult.Success -> {
                    val ocurredAt = IsoInstantFormat.formatFromEpochMilli(incidencia.fechaHora)
                    val body = CreateIncidenciaRequestDto(
                        theme = incidencia.tematicaId,
                        locality = requireNotNull(incidencia.localidadId) { "localidadId requerida" },
                        description = incidencia.descripcion,
                        address = incidencia.address,
                        latitude = incidencia.latitud?.toString()?.take(10),
                        longitude = incidencia.longitud?.toString()?.take(10),
                        ocurredAt = ocurredAt,
                        evidences = upload.data,
                        evidencesPublic = false,
                        assignedTo = incidencia.assignedToId,
                    )
                    when (val result = managementApi.createIncidenciaJson(body)) {
                        is NetworkResult.Success -> {
                            val response = result.data
                            val evidencePaths = response.evidences.mapNotNull { evidence ->
                                cu.uci.cisol.monitoreoambiental.data.remote.ApiConfig.resolveMediaUrl(evidence.fileUrl)
                            }.ifEmpty { upload.data }
                            withContext(Dispatchers.IO) {
                                val updated =
                                    incidencia.copy(
                                        serverId = response.id,
                                        sincronizado = true,
                                        esBorrador = false,
                                        imagenesPaths = evidencePaths.ifEmpty { incidencia.imagenesPaths },
                                        ticketNumber = response.ticketNumber,
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
            }
        }
    }

    override suspend fun syncDesdeServidor(
        page: Int?,
        pageSize: Int?,
        search: String?,
        ordering: String?,
    ): Result<Int> =
        fetchAndMergeIncidenciasPage(page, pageSize, search, ordering).map { it.first }

    override suspend fun enviarPendientesAlServidor(): Result<SyncUploadResult> {
        val pendientes = obtenerPendientes()
        var enviadas = 0
        var fallidas = 0
        for (incidencia in pendientes) {
            if (enviarAlServidor(incidencia).isSuccess) {
                enviadas++
            } else {
                fallidas++
            }
        }
        return kotlin.Result.success(SyncUploadResult(enviadas = enviadas, fallidas = fallidas))
    }

    override suspend fun syncCompletoDesdeServidor(pageSize: Int): Result<Int> {
        var page = 1
        var totalCount = 0
        while (true) {
            val pageResult =
                fetchAndMergeIncidenciasPage(
                    page = page,
                    pageSize = pageSize,
                    search = null,
                    ordering = "-updated_at",
                )
            if (pageResult.isFailure) {
                return kotlin.Result.failure(
                    pageResult.exceptionOrNull()
                        ?: Exception("Error al sincronizar incidencias"),
                )
            }
            val (count, itemsInPage) = pageResult.getOrThrow()
            totalCount = count
            if (itemsInPage == 0 || page * pageSize >= totalCount) {
                return kotlin.Result.success(totalCount)
            }
            page++
        }
    }

    private suspend fun fetchAndMergeIncidenciasPage(
        page: Int?,
        pageSize: Int?,
        search: String?,
        ordering: String?,
    ): Result<Pair<Int, Int>> {
        return when (
            val result =
                managementApi.getIncidencias(
                    ordering = ordering,
                    page = page,
                    pageSize = pageSize,
                    search = search,
                )
        ) {
            is NetworkResult.Success -> {
                withContext(Dispatchers.IO) {
                    val tematicas = tematicaDao.getAllActive()
                    val estados = estadoDao.getAllActive()
                    val localidades = localidadDao.getAllActive()

                    result.data.results.forEach { remote ->
                        mergeRemoteIncidencia(remote, tematicas, estados, localidades)
                    }
                }
                kotlin.Result.success(
                    Pair(result.data.count, result.data.results.size),
                )
            }
            is NetworkResult.Error ->
                kotlin.Result.failure(
                    Exception("Error al sincronizar incidencias: ${result.error}"),
                )
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

        val themeName = remote.themeName
            ?: tematicas.find { it.id == tematicaId }?.name
            ?: remote.theme
        val statusName = remote.statusName
            ?: estados.find { it.id == estadoId }?.name
            ?: remote.status
        val localityName = remote.localityName
            ?: localidades.find { it.id == localidadId }?.name
            ?: remote.locality
        val themeColor = remote.themeColor
            ?: tematicas.find { it.id == tematicaId }?.colorHex
            ?: existing?.themeColor
        val statusColor = remote.statusColor
            ?: estados.find { it.id == estadoId }?.colorHex
            ?: existing?.statusColor

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
                    themeColor = themeColor,
                    statusColor = statusColor,
                    criticality = remote.criticalityName ?: remote.criticality,
                    criticalityColor = remote.criticalityColor,
                    themeName = themeName,
                    statusName = statusName,
                    localityName = localityName,
                    reporterName = remote.reporterName,
                    assignedToName = remote.assignedToName,
                    assignedToId = remote.assignedTo,
                    address = remote.address ?: existing.address,
                    descripcion = remote.description?.takeIf { it.isNotBlank() } ?: existing.descripcion,
                    latitud = remote.latitude?.toDoubleOrNull() ?: existing.latitud,
                    longitud = remote.longitude?.toDoubleOrNull() ?: existing.longitud,
                    updatedAt = remote.updatedAt,
                    language = remote.language,
                    isActive = remote.isActive,
                )
            } else {
                IncidenciaEntity(
                    id = remote.id,
                    serverId = remote.id,
                    tematica = tematicaId,
                    localidadId = localidadId,
                    descripcion = remote.description.orEmpty(),
                    imagenesPaths = emptyList(),
                    imagenResolucionPath = null,
                    latitud = remote.latitude?.toDoubleOrNull(),
                    longitud = remote.longitude?.toDoubleOrNull(),
                    fechaHora = fechaHora,
                    estadoId = estadoId,
                    esBorrador = false,
                    sincronizado = true,
                    ticketNumber = remote.ticketNumber,
                    themeColor = themeColor,
                    statusColor = statusColor,
                    criticality = remote.criticalityName ?: remote.criticality,
                    criticalityColor = remote.criticalityColor,
                    themeName = themeName,
                    statusName = statusName,
                    localityName = localityName,
                    reporterName = remote.reporterName,
                    assignedToName = remote.assignedToName,
                    assignedToId = remote.assignedTo,
                    address = remote.address,
                    updatedAt = remote.updatedAt,
                    language = remote.language,
                    isActive = remote.isActive,
                )
            }

        incidenciaDao.insertar(entity)
    }

    override suspend fun obtenerPorId(id: String): Incidencia? {
        return withContext(Dispatchers.IO) { incidenciaDao.obtenerPorId(id)?.toDomain() }
    }

    override suspend fun sincronizarDetalle(id: String): Result<Unit> {
        val local =
            withContext(Dispatchers.IO) {
                incidenciaDao.obtenerPorId(id) ?: incidenciaDao.obtenerPorServerId(id)
            }
        val remoteId = local?.serverId ?: local?.id ?: id

        return when (val result = managementApi.getIncidenciaDetail(remoteId)) {
            is NetworkResult.Success -> {
                withContext(Dispatchers.IO) {
                    mergeDetailResponse(localId = local?.id ?: id, response = result.data)
                }
                kotlin.Result.success(Unit)
            }
            is NetworkResult.Error ->
                kotlin.Result.failure(
                    Exception("Error al sincronizar detalle de incidencia: ${result.error}"),
                )
        }
    }

    private suspend fun mergeDetailResponse(
        localId: String,
        response: CreateIncidenciaResponseDto,
    ) {
        val existing =
            incidenciaDao.obtenerPorId(localId)
                ?: incidenciaDao.obtenerPorServerId(response.id)
                ?: incidenciaDao.obtenerPorId(response.id)

        val tematicas = tematicaDao.getAllActive()
        val estados = estadoDao.getAllActive()
        val localidades = localidadDao.getAllActive()

        val tematicaId = CatalogResolver.resolveTematicaId(response.theme, tematicas) ?: response.theme
        val localidadId =
            CatalogResolver.resolveLocalidadId(response.locality, localidades) ?: response.locality
        val estadoId = response.status?.let { CatalogResolver.resolveEstadoId(it, estados) }
            ?: existing?.estadoId
            ?: ""
        val themeName = response.themeName
            ?: tematicas.find { it.id == response.theme }?.name
            ?: response.theme
        val localityName = response.localityName
            ?: localidades.find { it.id == response.locality }?.name
            ?: response.locality
        val statusName = response.statusName
            ?: estados.find { it.id == estadoId }?.name
            ?: existing?.statusName
        val evidenceUrls = response.evidences.mapNotNull { evidence ->
            cu.uci.cisol.monitoreoambiental.data.remote.ApiConfig.resolveMediaUrl(evidence.fileUrl)
        }.ifEmpty { existing?.imagenesPaths.orEmpty() }

        val updated =
            (existing ?: IncidenciaEntity(
                id = localId,
                serverId = response.id,
                tematica = tematicaId,
                localidadId = localidadId,
                descripcion = response.description,
                imagenesPaths = evidenceUrls,
                imagenResolucionPath = null,
                latitud = response.latitude?.toDoubleOrNull(),
                longitud = response.longitude?.toDoubleOrNull(),
                fechaHora = parseOcurredAt(response.ocurredAt),
                estadoId = estadoId,
                esBorrador = false,
                sincronizado = true,
            )).copy(
                serverId = response.id,
                tematica = tematicaId,
                localidadId = localidadId,
                descripcion = response.description,
                address = response.address,
                latitud = response.latitude?.toDoubleOrNull(),
                longitud = response.longitude?.toDoubleOrNull(),
                fechaHora = parseOcurredAt(response.ocurredAt),
                assignedToId = response.assignedTo,
                isActive = response.isActive,
                language = response.language,
                updatedAt = response.updatedAt,
                themeName = themeName,
                localityName = localityName,
                statusName = statusName,
                statusColor = response.statusColor ?: existing?.statusColor,
                ticketNumber = response.ticketNumber ?: existing?.ticketNumber,
                estadoId = estadoId.ifBlank { existing?.estadoId.orEmpty() },
                imagenesPaths = evidenceUrls,
                sincronizado = true,
                esBorrador = false,
            )

        incidenciaDao.insertar(updated)
    }

    override fun observarPaginado(filtro: String, search: String, limit: Int, offset: Int): Flow<List<Incidencia>> {
        return incidenciaDao.obtenerPaginado(filtro, search, limit, offset).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun obtenerCantidad(filtro: String, search: String): Flow<Int> {
        return incidenciaDao.obtenerCantidad(filtro, search)
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
        val uploadedIds = when (val upload = evidenceUploadHelper.resolveEvidenceIds(evidences)) {
            is NetworkResult.Success -> upload.data
            is NetworkResult.Error ->
                return kotlin.Result.failure(Exception("Error al subir evidencias: ${upload.error}"))
        }

        val evidenceIds = if (evidences.isNotEmpty()) {
            val remoteIds = when (val detail = managementApi.getIncidenciaDetail(id)) {
                is NetworkResult.Success -> detail.data.evidences.map { it.id }
                is NetworkResult.Error -> emptyList()
            }
            val existingUuids = evidences.filter {
                it.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
            }
            (remoteIds + existingUuids + uploadedIds).distinct()
        } else {
            emptyList()
        }

        val occurredAtIso = IsoInstantFormat.formatFromEpochMilli(occurredAt)
        val body = UpdateIncidenciaRequestDto(
            isActive = isActive,
            language = language,
            theme = theme,
            locality = locality,
            description = description,
            address = address,
            latitude = latitude?.toString(),
            longitude = longitude?.toString(),
            ocurredAt = occurredAtIso,
            evidences = evidenceIds,
            evidencesPublic = false,
            assignedTo = assignedTo
        )

        return when (val result = managementApi.updateIncidencia(id, body)) {
            is NetworkResult.Success -> {
                val response = result.data
                withContext(Dispatchers.IO) {
                    val existing = incidenciaDao.obtenerPorId(id)
                        ?: incidenciaDao.obtenerPorServerId(id)
                    val tematicas = tematicaDao.getAllActive()
                    val localidades = localidadDao.getAllActive()
                    val estados = estadoDao.getAllActive()

                    val tematicaId = CatalogResolver.resolveTematicaId(response.theme, tematicas) ?: response.theme
                    val localidadId = CatalogResolver.resolveLocalidadId(response.locality, localidades) ?: response.locality
                    val estadoId = response.status?.let { CatalogResolver.resolveEstadoId(it, estados) }
                        ?: existing?.estadoId
                        ?: ""

                    val themeName = response.themeName
                        ?: tematicas.find { it.id == response.theme }?.name
                        ?: response.theme
                    val localityName = response.localityName
                        ?: localidades.find { it.id == response.locality }?.name
                        ?: response.locality
                    val evidencePaths = response.evidences.mapNotNull { evidence ->
                        cu.uci.cisol.monitoreoambiental.data.remote.ApiConfig.resolveMediaUrl(evidence.fileUrl)
                    }.ifEmpty { existing?.imagenesPaths.orEmpty() }

                    val updated = (existing ?: IncidenciaEntity(
                        id = id,
                        serverId = id,
                        tematica = tematicaId,
                        localidadId = localidadId,
                        descripcion = response.description,
                        imagenResolucionPath = null,
                        latitud = response.latitude?.toDoubleOrNull(),
                        longitud = response.longitude?.toDoubleOrNull(),
                        fechaHora = parseOcurredAt(response.ocurredAt),
                        estadoId = estadoId,
                        esBorrador = false,
                        sincronizado = true
                    )).copy(
                        tematica = tematicaId,
                        localidadId = localidadId,
                        descripcion = response.description,
                        address = response.address,
                        latitud = response.latitude?.toDoubleOrNull(),
                        longitud = response.longitude?.toDoubleOrNull(),
                        fechaHora = parseOcurredAt(response.ocurredAt),
                        assignedToId = response.assignedTo,
                        isActive = response.isActive,
                        language = response.language,
                        updatedAt = response.updatedAt,
                        themeName = themeName,
                        localityName = localityName,
                        statusName = response.statusName ?: existing?.statusName,
                        statusColor = response.statusColor ?: existing?.statusColor,
                        ticketNumber = response.ticketNumber ?: existing?.ticketNumber,
                        estadoId = estadoId.ifBlank { existing?.estadoId.orEmpty() },
                        imagenesPaths = evidencePaths,
                        sincronizado = true,
                        esBorrador = false
                    )
                    incidenciaDao.insertar(updated)
                }
                kotlin.Result.success(Unit)
            }
            is NetworkResult.Error ->
                kotlin.Result.failure(Exception("Error de red al actualizar incidencia: ${result.error}"))
        }
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
            themeColor = themeColor,
            statusColor = statusColor,
            criticality = criticality,
            criticalityColor = criticalityColor,
            themeName = themeName,
            statusName = statusName,
            localityName = localityName,
            reporterName = reporterName,
            assignedToName = assignedToName,
            address = address,
            assignedToId = assignedToId,
            updatedAt = updatedAt,
            language = language,
            isActive = isActive,
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
            themeColor = themeColor,
            statusColor = statusColor,
            criticality = criticality,
            criticalityColor = criticalityColor,
            themeName = themeName,
            statusName = statusName,
            localityName = localityName,
            reporterName = reporterName,
            assignedToName = assignedToName,
            address = address,
            assignedToId = assignedToId,
            updatedAt = updatedAt,
            language = language,
            isActive = isActive,
        )
}
