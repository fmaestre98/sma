package cu.uci.cisol.monitoreoambiental.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.uci.cisol.monitoreoambiental.R
import cu.uci.cisol.monitoreoambiental.data.remote.ApiConfig

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.LoginSuccess -> onNavigateToHome()
                is AuthEvent.ConflictResolved -> {
                    snackbarHostState.showSnackbar(event.message.asString(context))
                }
                is AuthEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message.asString(context), duration = SnackbarDuration.Long)
                }
                is AuthEvent.RegisterSuccess -> {
                    snackbarHostState.showSnackbar(event.message.asString(context))
                }
            }
        }
    }

    // Conflict resolution dialog
    if (state.isResolvingConflict) {
        ConflictResolutionDialog(
            sessions = state.conflictSessions,
            selectedIds = state.selectedSessionIds,
            onSessionSelected = { id, selected -> viewModel.onAction(AuthAction.OnSessionSelected(id, selected)) },
            onConfirm = { viewModel.onAction(AuthAction.SubmitConflictResolution) },
            onDismiss = { viewModel.onAction(AuthAction.CancelConflictResolution) },
            isLoading = state.isLoading
        )
    }

    // Password reset dialog
    if (state.showPasswordResetDialog) {
        PasswordResetDialog(
            email = state.passwordResetEmail,
            emailError = state.passwordResetEmailError?.asString(context),
            isLoading = state.isPasswordResetLoading,
            onEmailChange = { viewModel.onAction(AuthAction.OnPasswordResetEmailChange(it)) },
            onConfirm = { viewModel.onAction(AuthAction.SubmitPasswordReset) },
            onDismiss = { viewModel.onAction(AuthAction.DismissPasswordResetDialog) }
        )
    }

    // URL config dialog
    if (state.showUrlConfigDialog) {
        UrlConfigDialog(
            url = state.tempUrl,
            onUrlChange = { viewModel.onAction(AuthAction.OnUrlChange(it)) },
            onConfirm = { viewModel.onAction(AuthAction.SaveUrl(context)) },
            onDismiss = { viewModel.onAction(AuthAction.DismissUrlConfigDialog) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            modifier = Modifier
                .size(160.dp)
                .background(
                    color = colorResource(id = R.color.ic_launcher_background),
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Monitoreo Ambiental",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(16.dp))


        Spacer(modifier = Modifier.height(32.dp))

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

        if (state.error != null) {
            Text(
                text = state.error?.asString(context) ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot password link
        TextButton(
            onClick = { viewModel.onAction(AuthAction.ShowPasswordResetDialog) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = "¿Olvidó su contraseña?",
                color = Color(0xFF123C78),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.onAction(AuthAction.SubmitLogin) },
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123C78)),
            enabled = !state.isLoading
        ) {
            if (state.isLoading && !state.isResolvingConflict) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Entrar", fontSize = 18.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        /*Row {
            Text("¿No posee cuenta? ", color = Color.Gray)
            Text(
                text = "Registrar",
                color = Color(0xFF005b82),
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }*/

        Spacer(modifier = Modifier.weight(1f))
    }

        if (ApiConfig.ENABLE_DYNAMIC_URL) {
            IconButton(
                onClick = { viewModel.onAction(AuthAction.ShowUrlConfigDialog) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .safeDrawingPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configurar URL API",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun PasswordResetDialog(
    email: String,
    emailError: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = Color(0xFF123C78)
            )
        },
        title = {
            Text(
                text = "Recuperar contraseña",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Ingrese su correo electrónico y le enviaremos un enlace para restablecer su contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("ejemplo@correo.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    isError = emailError != null,
                    enabled = !isLoading,
                    supportingText = if (emailError != null) {
                        { Text(emailError, color = MaterialTheme.colorScheme.error) }
                    } else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123C78)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Enviar enlace")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar", color = Color(0xFF123C78))
            }
        }
    )
}

@Composable
fun ConflictResolutionDialog(
    sessions: List<cu.uci.cisol.monitoreoambiental.domain.model.Session>,
    selectedIds: Set<String>,
    onSessionSelected: (String, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sesiones Activas") },
        text = {
            Column {
                Text("Tiene múltiples sesiones activas. Seleccione las que desea cerrar:")
                Spacer(modifier = Modifier.height(8.dp))
                sessions.forEach { session ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedIds.contains(session.sessionId),
                            onCheckedChange = { onSessionSelected(session.sessionId, it) }
                        )
                        Column {
                            Text("IP: ${session.ipAddress}", style = MaterialTheme.typography.bodyMedium)
                            Text("Activa: ${session.activeTime}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading && selectedIds.isNotEmpty()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Cerrar Seleccionadas")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun UrlConfigDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar URL API") },
        text = {
            Column {
                Text("Ingrese la URL base del backend para pruebas locales:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("http://192.168.1.x:8000") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
