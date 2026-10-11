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
        val startedAt = android.os.SystemClock.elapsedRealtime()
        android.util.Log.i(
            TAG,
            "Pull iniciado: id=$id, intento=$runAttemptCount, pageSize=$PAGE_SIZE",
        )
        if (sessionRepository.getAccessToken() == null) {
            android.util.Log.w(TAG, "Pull omitido: no hay access token; no se programará reintento")
            return Result.success()
        }

        return try {
            val syncResult = incidenciaRepository.syncCompletoDesdeServidor(pageSize = PAGE_SIZE)
            val elapsedMs = android.os.SystemClock.elapsedRealtime() - startedAt
            if (syncResult.isSuccess) {
                android.util.Log.i(
                    TAG,
                    "Pull completado: registros=${syncResult.getOrThrow()}, elapsedMs=$elapsedMs",
                )
                Result.success()
            } else {
                val error = syncResult.exceptionOrNull()
                android.util.Log.e(
                    TAG,
                    "Pull fallido; WorkManager reintentará: intento=$runAttemptCount, elapsedMs=$elapsedMs, error=${error?.message}",
                    error,
                )
                Result.retry()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            val elapsedMs = android.os.SystemClock.elapsedRealtime() - startedAt
            android.util.Log.e(
                TAG,
                "Excepción no controlada en pull; WorkManager reintentará: intento=$runAttemptCount, elapsedMs=$elapsedMs",
                e,
            )
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "IncidenciaSync"
        const val PAGE_SIZE = 50
    }
}
