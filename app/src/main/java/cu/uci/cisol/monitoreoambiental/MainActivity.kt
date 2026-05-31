package cu.uci.cisol.monitoreoambiental

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
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

        // Prevent system from adding translucent nav bar background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            MonitoreoAmbientalTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = koinViewModel()
                val isSessionActive by authViewModel.isSessionActive.collectAsState(initial = null)
                val snackbarHostState = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }

                splashScreen.setKeepOnScreenCondition { isSessionActive == null }

                if (isSessionActive != null) {
                    val startDestination = if (isSessionActive == true) "home" else "login"

                    androidx.compose.material3.Scaffold(
                        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { _ ->
                        NavHost(
                            navController = navController, 
                            startDestination = startDestination
                        ) {
                            composable("login") {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onNavigateToRegister = { navController.navigate("register") },
                                    onNavigateToHome = {
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    snackbarHostState = snackbarHostState
                                )
                            }
                            composable("register") {
                                RegisterScreen(
                                    viewModel = authViewModel,
                                    onNavigateToLogin = {
                                        navController.navigate("login") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                    },
                                    snackbarHostState = snackbarHostState
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
                                onInfoApp = {
                                    navController.navigate("info_app")
                                },
                                onLogout = {
                                    homeViewModel.logout()
                                },
                                onNavigateToDetail = { id, esBorrador ->
                                    if (esBorrador) {
                                        navController.navigate("nueva_incidencia?incidenciaId=$id")
                                    } else {
                                        navController.navigate("detalle_incidencia?incidenciaId=$id")
                                    }
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
                            route = "detalle_incidencia?incidenciaId={incidenciaId}",
                            arguments = listOf(
                                androidx.navigation.navArgument("incidenciaId") {
                                    nullable = false
                                },
                            ),
                        ) {
                            cu.uci.cisol.monitoreoambiental.ui.incidencia.DetalleIncidenciaRoot(
                                onNavigateBack = { navController.navigateUp() },
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
                                onNavigateToDetail = { id, esBorrador ->
                                    if (esBorrador) {
                                        navController.navigate("nueva_incidencia?incidenciaId=$id")
                                    } else {
                                        navController.navigate("detalle_incidencia?incidenciaId=$id")
                                    }
                                }
                            )
                        }
                        
                        composable("info_app") {
                            cu.uci.cisol.monitoreoambiental.ui.info.InfoAppScreen(
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                    }
                }
                }
            }
        }
    }
}
