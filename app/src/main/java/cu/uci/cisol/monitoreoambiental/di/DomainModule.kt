package cu.uci.cisol.monitoreoambiental.di

import cu.uci.cisol.monitoreoambiental.domain.usecase.EliminarUsuarioUseCase
import cu.uci.cisol.monitoreoambiental.domain.usecase.GuardarIncidenciaUseCase
import cu.uci.cisol.monitoreoambiental.domain.usecase.GuardarUsuarioUseCase
import cu.uci.cisol.monitoreoambiental.domain.usecase.ObtenerIncidenciasPendientesUseCase
import cu.uci.cisol.monitoreoambiental.domain.usecase.ObtenerIncidenciasUseCase
import cu.uci.cisol.monitoreoambiental.domain.usecase.ObtenerUsuarioUseCase
import cu.uci.cisol.monitoreoambiental.domain.usecase.SincronizarIncidenciasUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GuardarIncidenciaUseCase(get()) }
    factory { ObtenerIncidenciasPendientesUseCase(get()) }
    factory { ObtenerIncidenciasUseCase(get()) }
    factory { SincronizarIncidenciasUseCase(get()) }

    factory { GuardarUsuarioUseCase(get()) }
    factory { ObtenerUsuarioUseCase(get()) }
    factory { EliminarUsuarioUseCase(get()) }

    factory { cu.uci.cisol.monitoreoambiental.domain.usecase.LoginUseCase(get(), get()) }
    factory { cu.uci.cisol.monitoreoambiental.domain.usecase.RegisterUseCase(get()) }
    factory { cu.uci.cisol.monitoreoambiental.domain.usecase.CheckSessionUseCase(get()) }
    factory { cu.uci.cisol.monitoreoambiental.domain.usecase.LogoutUseCase(get()) }
}
