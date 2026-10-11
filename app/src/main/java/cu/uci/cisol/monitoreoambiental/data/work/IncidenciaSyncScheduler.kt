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
    private const val WORK_MIGRATION_PREFS = "work_manager_migrations"
    private const val APPENDED_UPLOAD_CHAIN_RECOVERED = "appended_upload_chain_recovered_v1"
    private const val PULL_WORK_NAME = "sincronizar_incidencias_periodico"
    private const val PULL_APPEARANCE_WORK_NAME = "sincronizar_apariencia_periodico"
    private const val PULL_INTERVAL_MINUTES = 15L
    private const val APPEARANCE_INTERVAL_HOURS = 3L

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    fun enqueueUploadPending(context: Context) {
        val request = buildUploadRequest()

        android.util.Log.i(
            "IncidenciaSync",
            "Solicitando envío de pendientes: workId=${request.id}, policy=KEEP",
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            UPLOAD_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Replaces the blocked chain created by older APPEND_OR_REPLACE releases, once. */
    fun recoverAppendedUploadChain(context: Context) {
        val prefs = context.getSharedPreferences(WORK_MIGRATION_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(APPENDED_UPLOAD_CHAIN_RECOVERED, false)) return

        val request = buildUploadRequest()
        android.util.Log.w(
            "IncidenciaSync",
            "Recuperando cadena antigua de envíos: workId=${request.id}, policy=REPLACE",
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            UPLOAD_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        prefs.edit().putBoolean(APPENDED_UPLOAD_CHAIN_RECOVERED, true).apply()
    }

    private fun buildUploadRequest() =
        OneTimeWorkRequestBuilder<EnviarIncidenciasPendientesWorker>()
            .setConstraints(networkConstraints())
            .build()

    fun schedulePeriodicPullSync(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<SincronizarIncidenciasWorker>(
                PULL_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            )
                .setConstraints(networkConstraints())
                .build()

        android.util.Log.i(
            "IncidenciaSync",
            "Programando pull periódico: intervalo solicitado=$PULL_INTERVAL_MINUTES minutos (WorkManager aplica mínimo de 15 minutos), workId=${request.id}, constraint=CONNECTED",
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PULL_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )

        val appearanceRequest = PeriodicWorkRequestBuilder<SincronizarAparienciaWorker>(
            APPEARANCE_INTERVAL_HOURS,
            TimeUnit.HOURS,
        ).setConstraints(networkConstraints()).build()

        android.util.Log.i(
            "IncidenciaSync",
            "Programando apariencia periódica: intervalo=$APPEARANCE_INTERVAL_HOURS horas, workId=${appearanceRequest.id}, constraint=CONNECTED",
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PULL_APPEARANCE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            appearanceRequest,
        )
    }
}
