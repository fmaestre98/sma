package cu.uci.cisol.monitoreoambiental.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SincronizarIncidenciasWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val incidenciaRepository: IncidenciaRepository by inject()
    private val sessionRepository: SessionRepository by inject()

    override suspend fun doWork(): Result {
        android.util.Log.d("SyncWorker", "Iniciando worker de sincronización...")
        if (sessionRepository.getAccessToken() == null) {
            android.util.Log.d("SyncWorker", "No hay token, cancelando sincronización")
            return Result.success()
        }

        val syncResult = incidenciaRepository.syncCompletoDesdeServidor(pageSize = PAGE_SIZE)
        return if (syncResult.isSuccess) {
            android.util.Log.d("SyncWorker", "Sincronización completada con éxito")
            Result.success()
        } else {
            android.util.Log.e("SyncWorker", "Error en sincronización, reintentando...")
            Result.retry()
        }
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
