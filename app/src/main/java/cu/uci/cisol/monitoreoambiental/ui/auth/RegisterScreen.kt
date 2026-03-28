package cu.uci.cisol.monitoreoambiental.ui.auth

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegisterScreen(viewModel: AuthViewModel, onNavigateToLogin: () -> Unit) {
    val authState by viewModel.authState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.SuccessRegister) {
            showDialog = true
            viewModel.resetState()
        }
    }

    if (showDialog) {
        AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    onNavigateToLogin()
                },
                title = { Text("Registro Exitoso") },
                text = {
                    Text(
                            "Los administradores revisaran la solicitud de registro y le notificaran cuando este aprobado para hacer login en la app."
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showDialog = false
                                onNavigateToLogin()
                            }
                    ) { Text("Aceptar") }
                }
        )
    }

    var username by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Box(
                modifier = Modifier.size(100.dp).background(Color.Gray, shape = CircleShape),
                contentAlignment = Alignment.Center
        ) { Text("LOGO", color = Color.White) }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
                text = "Registro",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                placeholder = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                placeholder = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

        Button(
                onClick = { viewModel.register(username, password, nombre, correo) },
                modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005b82))
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Registrar", fontSize = 18.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text("¿Ya posee cuenta? ", color = Color.Gray)
            Text(
                    text = "Entrar",
                    color = Color(0xFF005b82),
                    modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}
