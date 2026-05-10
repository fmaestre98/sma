package cu.uci.cisol.monitoreoambiental.ui.incidencia

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.TematicaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class FormMode { CREAR, EDITAR }

data class NuevaIncidenciaState(
    val incidenciaId: String = "",
    val tematicaId: String? = null,
    val tematicas: List<Tematica> = emptyList(),
    val descripcion: String = "",
    val imagenesUris: List<Uri> = emptyList(),
    val latitud: Double? = null,
    val longitud: Double? = null,
    val obteniendoCoordenadas: Boolean = false,
    val modo: FormMode = FormMode.CREAR,
    val estado: EstadoIncidencia = EstadoIncidencia.BORRADOR,
    val imagenResolucionUri: Uri? = null,
    val isLoading: Boolean = false,
    val showMapDialog: Boolean = false,
    val error: String? = null
) {
    /** Convenience to get the currently selected Tematica object from the list. */
    val selectedTematica: Tematica?
        get() = tematicas.firstOrNull { it.id == tematicaId }
}

sealed interface NuevaIncidenciaAction {
    data class OnTematicaChange(val tematicaId: String) : NuevaIncidenciaAction
    data class OnDescripcionChange(val descripcion: String) : NuevaIncidenciaAction
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
    object OnGuardarBorrador : NuevaIncidenciaAction
    object OnEnviarAlServidor : NuevaIncidenciaAction
    object OnMarcarComoResuelta : NuevaIncidenciaAction
}

sealed interface NuevaIncidenciaEvent {
    object NavigateBack : NuevaIncidenciaEvent
    data class ShowSnackbar(val message: String) : NuevaIncidenciaEvent
}

class NuevaIncidenciaViewModel(
    savedStateHandle: SavedStateHandle,
    private val incidenciaRepository: IncidenciaRepository,
    private val tematicaRepository: TematicaRepository
) : ViewModel() {

    private val incidenciaIdArg: String? = savedStateHandle["incidenciaId"]

    private val _state = MutableStateFlow(NuevaIncidenciaState())
    val state: StateFlow<NuevaIncidenciaState> = _state.asStateFlow()

    private val _events = Channel<NuevaIncidenciaEvent>()
    val events = _events.receiveAsFlow()

    init {
        observeTematicas()

        if (!incidenciaIdArg.isNullOrEmpty() && incidenciaIdArg != "{incidenciaId}") {
            cargarIncidencia(incidenciaIdArg)
        } else {
            _state.update { 
                it.copy(
                    incidenciaId = UUID.randomUUID().toString(),
                    modo = FormMode.CREAR
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

    private fun cargarIncidencia(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val incidencia = incidenciaRepository.obtenerPorId(id)
            if (incidencia != null) {
                _state.update {
                    it.copy(
                        incidenciaId = incidencia.id,
                        tematicaId = incidencia.tematicaId,
                        descripcion = incidencia.descripcion,
                        imagenesUris = incidencia.imagenesPaths.map { Uri.parse(it) },
                        imagenResolucionUri = incidencia.imagenResolucionPath?.let { path -> Uri.parse(path) },
                        latitud = incidencia.latitud,
                        longitud = incidencia.longitud,
                        modo = FormMode.EDITAR,
                        estado = incidencia.estado,
                        isLoading = false
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
            is NuevaIncidenciaAction.OnDescripcionChange -> {
                _state.update { it.copy(descripcion = action.descripcion) }
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
                _state.update {
                    it.copy(
                        latitud = action.lat,
                        longitud = action.lng,
                        obteniendoCoordenadas = false
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
                _state.update {
                    it.copy(
                        latitud = action.lat,
                        longitud = action.lng,
                        showMapDialog = false
                    )
                }
            }
            is NuevaIncidenciaAction.OnGuardarBorrador -> {
                guardar(EstadoIncidencia.BORRADOR)
            }
            is NuevaIncidenciaAction.OnEnviarAlServidor -> {
                guardar(EstadoIncidencia.NUEVA)
            }
            is NuevaIncidenciaAction.OnMarcarComoResuelta -> {
                marcarResuelta()
            }
        }
    }

    private fun guardar(estadoFinal: EstadoIncidencia) {
        val currentState = _state.value
        if (currentState.tematicaId == null || currentState.descripcion.isBlank()) {
            viewModelScope.launch {
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Por favor complete temática y descripción"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val incidencia = Incidencia(
                id = currentState.incidenciaId,
                tematicaId = currentState.tematicaId,
                descripcion = currentState.descripcion,
                imagenesPaths = currentState.imagenesUris.map { it.toString() },
                imagenResolucionPath = currentState.imagenResolucionUri?.toString(),
                latitud = currentState.latitud,
                longitud = currentState.longitud,
                fechaHora = System.currentTimeMillis(),
                estado = estadoFinal,
                sincronizado = false // By default is false when saving via the form
            )

            val result = incidenciaRepository.guardar(incidencia)
            if (result.isSuccess) {
                if (estadoFinal == EstadoIncidencia.NUEVA && cu.uci.cisol.monitoreoambiental.core.config.AppConfig.USE_MOCK_INCIDENCIAS) {
                    delay(1000) // mock delay
                    incidenciaRepository.enviarAlServidor(incidencia)
                }
                
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Incidencia guardada correctamente"))
                _events.send(NuevaIncidenciaEvent.NavigateBack)
            } else {
                _state.update { it.copy(isLoading = false) }
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Error al guardar: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    private fun marcarResuelta() {
        val currentState = _state.value
        // Al marcar como resuelta, primero cambiamos el estado y actualizamos DB
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = incidenciaRepository.actualizarEstado(
                id = currentState.incidenciaId,
                estado = EstadoIncidencia.RESUELTA,
                imagenResolucionPath = currentState.imagenResolucionUri?.toString()
            )
            
            if (result.isSuccess) {
                _state.update { it.copy(estado = EstadoIncidencia.RESUELTA, isLoading = false) }
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Marcada como resuelta"))
            } else {
                _state.update { it.copy(isLoading = false) }
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Error al marcar como resuelta"))
            }
        }
    }
}

