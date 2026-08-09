package cu.uci.cisol.monitoreoambiental.di

import UsuarioRepositoryImpl
import androidx.room.Room
import cu.uci.cisol.monitoreoambiental.data.local.db.MonitoreoDatabase
import cu.uci.cisol.monitoreoambiental.data.repository.IncidenciaRepositoryImpl
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
    single { get<MonitoreoDatabase>().tematicaDao }
    single { get<MonitoreoDatabase>().estadoDao }
    single { get<MonitoreoDatabase>().localidadDao }
    single { get<MonitoreoDatabase>().organizationDao }
    single { get<MonitoreoDatabase>().systemAppearanceDao }

    single<cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository> {
        cu.uci.cisol.monitoreoambiental.data.local.DataStoreSessionManager(androidContext())
    }

    single<cu.uci.cisol.monitoreoambiental.domain.repository.UsuarioRepository> {
        UsuarioRepositoryImpl(get(), get(), androidContext())
    }
    single<cu.uci.cisol.monitoreoambiental.domain.repository.TematicaRepository> {
        cu.uci.cisol.monitoreoambiental.data.repository.OfflineFirstTematicaRepository(get(), get())
    }
    single<cu.uci.cisol.monitoreoambiental.domain.repository.EstadoRepository> {
        cu.uci.cisol.monitoreoambiental.data.repository.OfflineFirstEstadoRepository(get(), get())
    }
    single<cu.uci.cisol.monitoreoambiental.domain.repository.LocalidadRepository> {
        cu.uci.cisol.monitoreoambiental.data.repository.OfflineFirstLocalidadRepository(get(), get())
    }
    single<cu.uci.cisol.monitoreoambiental.domain.repository.OrganizationRepository> {
        cu.uci.cisol.monitoreoambiental.data.repository.OfflineFirstOrganizationRepository(get(), get())
    }
    single<cu.uci.cisol.monitoreoambiental.domain.repository.AppearanceRepository> {
        cu.uci.cisol.monitoreoambiental.data.repository.AppearanceRepositoryImpl(get(), get())
    }
    single<cu.uci.cisol.monitoreoambiental.domain.repository.IncidenciaRepository> {
        if (cu.uci.cisol.monitoreoambiental.core.config.AppConfig.USE_MOCK_INCIDENCIAS) {
            cu.uci.cisol.monitoreoambiental.data.repository.MockIncidenciaRepositoryImpl()
        } else {
            IncidenciaRepositoryImpl(
                incidenciaDao = get(),
                managementApi = get(),
                tematicaDao = get(),
                estadoDao = get(),
                localidadDao = get(),
                context = androidContext(),
            )
        }
    }
    single<cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository> {
        if (cu.uci.cisol.monitoreoambiental.core.config.AppConfig.USE_MOCK_AUTH) {
            cu.uci.cisol.monitoreoambiental.data.repository.MockAuthRepositoryImpl()
        } else {
            cu.uci.cisol.monitoreoambiental.data.repository.AuthRepositoryImpl(get(), get())
        }
    }
}
