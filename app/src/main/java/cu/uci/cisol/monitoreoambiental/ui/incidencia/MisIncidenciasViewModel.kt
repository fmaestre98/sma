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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IncidenciaUi(
    val id: String,
    val tematica: String,
    val tematicaColor: String?,
    val descripcionCorta: String,
    val estadoNombre: String,
    val estadoColorHex: String,
    val criticality: String?,
    val criticalityColor: String?,
    val esBorrador: Boolean,
    val sincronizado: Boolean,
    val horaFormateada: String,
    val fechaAbsoluta: Long,
)

enum class IncidenciaFiltro(val label: String) {
    TODAS("Todas"),
    BORRADORES("Borradores"),
    ENVIADAS("Enviadas"),
}

data class MisIncidenciasState(
    val incidenciasPorFecha: Map<String, List<IncidenciaUi>> = emptyMap(),
    val filtro: IncidenciaFiltro = IncidenciaFiltro.TODAS,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val searchQuery: String = "",
)

sealed interface MisIncidenciasAction {
    data class OnFiltroChange(val filtro: IncidenciaFiltro) : MisIncidenciasAction
    object OnRefresh : MisIncidenciasAction
    data class OnSearchQueryChange(val query: String) : MisIncidenciasAction
    object OnLoadNextPage : MisIncidenciasAction
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MisIncidenciasViewModel(
    savedStateHandle: SavedStateHandle,
    private val incidenciaRepository: IncidenciaRepository,
    private val tematicaRepository: TematicaRepository,
    private val estadoRepository: EstadoRepository,
) : ViewModel() {

    private val filtroArg = savedStateHandle.get<String>("filtro")

    private val _state = MutableStateFlow(MisIncidenciasState(isLoading = true))
    val state: StateFlow<MisIncidenciasState> = _state.asStateFlow()

    private val filtroFlow = MutableStateFlow(IncidenciaFiltro.TODAS)
    private val searchFlow = MutableStateFlow("")
    private val limitFlow = MutableStateFlow(10)

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale("es", "ES"))

    init {
        val initialFilter =
            try {
                if (filtroArg != null) IncidenciaFiltro.valueOf(filtroArg) else IncidenciaFiltro.TODAS
            } catch (_: Exception) {
                IncidenciaFiltro.TODAS
            }

        filtroFlow.value = initialFilter
        _state.update { it.copy(filtro = initialFilter) }

        // Trigger initial sync
        syncPage(page = 1, search = "")

        viewModelScope.launch {
            combine(filtroFlow, searchFlow, limitFlow) { filtro, search, limit ->
                Triple(filtro, search, limit)
            }.flatMapLatest { (filtro, search, limit) ->
                combine(
                    incidenciaRepository.observarPaginado(filtro.name, search, limit, 0),
                    incidenciaRepository.obtenerCantidad(filtro.name, search),
                    tematicaRepository.observeTematicas(),
                    estadoRepository.observeEstados(),
                ) { list, totalCount, tematicas, estados ->
                    val tematicaMap = tematicas.associateBy { it.id }
                    val estadoMap = estados.associateBy { it.id }
                    val listUi = list.map { it.toUi(tematicaMap, estadoMap) }
                    val agrupadas = listUi.groupBy { it.getFechaFormateada() }
                    
                    _state.update {
                        it.copy(
                            incidenciasPorFecha = agrupadas,
                            totalCount = totalCount,
                            hasMore = list.size < totalCount,
                            isLoading = false
                        )
                    }
                }
            }.catch { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }.collect()
        }
    }

    fun onAction(action: MisIncidenciasAction) {
        when (action) {
            is MisIncidenciasAction.OnFiltroChange -> {
                filtroFlow.value = action.filtro
                _state.update { it.copy(filtro = action.filtro, currentPage = 1, isLoading = true) }
                limitFlow.value = 10
                syncPage(page = 1, search = searchFlow.value)
            }
            MisIncidenciasAction.OnRefresh -> {
                _state.update { it.copy(isLoading = true, error = null) }
                syncPage(page = 1, search = searchFlow.value)
            }
            is MisIncidenciasAction.OnSearchQueryChange -> {
                searchFlow.value = action.query
                _state.update { it.copy(searchQuery = action.query, currentPage = 1, isLoading = true) }
                limitFlow.value = 10
                syncPage(page = 1, search = action.query)
            }
            MisIncidenciasAction.OnLoadNextPage -> {
                val currentState = _state.value
                if (currentState.hasMore && !currentState.isLoading) {
                    val nextPage = currentState.currentPage + 1
                    _state.update { it.copy(currentPage = nextPage, isLoading = true) }
                    limitFlow.value = nextPage * currentState.pageSize
                    syncPage(page = nextPage, search = currentState.searchQuery)
                }
            }
        }
    }

    private fun syncPage(page: Int, search: String) {
        viewModelScope.launch {
            val result = incidenciaRepository.syncDesdeServidor(
                page = page,
                pageSize = 10,
                search = search.takeIf { it.isNotBlank() },
                ordering = "-updated_at"
            )
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Error al sincronizar",
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun Incidencia.toUi(
        tematicaMap: Map<String, Tematica>,
        estadoMap: Map<String, EstadoIncidencia>,
    ): IncidenciaUi {
        val date = Date(fechaHora)
        val estadoLocal = estadoMap[estadoId]
        
        val statusNameResolved = if (esBorrador) "Borrador" else statusName ?: estadoLocal?.name ?: estadoId
        val statusColorResolved = if (esBorrador) "#9E9E9E" else statusColor ?: estadoLocal?.colorHex ?: "#9E9E9E"

        return IncidenciaUi(
            id = id,
            tematica = themeName ?: tematicaMap[tematicaId]?.name ?: tematicaId,
            tematicaColor = themeColor ?: "#64748b",
            descripcionCorta = if (descripcion.length > 80) "${descripcion.take(80)}..." else descripcion,
            estadoNombre = statusNameResolved,
            estadoColorHex = statusColorResolved,
            criticality = criticality ?: "No evaluada",
            criticalityColor = criticalityColor ?: "#808080",
            esBorrador = esBorrador,
            sincronizado = sincronizado,
            horaFormateada = timeFormatter.format(date),
            fechaAbsoluta = fechaHora,
        )
    }

    private fun IncidenciaUi.getFechaFormateada(): String {
        return dateFormatter.format(Date(fechaAbsoluta))
    }
}
