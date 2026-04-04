package cu.uci.cisol.monitoreoambiental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cu.uci.cisol.monitoreoambiental.ui.auth.AuthViewModel
import cu.uci.cisol.monitoreoambiental.ui.auth.LoginScreen
import cu.uci.cisol.monitoreoambiental.ui.auth.RegisterScreen
import cu.uci.cisol.monitoreoambiental.ui.home.HomeScreen
import cu.uci.cisol.monitoreoambiental.ui.home.HomeViewModel
import cu.uci.cisol.monitoreoambiental.ui.theme.MonitoreoAmbientalTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonitoreoAmbientalTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = koinViewModel()
                val isSessionActive by authViewModel.isSessionActive.collectAsState(initial = null)

                splashScreen.setKeepOnScreenCondition { isSessionActive == null }

                if (isSessionActive != null) {
                    val startDestination = if (isSessionActive == true) "home" else "login"

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToRegister = { navController.navigate("register") },
                                onNavigateToHome = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            val homeViewModel: HomeViewModel = koinViewModel()
                            val homeUiState by homeViewModel.uiState.collectAsState()

                            // Observa cuando el logout se completó para navegar al login
                            LaunchedEffect(homeUiState.logoutCompleted) {
                                if (homeUiState.logoutCompleted) {
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            }

                            HomeScreen(
                                viewModel = homeViewModel,
                                onNuevaIncidencia = {
                                    navController.navigate("nueva_incidencia")
                                },
                                onMisIncidencias = {
                                    navController.navigate("mis_incidencias")
                                },
                                onVerBorradores = {
                                    navController.navigate("mis_incidencias?filtro=${cu.uci.cisol.monitoreoambiental.ui.incidencia.IncidenciaFiltro.BORRADORES.name}")
                                },
                                onLogout = {
                                    homeViewModel.logout()
                                }
                            )
                        }
                        
                        composable(
                            route = "nueva_incidencia?incidenciaId={incidenciaId}",
                            arguments = listOf(androidx.navigation.navArgument("incidenciaId") { 
                                nullable = true
                                defaultValue = null
                            })
                        ) {
                            cu.uci.cisol.monitoreoambiental.ui.incidencia.NuevaIncidenciaRoot(
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }

                        composable(
                            route = "mis_incidencias?filtro={filtro}",
                            arguments = listOf(androidx.navigation.navArgument("filtro") { 
                                nullable = true
                                defaultValue = null
                            })
                        ) {
                            cu.uci.cisol.monitoreoambiental.ui.incidencia.MisIncidenciasRoot(
                                onNavigateBack = { navController.navigateUp() },
                                onNavigateToDetail = { id ->
                                    navController.navigate("nueva_incidencia?incidenciaId=$id")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
