package cu.uci.cisol.monitoreoambiental.ui.incidencia

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Localidad
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
import cu.uci.cisol.monitoreoambiental.domain.repository.EstadoRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.LocalidadRepository
import cu.uci.cisol.monitoreoambiental.data.work.IncidenciaSyncScheduler
import cu.uci.cisol.monitoreoambiental.domain.repository.TematicaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

enum class FormMode { CREAR, EDITAR }

data class NuevaIncidenciaState(
    val incidenciaId: String = "",
    val tematicaId: String? = null,
    val tematicas: List<Tematica> = emptyList(),
    val localidadId: String? = null,
    val localidades: List<Localidad> = emptyList(),
    val descripcion: String = "",
    val direccion: String = "",
    val imagenesUris: List<Uri> = emptyList(),
    val latitud: Double? = null,
    val longitud: Double? = null,
    val ubicacionOrigen: String? = null,
    val obteniendoCoordenadas: Boolean = false,
    val modo: FormMode = FormMode.CREAR,
    val estadoId: String? = null,
    val esBorrador: Boolean = true,
    val estados: List<EstadoIncidencia> = emptyList(),
    val imagenResolucionUri: Uri? = null,
    val isLoading: Boolean = false,
    val showMapDialog: Boolean = false,
    val error: String? = null,
) {
    val selectedTematica: Tematica?
        get() = tematicas.firstOrNull { it.id == tematicaId }

    val selectedLocalidad: Localidad?
        get() = localidades.firstOrNull { it.id == localidadId }

    val estadoLocal: EstadoIncidencia?
        get() = estados.firstOrNull { it.id == estadoId }

    val estadoNombre: String
        get() = if (esBorrador) "Borrador" else estadoLocal?.name ?: "Desconocido"

    val estadoColorHex: String
        get() = if (esBorrador) "#9E9E9E" else estadoLocal?.colorHex ?: "#9E9E9E"
}

sealed interface NuevaIncidenciaAction {
    data class OnTematicaChange(val tematicaId: String) : NuevaIncidenciaAction
    data class OnLocalidadChange(val localidadId: String) : NuevaIncidenciaAction
    data class OnDescripcionChange(val descripcion: String) : NuevaIncidenciaAction
    data class OnDireccionChange(val direccion: String) : NuevaIncidenciaAction
    data class OnFotoTomada(val uri: Uri?) : NuevaIncidenciaAction
    data class OnFotoSeleccionada(val uris: List<Uri>) : NuevaIncidenciaAction
    data class OnFotoEliminada(val uri: Uri) : NuevaIncidenciaAction
    data class OnFotoResolucionTomada(val uri: Uri?) : NuevaIncidenciaAction
    object OnObtenerCoordenadas : NuevaIncidenciaAction
    data class OnCoordenadasObtenidas(val lat: Double, val lng: Double) : NuevaIncidenciaAction
    object OnCoordenadasError : NuevaIncidenciaAction
    object OnSeleccionarEnMapa : NuevaIncidenciaAction
    object OnCerrarMapa : NuevaIncidenciaAction
    data class OnMapLocationSelected(val lat: Double, val lng: Double) : NuevaIncidenciaAction
    object OnLimpiarUbicacion : NuevaIncidenciaAction
    object OnGuardarBorrador : NuevaIncidenciaAction
    object OnEnviarAlServidor : NuevaIncidenciaAction
    object OnMarcarComoResuelta : NuevaIncidenciaAction
}

sealed interface NuevaIncidenciaEvent {
    object NavigateBack : NuevaIncidenciaEvent
    data class ShowSnackbar(val message: String) : NuevaIncidenciaEvent
}

