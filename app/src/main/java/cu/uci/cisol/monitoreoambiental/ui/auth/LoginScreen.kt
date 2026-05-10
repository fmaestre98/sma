package cu.uci.cisol.monitoreoambiental.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
                    snackbarHostState.showSnackbar(event.message.asString(context))
                }
                is AuthEvent.RegisterSuccess -> {
                    snackbarHostState.showSnackbar(event.message.asString(context))
                }
            }
        }
    }

    // Determine what to show
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

    Column(
        modifier = Modifier
            .fillMaxSize()
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

        Spacer(modifier = Modifier.height(24.dp))

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

        Row {
            Text("¿No posee cuenta? ", color = Color.Gray)
            Text(
                text = "Registrar",
                color = Color(0xFF005b82),
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
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
