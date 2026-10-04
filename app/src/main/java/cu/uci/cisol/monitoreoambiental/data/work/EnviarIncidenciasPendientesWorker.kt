package cu.uci.cisol.monitoreoambiental.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EnviarIncidenciasPendientesWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val incidenciaRepository: IncidenciaRepository by inject()
    private val sessionRepository: SessionRepository by inject()

    override suspend fun doWork(): Result {
        android.util.Log.i(
            TAG,
            "Worker iniciado: id=$id, intento=$runAttemptCount",
        )
        if (sessionRepository.getAccessToken() == null) {
            android.util.Log.w(TAG, "No hay access token; termina sin enviar pendientes")
            return Result.success()
        }

        return try {
            val uploadResult = incidenciaRepository.enviarPendientesAlServidor()
            if (uploadResult.isFailure) {
                android.util.Log.e(
                    TAG,
                    "Falló la consulta de pendientes; WorkManager reintentará",
                    uploadResult.exceptionOrNull(),
                )
                return Result.retry()
            }

            val stats = uploadResult.getOrThrow()
            android.util.Log.i(
                TAG,
                "Worker terminó: enviadas=${stats.enviadas}, fallidas=${stats.fallidas}",
            )
            if (stats.fallidas > 0) Result.retry() else Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Excepción no controlada al enviar pendientes; WorkManager reintentará", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "IncidenciaSync"
    }
}