class NuevaIncidenciaViewModel(
    private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val incidenciaRepository: IncidenciaRepository,
    private val tematicaRepository: TematicaRepository,
    private val estadoRepository: EstadoRepository,
    private val localidadRepository: LocalidadRepository,
) : ViewModel() {

    private val incidenciaIdArg: String? = savedStateHandle["incidenciaId"]

    private val _state = MutableStateFlow(NuevaIncidenciaState())
    val state: StateFlow<NuevaIncidenciaState> = _state.asStateFlow()

    private val _events = Channel<NuevaIncidenciaEvent>()
    val events = _events.receiveAsFlow()

    init {
        observeTematicas()
        observeLocalidades()
        observeEstados()

        if (!incidenciaIdArg.isNullOrEmpty() && incidenciaIdArg != "{incidenciaId}") {
            cargarIncidencia(incidenciaIdArg)
        } else {
            _state.update {
                it.copy(
                    incidenciaId = UUID.randomUUID().toString(),
                    modo = FormMode.CREAR,
                )
            }
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
                    onAction(NuevaIncidenciaAction.OnLocalidadChange(localidades.first().id))
                }
            }
        }
    }

    private fun observeEstados() {
        viewModelScope.launch {
            estadoRepository.observeEstados().collect { estados ->
                _state.update { it.copy(estados = estados) }
            }
        }
    }

    private fun cargarIncidencia(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val incidencia = incidenciaRepository.obtenerPorId(id)
            if (incidencia != null) {
                _state.update {
                    it.copy(
                        incidenciaId = incidencia.id,
                        tematicaId = incidencia.tematicaId,
                        localidadId = incidencia.localidadId,
                        descripcion = incidencia.descripcion,
                        direccion = incidencia.address ?: "",
                        imagenesUris = incidencia.imagenesPaths.map { Uri.parse(it) },
                        imagenResolucionUri =
                            incidencia.imagenResolucionPath?.let { path -> Uri.parse(path) },
                        latitud = incidencia.latitud,
                        longitud = incidencia.longitud,
                        modo = FormMode.EDITAR,
                        estadoId = incidencia.estadoId,
                        esBorrador = incidencia.esBorrador,
                        isLoading = false,
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, error = "Incidencia no encontrada") }
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Error al cargar incidencia"))
                _events.send(NuevaIncidenciaEvent.NavigateBack)
            }
        }
    }

    fun onAction(action: NuevaIncidenciaAction) {
        when (action) {
            is NuevaIncidenciaAction.OnTematicaChange -> {
                _state.update { it.copy(tematicaId = action.tematicaId) }
            }
            is NuevaIncidenciaAction.OnLocalidadChange -> {
                _state.update { it.copy(localidadId = action.localidadId) }
            }
            is NuevaIncidenciaAction.OnDescripcionChange -> {
                _state.update { it.copy(descripcion = action.descripcion) }
            }
            is NuevaIncidenciaAction.OnDireccionChange -> {
                _state.update { it.copy(direccion = action.direccion) }
            }
            is NuevaIncidenciaAction.OnFotoTomada -> {
                if (action.uri != null && _state.value.imagenesUris.size < 5) {
                    _state.update { it.copy(imagenesUris = it.imagenesUris + action.uri) }
                }
            }
            is NuevaIncidenciaAction.OnFotoSeleccionada -> {
                val current = _state.value.imagenesUris
                val availableSpace = 5 - current.size
                if (availableSpace > 0) {
                    val newUris = action.uris.take(availableSpace)
                    _state.update { it.copy(imagenesUris = current + newUris) }
                }
            }
            is NuevaIncidenciaAction.OnFotoEliminada -> {
                _state.update { it.copy(imagenesUris = it.imagenesUris - action.uri) }
            }
            is NuevaIncidenciaAction.OnFotoResolucionTomada -> {
                if (action.uri != null) {
                    _state.update { it.copy(imagenResolucionUri = action.uri) }
                }
            }
            is NuevaIncidenciaAction.OnObtenerCoordenadas -> {
                _state.update { it.copy(obteniendoCoordenadas = true) }
            }
            is NuevaIncidenciaAction.OnCoordenadasObtenidas -> {
                val latitud = action.lat.toString().take(10).toDouble()
                val longitud = action.lng.toString().take(10).toDouble()
                _state.update {
                    it.copy(
                        latitud = latitud,
                        longitud = longitud,
                        ubicacionOrigen = "GPS",
                        obteniendoCoordenadas = false,
                    )
                }
            }
            is NuevaIncidenciaAction.OnCoordenadasError -> {
                _state.update { it.copy(obteniendoCoordenadas = false) }
                viewModelScope.launch {
                    _events.send(NuevaIncidenciaEvent.ShowSnackbar("Error al obtener ubicación"))
                }
            }
            is NuevaIncidenciaAction.OnSeleccionarEnMapa -> {
                _state.update { it.copy(showMapDialog = true) }
            }
            is NuevaIncidenciaAction.OnCerrarMapa -> {
                _state.update { it.copy(showMapDialog = false) }
            }
            is NuevaIncidenciaAction.OnMapLocationSelected -> {
                val latitud = action.lat.toString().take(10).toDouble()
                val longitud = action.lng.toString().take(10).toDouble()
                _state.update {
                    it.copy(
                        latitud = latitud,
                        longitud = longitud,
                        ubicacionOrigen = "Mapa",
                        showMapDialog = false,
                    )
                }
            }
            is NuevaIncidenciaAction.OnLimpiarUbicacion -> {
                _state.update {
                    it.copy(
                        latitud = null,
                        longitud = null,
                        ubicacionOrigen = null,
                    )
                }
            }
            is NuevaIncidenciaAction.OnGuardarBorrador -> {
                guardar(esBorradorFinal = true)
            }
            is NuevaIncidenciaAction.OnEnviarAlServidor -> {
                guardar(esBorradorFinal = false)
            }
            is NuevaIncidenciaAction.OnMarcarComoResuelta -> {
                marcarResuelta()
            }
        }
    }

    private fun guardar(esBorradorFinal: Boolean) {
        val currentState = _state.value
        if (currentState.tematicaId == null || currentState.descripcion.isBlank() || currentState.direccion.isBlank()) {
            viewModelScope.launch {
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Por favor complete temática, descripción y dirección"))
            }
            return
        }

        if (currentState.imagenesUris.isEmpty()) {
            viewModelScope.launch {
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Por favor agregue al menos una foto de evidencia"))
            }
            return
        }

        if (!esBorradorFinal && currentState.localidadId == null) {
            viewModelScope.launch {
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Seleccione una localidad para enviar"))
            }
            return
        }

        val baseEstadoId = currentState.estados.firstOrNull { it.isBase }?.id
        if (!esBorradorFinal && baseEstadoId == null) {
            viewModelScope.launch {
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Error: Estados no cargados. Sincronice primero."))
            }
            return
        }

        val resolvedEstadoId =
            if (esBorradorFinal) {
                currentState.estadoId ?: baseEstadoId ?: ""
            } else {
                baseEstadoId!!
            }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val incidencia =
                    Incidencia(
                        id = currentState.incidenciaId,
                        tematicaId = currentState.tematicaId!!,
                        localidadId = currentState.localidadId,
                        descripcion = currentState.descripcion,
                        address = currentState.direccion,
                        imagenesPaths = currentState.imagenesUris.map { it.toString() },
                        imagenResolucionPath = currentState.imagenResolucionUri?.toString(),
                        latitud = currentState.latitud,
                        longitud = currentState.longitud,
                        fechaHora = System.currentTimeMillis(),
                        estadoId = resolvedEstadoId,
                        esBorrador = esBorradorFinal,
                        sincronizado = false,
                    )

                val saveResult = incidenciaRepository.guardar(incidencia)
                if (saveResult.isFailure) {
                    _events.send(
                        NuevaIncidenciaEvent.ShowSnackbar(
                            "Error al guardar: ${saveResult.exceptionOrNull()?.message}",
                        ),
                    )
                    return@launch
                }

                if (!esBorradorFinal) {
                    val sendResult = withTimeoutOrNull(18000L) {
                        incidenciaRepository.enviarAlServidor(incidencia)
                    }
                    if (sendResult == null || sendResult.isFailure) {
                        IncidenciaSyncScheduler.enqueueUploadPending(appContext)
                        _events.send(
                            NuevaIncidenciaEvent.ShowSnackbar(
                                "Guardada localmente. Se enviará cuando haya conexión.",
                            ),
                        )
                        return@launch
                    }
                }

                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Incidencia guardada correctamente"))
                _events.send(NuevaIncidenciaEvent.NavigateBack)
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun marcarResuelta() {
        val currentState = _state.value
        val resueltaEstadoId = currentState.estados.firstOrNull { it.isFinal }?.id

        if (resueltaEstadoId == null) {
            viewModelScope.launch {
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("No se encontró el estado final"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result =
                incidenciaRepository.actualizarEstado(
                    id = currentState.incidenciaId,
                    estadoId = resueltaEstadoId,
                    esBorrador = false,
                    imagenResolucionPath = currentState.imagenResolucionUri?.toString(),
                )

            if (result.isSuccess) {
                _state.update {
                    it.copy(estadoId = resueltaEstadoId, esBorrador = false, isLoading = false)
                }
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Marcada como resuelta"))
            } else {
                _state.update { it.copy(isLoading = false) }
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Error al marcar como resuelta"))
            }
        }
    }
}
