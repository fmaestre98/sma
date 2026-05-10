package cu.uci.cisol.monitoreoambiental.ui.incidencia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
import cu.uci.cisol.monitoreoambiental.domain.repository.EstadoRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.TematicaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IncidenciaUi(
    val id: String,
    val tematica: String,
    val descripcionCorta: String,
    val estadoNombre: String,
    val estadoColorHex: String,
    val esBorrador: Boolean,
    val horaFormateada: String,
    val fechaAbsoluta: Long // For sorting
)

enum class IncidenciaFiltro(val label: String) {
    TODAS("Todas"),
    BORRADORES("Borradores"),
    ENVIADAS("Enviadas")
}

data class MisIncidenciasState(
    val incidenciasPorFecha: Map<String, List<IncidenciaUi>> = emptyMap(),
    val filtro: IncidenciaFiltro = IncidenciaFiltro.TODAS,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface MisIncidenciasAction {
    data class OnFiltroChange(val filtro: IncidenciaFiltro) : MisIncidenciasAction
}

class MisIncidenciasViewModel(
    savedStateHandle: SavedStateHandle,
    private val incidenciaRepository: IncidenciaRepository,
    private val tematicaRepository: TematicaRepository,
    private val estadoRepository: EstadoRepository
) : ViewModel() {

    private val filtroArg = savedStateHandle.get<String>("filtro")

    private val _state = MutableStateFlow(MisIncidenciasState(isLoading = true))
    val state: StateFlow<MisIncidenciasState> = _state.asStateFlow()

    private var todasIncidencias: List<IncidenciaUi> = emptyList()

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale("es", "ES"))

    init {
        val initialFilter = try {
            if (filtroArg != null) IncidenciaFiltro.valueOf(filtroArg) else IncidenciaFiltro.TODAS
        } catch (e: Exception) {
            IncidenciaFiltro.TODAS
        }
        
        _state.update { it.copy(filtro = initialFilter) }

        viewModelScope.launch {
            combine(
                incidenciaRepository.observarTodas(),
                tematicaRepository.observeTematicas(),
                estadoRepository.observeEstados()
            ) { incidencias, tematicas, estados ->
                val tematicaMap = tematicas.associateBy { it.id }
                val estadoMap = estados.associateBy { it.id }
                incidencias.map { it.toUi(tematicaMap, estadoMap) }
            }
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { listaUi ->
                    todasIncidencias = listaUi
                    aplicarFiltro()
                }
        }
    }

    fun onAction(action: MisIncidenciasAction) {
        when (action) {
            is MisIncidenciasAction.OnFiltroChange -> {
                _state.update { it.copy(filtro = action.filtro) }
                aplicarFiltro()
            }
        }
    }

    private fun aplicarFiltro() {
        val filtroActual = _state.value.filtro
        val filtradas = when (filtroActual) {
            IncidenciaFiltro.TODAS -> todasIncidencias
            IncidenciaFiltro.BORRADORES -> todasIncidencias.filter { it.esBorrador }
            IncidenciaFiltro.ENVIADAS -> todasIncidencias.filter { !it.esBorrador }
        }

        val agrupadas = filtradas.groupBy { it.getFechaFormateada() }
        
        _state.update { 
            it.copy(
                incidenciasPorFecha = agrupadas,
                isLoading = false
            ) 
        }
    }

    private fun Incidencia.toUi(tematicaMap: Map<String, Tematica>, estadoMap: Map<String, EstadoIncidencia>): IncidenciaUi {
        val date = Date(fechaHora)
        val estadoLocal = estadoMap[estadoId]
        
        return IncidenciaUi(
            id = id,
            tematica = tematicaMap[tematicaId]?.name ?: tematicaId,
            descripcionCorta = if (descripcion.length > 80) "${descripcion.take(80)}..." else descripcion,
            estadoNombre = if (esBorrador) "Borrador" else estadoLocal?.name ?: "Desconocido",
            estadoColorHex = if (esBorrador) "#9E9E9E" else estadoLocal?.colorHex ?: "#9E9E9E",
            esBorrador = esBorrador,
            horaFormateada = timeFormatter.format(date),
            fechaAbsoluta = fechaHora
        )
    }

    private fun IncidenciaUi.getFechaFormateada(): String {
        return dateFormatter.format(Date(fechaAbsoluta))
    }
}

