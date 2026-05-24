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
        if (sessionRepository.getAccessToken() == null) {
            return Result.success()
        }

        val uploadResult = incidenciaRepository.enviarPendientesAlServidor()
        if (uploadResult.isFailure) {
            return Result.retry()
        }

        val stats = uploadResult.getOrThrow()
        return if (stats.fallidas > 0) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
