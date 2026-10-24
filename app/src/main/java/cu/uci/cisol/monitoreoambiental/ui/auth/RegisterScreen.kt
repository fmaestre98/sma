package cu.uci.cisol.monitoreoambiental.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.uci.cisol.monitoreoambiental.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    var confirmationVisible by remember { mutableStateOf(false) }
    var countryMenuExpanded by remember { mutableStateOf(false) }
    val countries = remember {
        Locale.getISOCountries().map { code ->
            code to Locale("", code).getDisplayCountry(Locale("es"))
        }.sortedBy { it.second }
    }
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
            modifier = Modifier
                .size(160.dp)
                .background(
                    color = colorResource(id = R.color.ic_launcher_background),
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Registro",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = state.username,
            onValueChange = { viewModel.onAction(AuthAction.OnUsernameChange(it)) },
            label = { Text("Nombre de usuario *") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp), singleLine = true,
            isError = "username" in state.registrationFieldErrors,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.firstName,
            onValueChange = { viewModel.onAction(AuthAction.OnFirstNameChange(it)) },
            label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp), singleLine = true,
            isError = "firstName" in state.registrationFieldErrors,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.lastName,
            onValueChange = { viewModel.onAction(AuthAction.OnLastNameChange(it)) },
            label = { Text("Apellidos") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp), singleLine = true,
            isError = "lastName" in state.registrationFieldErrors,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onAction(AuthAction.OnEmailChange(it)) },
            label = { Text("Correo electrónico *") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = "email" in state.registrationFieldErrors,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box {
            OutlinedTextField(
                value = countries.firstOrNull { it.first == state.countryCode }?.second.orEmpty(),
                onValueChange = {}, readOnly = true,
                label = { Text("País *") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp), singleLine = true,
                isError = "country" in state.registrationFieldErrors,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryMenuExpanded) },
            )
            Box(Modifier.matchParentSize().clickable { countryMenuExpanded = true })
            DropdownMenu(
                expanded = countryMenuExpanded,
                onDismissRequest = { countryMenuExpanded = false },
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                countries.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            viewModel.onAction(AuthAction.OnCountryChange(code))
                            countryMenuExpanded = false
                        },
                    )
                }
            }
        }
        if (state.countryCode == "CU") {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.ci,
                onValueChange = { viewModel.onAction(AuthAction.OnCiChange(it)) },
                label = { Text("CI (11 dígitos) *") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = "ci" in state.registrationFieldErrors,
            )
        } else if (state.countryCode.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.passport,
                onValueChange = { viewModel.onAction(AuthAction.OnPassportChange(it)) },
                label = { Text("Pasaporte (máximo 9 caracteres) *") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp), singleLine = true,
                isError = "passport" in state.registrationFieldErrors,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.phone,
            onValueChange = { viewModel.onAction(AuthAction.OnPhoneChange(it)) },
            label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = "phone" in state.registrationFieldErrors,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = { viewModel.onAction(AuthAction.OnPasswordChange(it)) },
            label = { Text("Contraseña *") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp), singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña")
                }
            },
            isError = "password" in state.registrationFieldErrors,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.passwordConfirmation,
            onValueChange = { viewModel.onAction(AuthAction.OnPasswordConfirmationChange(it)) },
            label = { Text("Confirmar contraseña *") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp), singleLine = true,
            visualTransformation = if (confirmationVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmationVisible = !confirmationVisible }) {
                    Icon(if (confirmationVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (confirmationVisible) "Ocultar contraseña" else "Mostrar contraseña")
                }
            },
            isError = "passwordConfirmation" in state.registrationFieldErrors,
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
