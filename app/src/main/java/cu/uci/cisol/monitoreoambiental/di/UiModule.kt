package cu.uci.cisol.monitoreoambiental.di

import cu.uci.cisol.monitoreoambiental.ui.auth.AuthViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module { viewModel { AuthViewModel(get(), get(), get()) } }
