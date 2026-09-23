package cu.uci.cisol.monitoreoambiental.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.uci.cisol.monitoreoambiental.R

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    snackbarHostState: SnackbarHostState
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.LoginSuccess -> onNavigateToHome()
                is AuthEvent.RegisterSuccess -> {
                    snackbarHostState.showSnackbar(event.message.asString(context))
                }
                is AuthEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message.asString(context))
                }
                else -> Unit
            }
        }
    }

    if (state.showOtpVerification) {
        OtpVerificationDialog(
            email = state.otpEmail.ifBlank { state.email },
            otp = state.otpCode,
            isLoading = state.isLoading,
            error = state.error?.asString(context),
            onOtpChange = { viewModel.onAction(AuthAction.OnOtpChange(it)) },
            onConfirm = { viewModel.onAction(AuthAction.SubmitOtpVerification) },
            onResend = { viewModel.onAction(AuthAction.ResendOtp) },
            onDismiss = { viewModel.onAction(AuthAction.DismissOtpVerification) },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Registro",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.firstName,
                onValueChange = { viewModel.onAction(AuthAction.OnFirstNameChange(it)) },
                placeholder = { Text("Nombre") },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                isError = state.error != null
            )

            OutlinedTextField(
                value = state.lastName,
                onValueChange = { viewModel.onAction(AuthAction.OnLastNameChange(it)) },
                placeholder = { Text("Apellidos") },
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                isError = state.error != null
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onAction(AuthAction.OnEmailChange(it)) },
            placeholder = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            isError = state.error != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.username,
            onValueChange = { viewModel.onAction(AuthAction.OnUsernameChange(it)) },
            placeholder = { Text("Nombre de Usuario") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            isError = state.error != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = { viewModel.onAction(AuthAction.OnPasswordChange(it)) },
            placeholder = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            isError = state.error != null
        )

        if (state.error != null && !state.showOtpVerification) {
            Text(
                text = state.error?.asString(context) ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.onAction(AuthAction.SubmitRegister)
            },
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005b82)),
            enabled = !state.isLoading
        ) {
            if (state.isLoading && !state.showOtpVerification) {
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
