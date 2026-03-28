package cu.uci.cisol.monitoreoambiental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cu.uci.cisol.monitoreoambiental.ui.auth.AuthViewModel
import cu.uci.cisol.monitoreoambiental.ui.auth.LoginScreen
import cu.uci.cisol.monitoreoambiental.ui.auth.RegisterScreen
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
                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                Greeting(
                                        name = "Android User",
                                        modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name! Here goes the Home screen.", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MonitoreoAmbientalTheme { Greeting("Android") }
}
