package cu.uci.cisol.monitoreoambiental.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.data.work.IncidenciaSyncScheduler
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
import cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.EstadoRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.LocalidadRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.TematicaRepository
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import cu.uci.cisol.monitoreoambiental.ui.incidencia.IncidenciaUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HomeFiltro {
    const val TODAS = "TODAS"
    const val BORRADOR = "BORRADOR"
    const val PENDIENTES_ENVIAR = "PENDIENTES_ENVIAR"
}

// -----------------------
// Configuración de datos de prueba
// Cambia a false para cargar datos reales desde la base de datos local
// -----------------------
private const val USE_MOCK_DATA = false

private val MESES_CORTOS = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun",
    "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

data class EstadoCount(val id: String, val nombre: String, val colorHex: String, val count: Int)

data class EstadisticaItem(
    val id: String,
    val nombre: String,
    val colorHex: String,
    val count: Int,
)

data class MesDisponible(val year: Int, val month: Int, val label: String)

data class MonthlyData(
    val mes: String,
    val counts: Map<String, Int>
)

data class HomeUiState(
    val incidencias: List<Incidencia> = emptyList(),
    val conteosPorEstado: List<EstadoCount> = emptyList(),
    val estadisticas: List<EstadisticaItem> = emptyList(),
    val datosMensuales: List<MonthlyData> = emptyList(),
    val totalBorradores: Int = 0,
    val totalPendientes: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val logoutCompleted: Boolean = false,
    val tematicaSyncError: String? = null,
    val searchQuery: String = "",
    val selectedFiltro: String = HomeFiltro.TODAS,
    val selectedMes: MesDisponible? = null,
    val selectedEstadoDropdown: String? = null,
    val mesesDisponibles: List<MesDisponible> = emptyList(),
    val incidenciasFiltradas: List<IncidenciaUi> = emptyList()
)

