package cu.uci.cisol.monitoreoambiental.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cu.uci.cisol.monitoreoambiental.domain.repository.AppearanceRepository

class SincronizarAparienciaWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val appearanceRepository: AppearanceRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            appearanceRepository.syncAppearance()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
