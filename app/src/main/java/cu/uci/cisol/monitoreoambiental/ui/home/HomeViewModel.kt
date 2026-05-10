package cu.uci.cisol.monitoreoambiental.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// -----------------------
// Configuración de datos de prueba
// Cambia a false para cargar datos reales desde la base de datos local
// -----------------------
private const val USE_MOCK_DATA = true

data class EstadoCount(val estado: EstadoIncidencia, val count: Int)

data class MonthlyData(
    val mes: String,
    val counts: Map<EstadoIncidencia, Int>
)

data class HomeUiState(
    val incidencias: List<Incidencia> = emptyList(),
    val conteosPorEstado: List<EstadoCount> = emptyList(),
    val datosMensuales: List<MonthlyData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val logoutCompleted: Boolean = false
)

class HomeViewModel(
    private val repository: IncidenciaRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(logoutCompleted = true)
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
            EstadoCount(EstadoIncidencia.PENDIENTE, 8),
            EstadoCount(EstadoIncidencia.NUEVA, 15),
            EstadoCount(EstadoIncidencia.ACEPTADA, 42),
            EstadoCount(EstadoIncidencia.RESUELTA, 38),
            EstadoCount(EstadoIncidencia.RECHAZADA, 20)
        )

        val meses = listOf("E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        val datosMensuales = meses.mapIndexed { index, mes ->
            MonthlyData(
                mes = mes,
                counts = mapOf(
                    EstadoIncidencia.NUEVA to (5 + (index * 3) % 20),
                    EstadoIncidencia.ACEPTADA to (10 + (index * 5) % 35),
                    EstadoIncidencia.RECHAZADA to (3 + (index * 2) % 15)
                )
            )
        }

        _uiState.value = HomeUiState(
            conteosPorEstado = conteosSimulados,
            datosMensuales = datosMensuales,
            isLoading = false
        )
    }

    private fun cargarDatosReales() {
        viewModelScope.launch {
            repository.observarTodas()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .map { incidencias: List<Incidencia> -> procesarIncidencias(incidencias) }
                .collect { nuevoEstado: HomeUiState ->
                    _uiState.value = nuevoEstado
                }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun procesarIncidencias(incidencias: List<Incidencia>): HomeUiState {
        val conteosPorEstado = EstadoIncidencia.entries.map { estado ->
            EstadoCount(estado, incidencias.count { it.estado == estado })
        }.filter { it.count > 0 }

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
                counts = EstadoIncidencia.entries.associateWith { estado ->
                    delMes.count { it.estado == estado }
                }
            )
        }

        return HomeUiState(
            incidencias = incidencias,
            conteosPorEstado = conteosPorEstado,
            datosMensuales = datosMensuales,
            isLoading = false
        )
    }
}