class HomeViewModel(
    private val appContext: Context,
    private val repository: IncidenciaRepository,
    private val authRepository: AuthRepository,
    private val tematicaRepository: TematicaRepository,
    private val estadoRepository: EstadoRepository,
    private val localidadRepository: LocalidadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val searchFlow = MutableStateFlow("")
    private val filtroFlow = MutableStateFlow(HomeFiltro.TODAS)
    private val mesFlow = MutableStateFlow<MesDisponible?>(null)
    private val estadoDropdownFlow = MutableStateFlow<String?>(null)

    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale("es", "ES"))
    private val fechaCompletaFormatter = SimpleDateFormat("dd/MM/yy - hh:mm a", Locale("es", "ES"))

    init {
        syncTematicas()
        syncEstados()
        syncLocalities()
        cargarDatos()
        IncidenciaSyncScheduler.enqueueUploadPending(appContext)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(logoutCompleted = true)
        }
    }

    fun onSearchQueryChange(query: String) {
        searchFlow.value = query
    }

    fun onFiltroChange(filtro: String) {
        var curr = filtro
        if (filtroFlow.value == curr){
            filtroFlow.value = HomeFiltro.TODAS
            curr = HomeFiltro.TODAS
        } else {
            filtroFlow.value = curr
        }
        estadoDropdownFlow.value = when (curr) {
            HomeFiltro.TODAS, HomeFiltro.BORRADOR, HomeFiltro.PENDIENTES_ENVIAR -> null
            else -> curr
        }
    }

    fun onMesChange(mes: MesDisponible?) {
        mesFlow.value = mes
    }

    fun onEstadoDropdownChange(estadoId: String?) {
        estadoDropdownFlow.value = estadoId
        if (estadoId != null) {
            filtroFlow.value = estadoId
        } else if (filtroFlow.value !in listOf(
                HomeFiltro.TODAS,
                HomeFiltro.BORRADOR,
                HomeFiltro.PENDIENTES_ENVIAR
            )
        ) {
            filtroFlow.value = HomeFiltro.TODAS
        }
    }

    fun onTodasIncidenciasClick() {
        onFiltroChange(HomeFiltro.TODAS)
    }

    private fun syncTematicas() {
        viewModelScope.launch {
            when (val result = tematicaRepository.syncTematicas()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(tematicaSyncError = null)
                }
                is Result.Error -> {
                    tematicaRepository.observeTematicas().collect { tematicas ->
                        if (tematicas.isEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                tematicaSyncError = "Error al sincronizar temáticas. Verifique su conexión."
                            )
                        }
                        return@collect
                    }
                }
            }
        }
    }

    private fun syncEstados() {
        viewModelScope.launch {
            estadoRepository.syncEstados()
        }
    }

    private fun syncLocalities() {
        viewModelScope.launch {
            localidadRepository.syncLocalities()
        }
    }

    private fun cargarDatos() {
        if (USE_MOCK_DATA) {
            cargarDatosMock()
        } else {
            cargarDatosReales()
        }
    }

    private fun cargarDatosMock() {
        val conteosSimulados = listOf(
            EstadoCount("pendiente-id", "Pendiente", "#FF9800", 8),
            EstadoCount("nueva-id", "Nueva", "#2196F3", 15),
            EstadoCount("aceptada-id", "Aceptada", "#4CAF50", 42),
            EstadoCount("resuelta-id", "Resuelta", "#009688", 38),
            EstadoCount("rechazada-id", "Rechazada", "#F44336", 20)
        )

        val meses = listOf("E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        val datosMensuales = meses.mapIndexed { index, mes ->
            MonthlyData(
                mes = mes,
                counts = mapOf(
                    "nueva-id" to (5 + (index * 3) % 20),
                    "aceptada-id" to (10 + (index * 5) % 35),
                    "rechazada-id" to (3 + (index * 2) % 15)
                )
            )
        }

        val estadisticas = buildEstadisticas(conteosSimulados, totalBorradores = 3, totalPendientes = 5)

        _uiState.value = HomeUiState(
            conteosPorEstado = conteosSimulados,
            estadisticas = estadisticas,
            datosMensuales = datosMensuales,
            isLoading = false,
            totalBorradores = 3,
            totalPendientes = 5
        )
    }

    private fun cargarDatosReales() {
        viewModelScope.launch {
            combine(
                listOf(
                    repository.observarTodas(),
                    estadoRepository.observeEstados(),
                    tematicaRepository.observeTematicas(),
                    searchFlow,
                    filtroFlow,
                    mesFlow,
                    estadoDropdownFlow,
                )
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                procesarIncidencias(
                    incidencias = values[0] as List<Incidencia>,
                    estados = values[1] as List<EstadoIncidencia>,
                    tematicas = values[2] as List<Tematica>,
                    search = values[3] as String,
                    filtro = values[4] as String,
                    mes = values[5] as MesDisponible?,
                    estadoDropdown = values[6] as String?,
                )
            }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { nuevoEstado ->
                    _uiState.value = nuevoEstado
                }
        }
    }

    private fun buildEstadisticas(
        conteosPorEstado: List<EstadoCount>,
        totalBorradores: Int,
        totalPendientes: Int,
    ): List<EstadisticaItem> {
        val serverItems = conteosPorEstado.map { estado ->
            EstadisticaItem(estado.id, estado.nombre, estado.colorHex, estado.count)
        }
        val localItems = listOf(
            EstadisticaItem(HomeFiltro.BORRADOR, "Borrador", "#A9A8A8", totalBorradores),
            EstadisticaItem(
                HomeFiltro.PENDIENTES_ENVIAR,
                "Pendientes por sincronizar",
                "#FF9800",
                totalPendientes
            ),
        )
        return serverItems + localItems
    }

    private fun buildMesesDisponibles(incidencias: List<Incidencia>): List<MesDisponible> {
        return incidencias
            .map { incidencia ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = incidencia.fechaHora
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                MesDisponible(
                    year = year,
                    month = month,
                    label = "${MESES_CORTOS[month]} $year"
                )
            }
            .distinctBy { it.year to it.month }
            .sortedWith(compareByDescending<MesDisponible> { it.year }.thenByDescending { it.month })
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun procesarIncidencias(
        incidencias: List<Incidencia>,
        estados: List<EstadoIncidencia>,
        tematicas: List<Tematica>,
        search: String,
        filtro: String,
        mes: MesDisponible?,
        estadoDropdown: String?,
    ): HomeUiState {
        val noBorradores = incidencias.filter { !it.esBorrador }
        val conteosPorEstado = estados.map { estado ->
            EstadoCount(
                estado.id,
                estado.name,
                estado.colorHex,
                noBorradores.count { it.estadoId == estado.id }
            )
        }

        val totalBorradores = incidencias.count { it.esBorrador }
        val totalPendientes = noBorradores.count { !it.sincronizado }
        val estadisticas = buildEstadisticas(conteosPorEstado, totalBorradores, totalPendientes)
        val mesesDisponibles = buildMesesDisponibles(incidencias)

        val meses = listOf("E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        val datosMensuales = meses.mapIndexed { index, mesLabel ->
            val mesNum = index + 1
            val delMes = incidencias.filter { incidencia ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = incidencia.fechaHora
                cal.get(Calendar.MONTH) + 1 == mesNum
            }
            MonthlyData(
                mes = mesLabel,
                counts = estados.associate { estado ->
                    estado.id to delMes.count { it.estadoId == estado.id }
                }
            )
        }

        val tematicaMap = tematicas.associateBy { it.id }
        val estadoMap = estados.associateBy { it.id }

        var filteredList = when (filtro) {
            HomeFiltro.TODAS -> incidencias
            HomeFiltro.BORRADOR -> incidencias.filter { it.esBorrador }
            HomeFiltro.PENDIENTES_ENVIAR -> incidencias.filter { !it.esBorrador && !it.sincronizado }
            else -> incidencias.filter { !it.esBorrador && it.estadoId == filtro }
        }

        if (estadoDropdown != null && filtro == HomeFiltro.TODAS) {
            filteredList = filteredList.filter { !it.esBorrador && it.estadoId == estadoDropdown }
        }

        if (mes != null) {
            filteredList = filteredList.filter { incidencia ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = incidencia.fechaHora
                cal.get(Calendar.YEAR) == mes.year && cal.get(Calendar.MONTH) == mes.month
            }
        }

        if (search.isNotBlank()) {
            filteredList = filteredList.filter { incidencia ->
                val themeNameResolved = incidencia.themeName
                    ?: tematicaMap[incidencia.tematicaId]?.name
                    ?: incidencia.tematicaId
                incidencia.descripcion.contains(search, ignoreCase = true) ||
                    themeNameResolved.contains(search, ignoreCase = true)
            }
        }

        val listUi = filteredList
            .sortedByDescending { it.fechaHora }
            .map { it.toUi(tematicaMap, estadoMap) }

        return HomeUiState(
            incidencias = incidencias,
            conteosPorEstado = conteosPorEstado,
            estadisticas = estadisticas,
            datosMensuales = datosMensuales,
            totalBorradores = totalBorradores,
            totalPendientes = totalPendientes,
            isLoading = false,
            searchQuery = search,
            selectedFiltro = filtro,
            selectedMes = mes,
            selectedEstadoDropdown = estadoDropdown,
            mesesDisponibles = mesesDisponibles,
            incidenciasFiltradas = listUi
        )
    }

    private fun Incidencia.toUi(
        tematicaMap: Map<String, Tematica>,
        estadoMap: Map<String, EstadoIncidencia>,
    ): IncidenciaUi {
        val date = Date(fechaHora)
        val estadoLocal = estadoMap[estadoId]

        val statusNameResolved = if (esBorrador) {
            "Borrador"
        } else if (!sincronizado) {
            "Pendiente por sincronizar"
        } else {
            statusName ?: estadoLocal?.name ?: estadoId
        }
        val statusColorResolved = when {
            esBorrador -> "#A9A8A8"
            !sincronizado -> "#FF9800"
            else -> statusColor ?: estadoLocal?.colorHex ?: "#9E9E9E"
        }

        return IncidenciaUi(
            id = id,
            serverId = serverId,
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
            fechaFormateadaCompleta = fechaCompletaFormatter.format(date),
            fechaAbsoluta = fechaHora,
        )
    }
}
