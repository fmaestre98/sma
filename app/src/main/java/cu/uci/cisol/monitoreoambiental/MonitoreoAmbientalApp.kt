package cu.uci.cisol.monitoreoambiental

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import cu.uci.cisol.monitoreoambiental.di.dataModule
import cu.uci.cisol.monitoreoambiental.di.domainModule
import cu.uci.cisol.monitoreoambiental.di.networkModule

class MonitoreoAmbientalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MonitoreoAmbientalApp)
            modules(
                dataModule,
                domainModule,
                networkModule
            )
        }
    }
}
