package cu.uci.cisol.monitoreoambiental.di

import cu.uci.cisol.monitoreoambiental.data.work.EnviarIncidenciasPendientesWorker
import cu.uci.cisol.monitoreoambiental.data.work.SincronizarAparienciaWorker
import cu.uci.cisol.monitoreoambiental.data.work.SincronizarIncidenciasWorker
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val workModule = module {
    worker { EnviarIncidenciasPendientesWorker(get(), get()) }
    worker { SincronizarIncidenciasWorker(get(), get()) }
    worker { SincronizarAparienciaWorker(get(), get(), get()) }
}
