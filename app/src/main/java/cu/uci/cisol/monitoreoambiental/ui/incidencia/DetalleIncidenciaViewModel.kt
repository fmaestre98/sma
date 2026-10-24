package cu.uci.cisol.monitoreoambiental.ui.incidencia

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Localidad
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetalleIncidenciaState(
    val incidenciaId: String = "",
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
    val fechaFormateada: String = "",
    val tematicaId: String? = null,
    val localidadId: String? = null,
    val descripcion: String = "",
    val address: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val imagenesUris: List<Uri> = emptyList(),
    val tematicas: List<Tematica> = emptyList(),
    val localidades: List<Localidad> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val tematicaVisible: String
        get() = themeName
            ?: tematicas.firstOrNull { it.id == tematicaId }?.name
            ?: tematicaId
            ?: "—"

    val localidadVisible: String
        get() = localityName
            ?: localidades.firstOrNull { it.id == localidadId }
                ?.let { "${it.name} (${it.municipality})" }
            ?: localidadId
            ?: "—"
}

sealed interface DetalleIncidenciaEvent {
    object NavigateBack : DetalleIncidenciaEvent
    data class ShowSnackbar(val message: String) : DetalleIncidenciaEvent
}

class DetalleIncidenciaViewModel(
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

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("es", "ES"))

    init {
        observeCatalogs()
        val id = incidenciaIdArg
        if (id.isNullOrBlank() || id == "{incidenciaId}") {
            showNotFound()
        } else {
            cargarIncidencia(id)
        }
    }

    private fun observeCatalogs() {
        viewModelScope.launch {
            tematicaRepository.observeTematicas().collect { tematicas ->
                _state.update { it.copy(tematicas = tematicas) }
            }
        }
        viewModelScope.launch {
            localidadRepository.observeLocalidades().collect { localidades ->
                _state.update { it.copy(localidades = localidades) }
            }
        }
    }

    private fun cargarIncidencia(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            var incidencia = incidenciaRepository.obtenerPorId(id)

            if (incidencia == null) {
                val syncResult = incidenciaRepository.sincronizarDetalle(id)
                if (syncResult.isSuccess) incidencia = incidenciaRepository.obtenerPorId(id)
            } else if (!incidencia.esBorrador && incidencia.serverId != null) {
                // Refresh remote details, including evidence URLs; retain the local copy if offline.
                val syncResult = incidenciaRepository.sincronizarDetalle(id)
                if (syncResult.isSuccess) incidencia = incidenciaRepository.obtenerPorId(id)
            }

            if (incidencia == null || incidencia.esBorrador) {
                showNotFound()
                return@launch
            }

            applyIncidencia(incidencia)
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun applyIncidencia(incidencia: Incidencia) {
        _state.update {
            it.copy(
                isLoading = false,
                incidenciaId = incidencia.id,
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
                fechaFormateada = dateFormatter.format(Date(incidencia.fechaHora)),
                tematicaId = incidencia.tematicaId,
                localidadId = incidencia.localidadId,
                descripcion = incidencia.descripcion,
                address = incidencia.address.orEmpty(),
                latitud = incidencia.latitud,
                longitud = incidencia.longitud,
                imagenesUris = incidencia.imagenesPaths.map { Uri.parse(it) },
            )
        }
    }

    private fun showNotFound() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = false, error = "Incidencia no encontrada") }
            _events.send(DetalleIncidenciaEvent.ShowSnackbar("Incidencia no encontrada"))
            _events.send(DetalleIncidenciaEvent.NavigateBack)
        }
    }
}
