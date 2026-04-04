package cu.uci.cisol.monitoreoambiental.ui.incidencia

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.usecase.ActualizarEstadoIncidenciaUseCase
import cu.uci.cisol.monitoreoambiental.domain.usecase.GuardarIncidenciaUseCase
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
    val tematica: Tematica? = null,
    val descripcion: String = "",
    val imagenUri: Uri? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val obteniendoCoordenadas: Boolean = false,
    val modo: FormMode = FormMode.CREAR,
    val estado: EstadoIncidencia = EstadoIncidencia.BORRADOR,
    val imagenResolucionUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface NuevaIncidenciaAction {
    data class OnTematicaChange(val tematica: Tematica) : NuevaIncidenciaAction
    data class OnDescripcionChange(val descripcion: String) : NuevaIncidenciaAction
    data class OnFotoTomada(val uri: Uri?) : NuevaIncidenciaAction
    data class OnFotoResolucionTomada(val uri: Uri?) : NuevaIncidenciaAction
    object OnObtenerCoordenadas : NuevaIncidenciaAction
    data class OnCoordenadasObtenidas(val lat: Double, val lng: Double) : NuevaIncidenciaAction
    object OnCoordenadasError : NuevaIncidenciaAction
    object OnSeleccionarEnMapa : NuevaIncidenciaAction
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
    private val guardarIncidenciaUseCase: GuardarIncidenciaUseCase,
    private val actualizarEstadoUseCase: ActualizarEstadoIncidenciaUseCase,
    private val incidenciaRepository: IncidenciaRepository
) : ViewModel() {

    private val incidenciaIdArg: String? = savedStateHandle["incidenciaId"]

    private val _state = MutableStateFlow(NuevaIncidenciaState())
    val state: StateFlow<NuevaIncidenciaState> = _state.asStateFlow()

    private val _events = Channel<NuevaIncidenciaEvent>()
    val events = _events.receiveAsFlow()

    init {
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

    private fun cargarIncidencia(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val incidencia = incidenciaRepository.obtenerPorId(id)
            if (incidencia != null) {
                _state.update {
                    it.copy(
                        incidenciaId = incidencia.id,
                        tematica = incidencia.tematica,
                        descripcion = incidencia.descripcion,
                        imagenUri = incidencia.imagenPath?.let { path -> Uri.parse(path) },
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
                _state.update { it.copy(tematica = action.tematica) }
            }
            is NuevaIncidenciaAction.OnDescripcionChange -> {
                _state.update { it.copy(descripcion = action.descripcion) }
            }
            is NuevaIncidenciaAction.OnFotoTomada -> {
                if (action.uri != null) {
                    _state.update { it.copy(imagenUri = action.uri) }
                }
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
                viewModelScope.launch {
                    _events.send(NuevaIncidenciaEvent.ShowSnackbar("Selección en mapa en desarrollo"))
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
        if (currentState.tematica == null || currentState.descripcion.isBlank()) {
            viewModelScope.launch {
                _events.send(NuevaIncidenciaEvent.ShowSnackbar("Por favor complete temática y descripción"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val incidencia = Incidencia(
                id = currentState.incidenciaId,
                tematica = currentState.tematica,
                descripcion = currentState.descripcion,
                imagenPath = currentState.imagenUri?.toString(),
                imagenResolucionPath = currentState.imagenResolucionUri?.toString(),
                latitud = currentState.latitud,
                longitud = currentState.longitud,
                fechaHora = System.currentTimeMillis(),
                estado = estadoFinal,
                sincronizado = false // By default is false when saving via the form
            )

            val result = guardarIncidenciaUseCase(incidencia)
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
            val result = actualizarEstadoUseCase(
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
