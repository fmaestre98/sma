package cu.uci.cisol.monitoreoambiental.di

import androidx.room.Room
import cu.uci.cisol.monitoreoambiental.data.local.db.MonitoreoDatabase
import cu.uci.cisol.monitoreoambiental.data.repository.IncidenciaRepositoryImpl
import cu.uci.cisol.monitoreoambiental.data.repository.UsuarioRepositoryImpl
import cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository
import cu.uci.cisol.monitoreoambiental.domain.repository.UsuarioRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(
                        androidContext(),
                        MonitoreoDatabase::class.java,
                        "monitoreo_ambiental_db"
                )
                .fallbackToDestructiveMigration()
                .build()
    }

    single { get<MonitoreoDatabase>().usuarioDao }
    single { get<MonitoreoDatabase>().incidenciaDao }

    single<cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository> {
        cu.uci.cisol.monitoreoambiental.data.local.DataStoreSessionManager(androidContext())
    }

    single<cu.uci.cisol.monitoreoambiental.domain.repository.UsuarioRepository> {
        UsuarioRepositoryImpl(get())
    }
    single<cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository> {
        if (cu.uci.cisol.monitoreoambiental.core.config.AppConfig.USE_MOCK_INCIDENCIAS) {
            cu.uci.cisol.monitoreoambiental.data.repository.MockIncidenciaRepositoryImpl()
        } else {
            IncidenciaRepositoryImpl(get())
        }
    }
    single<cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository> {
        if (cu.uci.cisol.monitoreoambiental.core.config.AppConfig.USE_MOCK_AUTH) {
            cu.uci.cisol.monitoreoambiental.data.repository.MockAuthRepositoryImpl()
        } else {
            cu.uci.cisol.monitoreoambiental.data.repository.AuthRepositoryImpl(get())
        }
    }
}
