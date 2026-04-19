package cu.uci.cisol.monitoreoambiental.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.uci.cisol.monitoreoambiental.R

@Composable
fun LoginScreen(
        viewModel: AuthViewModel,
        onNavigateToRegister: () -> Unit,
        onNavigateToHome: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.SuccessLogin) {
            onNavigateToHome()
        }
    }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

    Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(160.dp)
        )


        Spacer(modifier = Modifier.height(24.dp))

        Text(
                text = "Monitoreo Ambiental",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
                text =
                        "Herramienta para realizar el monitoreo en tiempo real de indicadores medioambientales claves",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text("Nombre de Usuario") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        /*Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
            Text("Recuérdame", color = Color.Gray, fontSize = 14.sp)
        }*/

        Spacer(modifier = Modifier.height(24.dp))

        Button(
                onClick = { viewModel.login(username, password) },
                modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005b82))
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Entrar", fontSize = 18.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text("¿No posee cuenta? ", color = Color.Gray)
            Text(
                    text = "Registrar",
                    color = Color(0xFF005b82),
                    modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
                text =
                        "El usuario al pulsar en \"Entrar\" se encuentra\naceptando además los Terminos Condiciones y\nPoliticas de la Institucion.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF666666)
        )
    }
}
