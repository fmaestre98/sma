package cu.uci.cisol.monitoreoambiental.ui.incidencia

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Localidad
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
import cu.uci.cisol.monitoreoambiental.data.work.IncidenciaSyncScheduler
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.LocalidadRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.TematicaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetalleIncidenciaState(
    val incidenciaId: String = "",
    val serverId: String? = null,
    val ticketNumber: Int? = null,
    val statusName: String = "",
    val statusColor: String = "#9E9E9E",
    val criticality: String = "No evaluada",
    val criticalityColor: String = "#808080",
    val reporterName: String? = null,
    val assignedToName: String? = null,
    val themeName: String? = null,
    val localityName: String? = null,
    val updatedAt: String? = null,
    val fechaHora: Long = 0L,
    val fechaFormateada: String = "",
    val tematicaId: String? = null,
    val localidadId: String? = null,
    val descripcion: String = "",
    val address: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val marcadaResuelta: Boolean = false,
    val assignedToId: String? = null,
    val language: String = "es",
    val imagenesUris: List<Uri> = emptyList(),
    val evidenciasResolucionUris: List<Uri> = emptyList(),
    val tematicas: List<Tematica> = emptyList(),
    val localidades: List<Localidad> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showMapDialog: Boolean = false,
    val obteniendoCoordenadas: Boolean = false,
    val error: String? = null,
) {
    val selectedTematica: Tematica?
        get() = tematicas.firstOrNull { it.id == tematicaId }

    val selectedLocalidad: Localidad?
        get() = localidades.firstOrNull { it.id == localidadId }
}

sealed interface DetalleIncidenciaAction {
    data class OnTematicaChange(val tematicaId: String) : DetalleIncidenciaAction
    data class OnLocalidadChange(val localidadId: String) : DetalleIncidenciaAction
    data class OnDescripcionChange(val descripcion: String) : DetalleIncidenciaAction
    data class OnAddressChange(val address: String) : DetalleIncidenciaAction
    data class OnMarcadaResueltaChange(val marcada: Boolean) : DetalleIncidenciaAction
    data class OnFotoTomada(val uri: Uri?) : DetalleIncidenciaAction
    data class OnFotoSeleccionada(val uris: List<Uri>) : DetalleIncidenciaAction
    data class OnFotoEliminada(val uri: Uri) : DetalleIncidenciaAction
    object OnObtenerCoordenadas : DetalleIncidenciaAction
    data class OnCoordenadasObtenidas(val lat: Double, val lng: Double) : DetalleIncidenciaAction
    object OnCoordenadasError : DetalleIncidenciaAction
    object OnSeleccionarEnMapa : DetalleIncidenciaAction
    object OnCerrarMapa : DetalleIncidenciaAction
    data class OnMapLocationSelected(val lat: Double, val lng: Double) : DetalleIncidenciaAction
    object OnGuardar : DetalleIncidenciaAction
}

sealed interface DetalleIncidenciaEvent {
    object NavigateBack : DetalleIncidenciaEvent
    data class ShowSnackbar(val message: String) : DetalleIncidenciaEvent
}

