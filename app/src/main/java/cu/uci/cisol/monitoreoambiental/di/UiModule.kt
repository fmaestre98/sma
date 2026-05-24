package cu.uci.cisol.monitoreoambiental.di

import cu.uci.cisol.monitoreoambiental.ui.auth.AuthViewModel
import cu.uci.cisol.monitoreoambiental.ui.home.HomeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { AuthViewModel(get(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { cu.uci.cisol.monitoreoambiental.ui.incidencia.NuevaIncidenciaViewModel(get(), get(), get(), get(), get()) }
    viewModel { cu.uci.cisol.monitoreoambiental.ui.incidencia.MisIncidenciasViewModel(get(), get(), get(), get()) }
    viewModel { cu.uci.cisol.monitoreoambiental.ui.incidencia.DetalleIncidenciaViewModel(get(), get(), get(), get()) }
}

