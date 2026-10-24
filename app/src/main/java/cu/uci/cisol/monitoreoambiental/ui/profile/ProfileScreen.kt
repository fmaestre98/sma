package cu.uci.cisol.monitoreoambiental.ui.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulPrimario
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch

private const val MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024L
private val allowedAvatarMimeTypes = setOf("image/jpeg", "image/png", "image/webp", "image/svg+xml")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var avatarMenuExpanded by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        coroutineScope.launch { validateAndSelectAvatar(context, uri, viewModel, snackbarHostState) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) coroutineScope.launch { validateAndSelectAvatar(context, cameraUri, viewModel, snackbarHostState) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ProfileEvent.Error -> snackbarHostState.showSnackbar(event.uiText.asString(context))
                is ProfileEvent.MessageError -> snackbarHostState.showSnackbar(event.message)
                ProfileEvent.UpdateSuccess -> snackbarHostState.showSnackbar("Datos del perfil actualizados exitosamente")
                ProfileEvent.PasswordUpdateSuccess -> snackbarHostState.showSnackbar("Contraseña actualizada exitosamente")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil de Usuario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulPrimario,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (uiState.isLoading && uiState.user == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .clickable { avatarMenuExpanded = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            uiState.localAvatarUri != null -> AsyncImage(
                                model = uiState.localAvatarUri,
                                contentDescription = "Avatar seleccionado",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            !uiState.user?.avatarUrl.isNullOrEmpty() -> AsyncImage(
                                model = uiState.user?.avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            else -> Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray)
                        }
                    }
                    DropdownMenu(expanded = avatarMenuExpanded, onDismissRequest = { avatarMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Tomar foto") },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                            onClick = {
                                avatarMenuExpanded = false
                                val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
                                val file = File(imagesDir, "profile_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                cameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Elegir de la galería") },
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                            onClick = {
                                avatarMenuExpanded = false
                                galleryLauncher.launch("image/*")
                            },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Rol: ${uiState.user?.role.orEmpty()}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = uiState.userName,
                    onValueChange = viewModel::onUserNameChange,
                    label = { Text("Nombre de usuario *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.userNameError != null,
                    supportingText = { uiState.userNameError?.let { Text(it) } },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.firstName,
                    onValueChange = viewModel::onFirstNameChange,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.firstNameError != null,
                    supportingText = { uiState.firstNameError?.let { Text(it) } },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.lastName,
                    onValueChange = viewModel::onLastNameChange,
                    label = { Text("Apellidos") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.lastNameError != null,
                    supportingText = { uiState.lastNameError?.let { Text(it) } },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = countryDisplayName(uiState.country),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("País") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                when {
                    uiState.country == "CU" -> {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.ci,
                            onValueChange = viewModel::onCiChange,
                            readOnly = uiState.user?.ci?.isNotBlank() == true,
                            label = { Text("CI${if (uiState.ci.isBlank()) " *" else ""}") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = uiState.ciError != null,
                            supportingText = { uiState.ciError?.let { Text(it) } },
                        )
                    }
                    uiState.country.isNotBlank() -> {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.passport,
                            onValueChange = viewModel::onPassportChange,
                            readOnly = uiState.user?.passport?.isNotBlank() == true,
                            label = { Text("Pasaporte${if (uiState.passport.isBlank()) " *" else ""}") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = uiState.passportError != null,
                            supportingText = { uiState.passportError?.let { Text(it) } },
                        )
                    }
                    else -> Text("País e identificación no informados por el servidor", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = uiState.phoneError != null,
                    supportingText = { uiState.phoneError?.let { Text(it) } },
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = viewModel::showChangePassword, modifier = Modifier.fillMaxWidth()) {
                    Text("Cambiar contraseña")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::onSave,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario),
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Guardar Cambios")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (uiState.showChangePassword) {
        AlertDialog(
            onDismissRequest = viewModel::cancelChangePassword,
            title = { Text("Cambiar contraseña") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PasswordField(
                        value = uiState.currentPassword,
                        label = "Contraseña actual *",
                        visible = currentPasswordVisible,
                        error = uiState.currentPasswordError,
                        onVisibilityChange = { currentPasswordVisible = it },
                        onValueChange = viewModel::onCurrentPasswordChange,
                    )
                    PasswordField(
                        value = uiState.newPassword,
                        label = "Nueva contraseña *",
                        visible = newPasswordVisible,
                        error = uiState.newPasswordError,
                        onVisibilityChange = { newPasswordVisible = it },
                        onValueChange = viewModel::onNewPasswordChange,
                    )
                    PasswordField(
                        value = uiState.confirmNewPassword,
                        label = "Confirmar nueva contraseña *",
                        visible = confirmPasswordVisible,
                        error = uiState.confirmPasswordError,
                        onVisibilityChange = { confirmPasswordVisible = it },
                        onValueChange = viewModel::onConfirmNewPasswordChange,
                    )
                    uiState.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::submitChangePassword, enabled = !uiState.passwordChangeLoading) {
                    if (uiState.passwordChangeLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Guardar")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelChangePassword) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PasswordField(
    value: String,
    label: String,
    visible: Boolean,
    error: String?,
    onVisibilityChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { onVisibilityChange(!visible) }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                )
            }
        },
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
    )
}

private suspend fun validateAndSelectAvatar(
    context: Context,
    uri: Uri?,
    viewModel: ProfileViewModel,
    snackbarHostState: SnackbarHostState,
) {
    if (uri == null) return
    val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT)
    val size = runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor -> descriptor.length }
            ?.takeIf { it >= 0 }
            ?: context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (total <= MAX_AVATAR_SIZE_BYTES) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                }
                total
            }
    }.getOrNull()
    when {
        mimeType !in allowedAvatarMimeTypes -> snackbarHostState.showSnackbar("La foto debe ser JPG, JPEG, WEBP, SVG o PNG y no superar 5 MB")
        size == null || size > MAX_AVATAR_SIZE_BYTES -> snackbarHostState.showSnackbar("La foto debe ser JPG, JPEG, WEBP, SVG o PNG y no superar 5 MB")
        else -> viewModel.onAvatarSelected(uri)
    }
}

private fun countryDisplayName(countryCode: String): String = when {
    countryCode.isBlank() -> ""
    countryCode.length == 2 -> Locale("", countryCode).getDisplayCountry(Locale("es"))
    else -> countryCode
}
