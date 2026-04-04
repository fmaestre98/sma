package cu.uci.cisol.monitoreoambiental.data.repository

import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

class MockIncidenciaRepositoryImpl : IncidenciaRepository {

    private val now = System.currentTimeMillis()
    private val oneDay = 24 * 60 * 60 * 1000L

    private val mockData = mutableListOf(
        Incidencia(
            id = UUID.randomUUID().toString(),
            tematica = Tematica.DESECHOS_SOLIDOS,
            descripcion = "Se ha detectado un vertedero de basura ilegal cerca del parque central.",
            imagenPath = null,
            imagenResolucionPath = null,
            latitud = 23.113592,
            longitud = -82.366592,
            fechaHora = now,
            estado = EstadoIncidencia.BORRADOR,
            sincronizado = false
        ),
        Incidencia(
            id = UUID.randomUUID().toString(),
            tematica = Tematica.FOCOS_CONTAMINANTES,
            descripcion = "Humo denso proveniente de una fábrica a las afueras de la ciudad, olor fuerte.",
            imagenPath = null,
            imagenResolucionPath = null,
            latitud = 23.123592,
            longitud = -82.376592,
            fechaHora = now - (oneDay * 1),
            estado = EstadoIncidencia.PENDIENTE,
            sincronizado = false
        ),
        Incidencia(
            id = UUID.randomUUID().toString(),
            tematica = Tematica.ARBOLES,
            descripcion = "Tala indiscriminada de árboles en la zona forestal norte, sin permiso aparente.",
            imagenPath = null,
            imagenResolucionPath = null,
            latitud = 23.133592,
            longitud = -82.386592,
            fechaHora = now - (oneDay * 2),
            estado = EstadoIncidencia.NUEVA,
            sincronizado = true
        ),
        Incidencia(
            id = UUID.randomUUID().toString(),
            tematica = Tematica.SALIDEROS_AGUA,
            descripcion = "Salidero de agua potable continuo desde hace 3 días en la avenida principal.",
            imagenPath = null,
            imagenResolucionPath = null,
            latitud = 23.143592,
            longitud = -82.396592,
            fechaHora = now - (oneDay * 3),
            estado = EstadoIncidencia.ACEPTADA,
            sincronizado = true
        ),
        Incidencia(
            id = UUID.randomUUID().toString(),
            tematica = Tematica.ORDEN_PUBLICO,
            descripcion = "Ruidos molestos a altas horas de la noche por centro recreativo cercano.",
            imagenPath = null,
            imagenResolucionPath = null,
            latitud = 23.153592,
            longitud = -82.406592,
            fechaHora = now - (oneDay * 5),
            estado = EstadoIncidencia.RESUELTA,
            sincronizado = true
        )
    )

    private val _incidencias = MutableStateFlow(mockData.toList())

    override suspend fun guardar(incidencia: Incidencia): Result<Unit> {
        val index = mockData.indexOfFirst { it.id == incidencia.id }
        if (index != -1) {
            mockData[index] = incidencia
        } else {
            mockData.add(0, incidencia) // Add new ones at the beginning
        }
        _incidencias.update { mockData.toList() }
        return Result.success(Unit)
    }

    override fun observarTodas(): Flow<List<Incidencia>> {
        return _incidencias.map { it.sortedByDescending { inc -> inc.fechaHora } }
    }

    override suspend fun obtenerPendientes(): List<Incidencia> {
        return mockData.filter { it.estado == EstadoIncidencia.PENDIENTE }
    }

    override suspend fun marcarComoEnviada(id: String): Result<Unit> {
        val index = mockData.indexOfFirst { it.id == id }
        if (index != -1) {
            mockData[index] = mockData[index].copy(estado = EstadoIncidencia.NUEVA, sincronizado = true)
            _incidencias.update { mockData.toList() }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Incidencia no encontrada"))
    }

    override suspend fun enviarAlServidor(incidencia: Incidencia): Result<Unit> {
        return actualizarEstado(incidencia.id, EstadoIncidencia.NUEVA, null)
    }

    override suspend fun obtenerPorId(id: String): Incidencia? {
        return mockData.find { it.id == id }
    }

    override suspend fun actualizarEstado(
        id: String,
        estado: EstadoIncidencia,
        imagenResolucionPath: String?
    ): Result<Unit> {
        val index = mockData.indexOfFirst { it.id == id }
        if (index != -1) {
            val syncStatus = (estado != EstadoIncidencia.BORRADOR && estado != EstadoIncidencia.PENDIENTE)
            mockData[index] = mockData[index].copy(
                estado = estado,
                imagenResolucionPath = imagenResolucionPath ?: mockData[index].imagenResolucionPath,
                sincronizado = syncStatus
            )
            _incidencias.update { mockData.toList() }
            return Result.success(Unit)
        }
        return Result.failure(Exception("Incidencia no encontrada"))
    }
}
