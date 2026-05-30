package cu.uci.cisol.monitoreoambiental.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object IncidenciaSyncScheduler {

    private const val UPLOAD_WORK_NAME = "enviar_incidencias_pendientes"
    private const val PULL_WORK_NAME = "sincronizar_incidencias_periodico"
    private const val PULL_INTERVAL_HOURS = 3L

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    fun enqueueUploadPending(context: Context) {
        val request =
            OneTimeWorkRequestBuilder<EnviarIncidenciasPendientesWorker>()
                .setConstraints(networkConstraints())
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UPLOAD_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun schedulePeriodicPullSync(context: Context) {
        android.util.Log.d("SyncWorker", "Programando worker periódico con intervalo de $PULL_INTERVAL_HOURS minutos (WorkManager lo ajustará a 15 min como mínimo)")
        val request =
            PeriodicWorkRequestBuilder<SincronizarIncidenciasWorker>(
                PULL_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(networkConstraints())
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PULL_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