class DetalleIncidenciaViewModel(
    private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val incidenciaRepository: IncidenciaRepository,
    private val tematicaRepository: TematicaRepository,
    private val localidadRepository: LocalidadRepository,
) : ViewModel() {

    private val incidenciaIdArg: String? = savedStateHandle["incidenciaId"]

    private val _state = MutableStateFlow(DetalleIncidenciaState(isLoading = true))
    val state: StateFlow<DetalleIncidenciaState> = _state.asStateFlow()

    private val _events = Channel<DetalleIncidenciaEvent>()
    val events = _events.receiveAsFlow()

    private var fechaHoraOriginal: Long = 0L

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("es", "ES"))

    init {
        observeTematicas()
        observeLocalidades()

        val id = incidenciaIdArg
        if (id.isNullOrBlank() || id == "{incidenciaId}") {
            viewModelScope.launch {
                _state.update { it.copy(isLoading = false, error = "Incidencia no encontrada") }
                _events.send(DetalleIncidenciaEvent.ShowSnackbar("Incidencia no encontrada"))
                _events.send(DetalleIncidenciaEvent.NavigateBack)
            }
        } else {
            cargarIncidencia(id)
        }
    }

    private fun observeTematicas() {
        viewModelScope.launch {
            tematicaRepository.observeTematicas().collect { tematicas ->
                _state.update { it.copy(tematicas = tematicas) }
            }
        }
    }

    private fun observeLocalidades() {
        viewModelScope.launch {
            localidadRepository.observeLocalidades().collect { localidades ->
                _state.update { it.copy(localidades = localidades) }
                if (localidades.size == 1 && _state.value.localidadId == null) {
                    onAction(DetalleIncidenciaAction.OnLocalidadChange(localidades.first().id))
                }
            }
        }
    }

    private fun cargarIncidencia(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val local = incidenciaRepository.obtenerPorId(id)
            if (local != null) {
                applyIncidencia(local)
            } else {
                _state.update { it.copy(isLoading = false, error = "Incidencia no encontrada") }
                _events.send(DetalleIncidenciaEvent.ShowSnackbar("Incidencia no encontrada"))
                _events.send(DetalleIncidenciaEvent.NavigateBack)
                return@launch
            }

            val syncResult = incidenciaRepository.sincronizarDetalle(id)
            if (syncResult.isSuccess) {
                incidenciaRepository.obtenerPorId(id)?.let { applyIncidencia(it) }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun applyIncidencia(incidencia: Incidencia) {
        fechaHoraOriginal = incidencia.fechaHora
        _state.update {
            it.copy(
                incidenciaId = incidencia.id,
                serverId = incidencia.serverId,
                ticketNumber = incidencia.ticketNumber,
                statusName = incidencia.statusName ?: incidencia.estadoId,
                statusColor = incidencia.statusColor ?: "#9E9E9E",
                criticality = incidencia.criticality ?: "No evaluada",
                criticalityColor = incidencia.criticalityColor ?: "#808080",
                reporterName = incidencia.reporterName,
                assignedToName = incidencia.assignedToName,
                themeName = incidencia.themeName,
                localityName = incidencia.localityName,
                updatedAt = incidencia.updatedAt,
                fechaHora = incidencia.fechaHora,
                fechaFormateada = dateFormatter.format(Date(incidencia.fechaHora)),
                tematicaId = incidencia.tematicaId,
                localidadId = incidencia.localidadId,
                descripcion = incidencia.descripcion,
                address = incidencia.address.orEmpty(),
                latitud = incidencia.latitud,
                longitud = incidencia.longitud,
                marcadaResuelta = !incidencia.isActive,
                assignedToId = incidencia.assignedToId,
                language = incidencia.language ?: "es",
                imagenesUris = incidencia.imagenesPaths.map { Uri.parse(it) },
            )
        }
    }

    fun onAction(action: DetalleIncidenciaAction) {
        when (action) {
            is DetalleIncidenciaAction.OnTematicaChange -> {
                _state.update { it.copy(tematicaId = action.tematicaId) }
            }
            is DetalleIncidenciaAction.OnLocalidadChange -> {
                _state.update { it.copy(localidadId = action.localidadId) }
            }
            is DetalleIncidenciaAction.OnDescripcionChange -> {
                _state.update { it.copy(descripcion = action.descripcion) }
            }
            is DetalleIncidenciaAction.OnAddressChange -> {
                _state.update { it.copy(address = action.address) }
            }
            is DetalleIncidenciaAction.OnMarcadaResueltaChange -> {
                _state.update { it.copy(marcadaResuelta = action.marcada) }
            }
            is DetalleIncidenciaAction.OnFotoTomada -> {
                if (action.uri != null && _state.value.evidenciasResolucionUris.size < 2) {
                    viewModelScope.launch {
                        val savedUri = copyUriToInternalStorage(appContext, action.uri)
                        _state.update {
                            it.copy(evidenciasResolucionUris = it.evidenciasResolucionUris + savedUri)
                        }
                    }
                }
            }
            is DetalleIncidenciaAction.OnFotoSeleccionada -> {
                viewModelScope.launch {
                    val current = _state.value.evidenciasResolucionUris
                    val available = 2 - current.size
                    if (available > 0) {
                        val newUris = action.uris.take(available).map {
                            copyUriToInternalStorage(appContext, it)
                        }
                        _state.update { it.copy(evidenciasResolucionUris = current + newUris) }
                    }
                }
            }
            is DetalleIncidenciaAction.OnFotoEliminada -> {
                _state.update {
                    it.copy(evidenciasResolucionUris = it.evidenciasResolucionUris - action.uri)
                }
            }
            DetalleIncidenciaAction.OnObtenerCoordenadas -> {
                _state.update { it.copy(obteniendoCoordenadas = true) }
            }
            is DetalleIncidenciaAction.OnCoordenadasObtenidas -> {
                _state.update {
                    it.copy(
                        latitud = action.lat,
                        longitud = action.lng,
                        obteniendoCoordenadas = false,
                    )
                }
            }
            DetalleIncidenciaAction.OnCoordenadasError -> {
                _state.update { it.copy(obteniendoCoordenadas = false) }
                viewModelScope.launch {
                    _events.send(DetalleIncidenciaEvent.ShowSnackbar("Error al obtener ubicación"))
                }
            }
            DetalleIncidenciaAction.OnSeleccionarEnMapa -> {
                _state.update { it.copy(showMapDialog = true) }
            }
            DetalleIncidenciaAction.OnCerrarMapa -> {
                _state.update { it.copy(showMapDialog = false) }
            }
            is DetalleIncidenciaAction.OnMapLocationSelected -> {
                _state.update {
                    it.copy(
                        latitud = action.lat,
                        longitud = action.lng,
                        showMapDialog = false,
                    )
                }
            }
            DetalleIncidenciaAction.OnGuardar -> guardar()
        }
    }

    private fun guardar() {
        val current = _state.value
        if (current.tematicaId == null || current.descripcion.isBlank()) {
            viewModelScope.launch {
                _events.send(
                    DetalleIncidenciaEvent.ShowSnackbar("Complete temática y descripción"),
                )
            }
            return
        }
        if (current.localidadId == null) {
            viewModelScope.launch {
                _events.send(DetalleIncidenciaEvent.ShowSnackbar("Seleccione una localidad"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val remoteId = current.serverId ?: current.incidenciaId
            val evidencePaths = current.evidenciasResolucionUris.map { it.toString() }

            if (evidencePaths.isNotEmpty()) {
                val local = incidenciaRepository.obtenerPorId(current.incidenciaId)
                if (local != null) {
                    val updatedLocal =
                        local.copy(
                            imagenesPaths = local.imagenesPaths + evidencePaths,
                            imagenResolucionPath =
                                evidencePaths.firstOrNull() ?: local.imagenResolucionPath,
                        )
                    incidenciaRepository.guardar(updatedLocal)
                }
            }

            val result = withTimeoutOrNull(18000L) {
                incidenciaRepository.actualizarRemoto(
                    id = remoteId,
                    theme = current.tematicaId!!,
                    locality = current.localidadId!!,
                    description = current.descripcion,
                    address = current.address.takeIf { it.isNotBlank() },
                    latitude = current.latitud,
                    longitude = current.longitud,
                    occurredAt = fechaHoraOriginal,
                    evidences = evidencePaths,
                    assignedTo = current.assignedToId,
                    isActive = !current.marcadaResuelta,
                    language = current.language,
                )
            }

            _state.update { it.copy(isSaving = false) }

            if (result != null && result.isSuccess) {
                _events.send(DetalleIncidenciaEvent.ShowSnackbar("Incidencia actualizada"))
                _events.send(DetalleIncidenciaEvent.NavigateBack)
            } else {
                val local = incidenciaRepository.obtenerPorId(current.incidenciaId)
                if (local != null) {
                    val updatedLocal = local.copy(
                        tematicaId = current.tematicaId!!,
                        localidadId = current.localidadId,
                        descripcion = current.descripcion,
                        address = current.address.takeIf { it.isNotBlank() },
                        latitud = current.latitud,
                        longitud = current.longitud,
                        imagenesPaths = local.imagenesPaths + evidencePaths,
                        imagenResolucionPath = evidencePaths.firstOrNull() ?: local.imagenResolucionPath,
                        assignedToId = current.assignedToId,
                        isActive = !current.marcadaResuelta,
                        sincronizado = false
                    )
                    incidenciaRepository.guardar(updatedLocal)
                    IncidenciaSyncScheduler.enqueueUploadPending(appContext)
                }
                _events.send(DetalleIncidenciaEvent.ShowSnackbar("Guardada localmente. Se enviará cuando haya conexión."))
                _events.send(DetalleIncidenciaEvent.NavigateBack)
            }
        }
    }
    private suspend fun copyUriToInternalStorage(context: Context, uri: Uri): Uri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (uri.scheme == "file") return@withContext uri
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext uri
            val fileName = "img_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(4)}.jpg"
            val dir = java.io.File(context.filesDir, "incidencias_images")
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, fileName)
            java.io.FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            uri
        }
    }
}
