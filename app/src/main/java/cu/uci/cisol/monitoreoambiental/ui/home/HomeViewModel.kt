package cu.uci.cisol.monitoreoambiental.ui.home

import android.content.Context
import androidx.annotation.DisplayContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.data.work.IncidenciaSyncScheduler
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.TematicaRepository
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import cu.uci.cisol.monitoreoambiental.domain.repository.EstadoRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.LocalidadRepository

// -----------------------
// Configuración de datos de prueba
// Cambia a false para cargar datos reales desde la base de datos local
// -----------------------
private const val USE_MOCK_DATA = false

data class EstadoCount(val id: String, val nombre: String, val colorHex: String, val count: Int)

data class MonthlyData(
    val mes: String,
    val counts: Map<String, Int> // Map of estadoId to count
)

data class HomeUiState(
    val incidencias: List<Incidencia> = emptyList(),
    val conteosPorEstado: List<EstadoCount> = emptyList(),
    val datosMensuales: List<MonthlyData> = emptyList(),
    val totalBorradores: Int = 0,
    val totalPendientes: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val logoutCompleted: Boolean = false,
    val tematicaSyncError: String? = null
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

    private fun syncTematicas() {
        viewModelScope.launch {
            when (val result = tematicaRepository.syncTematicas()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(tematicaSyncError = null)
                }
                is Result.Error -> {
                    // Check if there's cached data
                    tematicaRepository.observeTematicas().collect { tematicas ->
                        if (tematicas.isEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                tematicaSyncError = "Error al sincronizar temáticas. Verifique su conexión."
                            )
                        }
                        // Only collect the first emission to check cache status
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

        _uiState.value = HomeUiState(
            conteosPorEstado = conteosSimulados,
            datosMensuales = datosMensuales,
            isLoading = false,
            totalPendientes = 5
        )
    }

    private fun cargarDatosReales() {
        viewModelScope.launch {
            combine(
                repository.observarTodas(),
                estadoRepository.observeEstados()
            ) { incidencias, estados ->
                procesarIncidencias(incidencias, estados)
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

    @OptIn(ExperimentalStdlibApi::class)
    private fun procesarIncidencias(incidencias: List<Incidencia>, estados: List<EstadoIncidencia>): HomeUiState {
        val noBorradores = incidencias.filter { !it.esBorrador }
        val conteosPorEstado = estados.map { estado ->
            EstadoCount(estado.id, estado.name, estado.colorHex, noBorradores.count { it.estadoId == estado.id })
        }

        val totalBorradores = incidencias.count { it.esBorrador }
        val totalPendientes = noBorradores.count { !it.sincronizado }

        val meses = listOf("E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        val datosMensuales = meses.mapIndexed { index, mes ->
            val mesNum = index + 1
            val delMes = incidencias.filter { incidencia ->
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = incidencia.fechaHora
                cal.get(java.util.Calendar.MONTH) + 1 == mesNum
            }
            MonthlyData(
                mes = mes,
                counts = estados.associate { estado ->
                    estado.id to delMes.count { it.estadoId == estado.id }
                }
            )
        }

        return HomeUiState(
            incidencias = incidencias,
            conteosPorEstado = conteosPorEstado,
            datosMensuales = datosMensuales,
            totalBorradores = totalBorradores,
            totalPendientes = totalPendientes,
            isLoading = false
        )
    }
}
