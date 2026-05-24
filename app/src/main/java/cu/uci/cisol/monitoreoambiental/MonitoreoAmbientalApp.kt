package cu.uci.cisol.monitoreoambiental

import android.app.Application
import androidx.work.Configuration
import cu.uci.cisol.monitoreoambiental.data.work.IncidenciaSyncScheduler
import cu.uci.cisol.monitoreoambiental.di.dataModule
import cu.uci.cisol.monitoreoambiental.di.networkModule
import cu.uci.cisol.monitoreoambiental.di.uiModule
import cu.uci.cisol.monitoreoambiental.di.workModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MonitoreoAmbientalApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MonitoreoAmbientalApp)
            modules(
                dataModule,
                networkModule,
                uiModule,
                workModule,
            )
        }

        IncidenciaSyncScheduler.schedulePeriodicPullSync(this)
    }
}
