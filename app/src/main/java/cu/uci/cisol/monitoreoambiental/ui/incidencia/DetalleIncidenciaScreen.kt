package cu.uci.cisol.monitoreoambiental.ui.incidencia

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulClaro
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulPrimario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSecundario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleIncidenciaRoot(
    onNavigateBack: () -> Unit,
    viewModel: DetalleIncidenciaViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                DetalleIncidenciaEvent.NavigateBack -> onNavigateBack()
                is DetalleIncidenciaEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    DetalleIncidenciaScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DetalleIncidenciaScreen(
    state: DetalleIncidenciaState,
    onAction: (DetalleIncidenciaAction) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var showDateTimePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
            onResult = { success ->
                if (success) {
                    onAction(DetalleIncidenciaAction.OnFotoTomada(photoUri))
                }
            },
        )

    val pickMedia =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxItems = 2),
        ) { uris ->
            if (uris.isNotEmpty()) {
                onAction(DetalleIncidenciaAction.OnFotoSeleccionada(uris))
            }
        }

    if (state.showMapDialog) {
        AlertDialog(
            onDismissRequest = { onAction(DetalleIncidenciaAction.OnCerrarMapa) },
            title = { Text("Seleccionar lugar") },
            text = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                ) {
                    var mapFile by remember { mutableStateOf<File?>(null) }
                    var isMapPreparing by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        try {
                            AndroidGraphicFactory.createInstance(
                                context.applicationContext as Application,
                            )
                        } catch (_: Exception) {
                        }

                        mapFile =
                            withContext(Dispatchers.IO) {
                                val filename = "Cuba_oam.osm.map"
                                val destFile = File(context.filesDir, filename)
                                if (!destFile.exists()) {
                                    try {
                                        context.assets.open(filename).use { input ->
                                            FileOutputStream(destFile).use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                    } catch (_: Exception) {
                                        return@withContext null
                                    }
                                }
                                destFile
                            }
                        isMapPreparing = false
                    }

                    if (isMapPreparing) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            CircularProgressIndicator(color = AzulPrimario)
                        }
                    } else if (mapFile != null) {
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    isClickable = true
                                    mapScaleBar.isVisible = true
                                    setCenter(LatLong(23.1492, -81.2494))
                                    setZoomLevel(13.toByte())

                                    val tileCache: TileCache =
                                        AndroidUtil.createTileCache(
                                            ctx,
                                            "mapcache_detalle",
                                            model.displayModel.tileSize,
                                            1f,
                                            model.frameBufferModel.overdrawFactor,
                                        )

                                    val mapDataStore: MapDataStore = MapFile(mapFile)
                                    val tileRendererLayer =
                                        TileRendererLayer(
                                            tileCache,
                                            mapDataStore,
                                            model.mapViewPosition,
                                            AndroidGraphicFactory.INSTANCE,
                                        )
                                    tileRendererLayer.setXmlRenderTheme(
                                        InternalRenderTheme.OSMARENDER,
                                    )
                                    layerManager.layers.add(tileRendererLayer)

                                    val tapLayer =
                                        object : Layer() {
                                            override fun draw(
                                                boundingBox: org.mapsforge.core.model.BoundingBox?,
                                                zoomLevel: Byte,
                                                canvas: org.mapsforge.core.graphics.Canvas?,
                                                topLeftPoint: org.mapsforge.core.model.Point?,
                                            ) {
                                            }

                                            override fun onTap(
                                                tapLatLong: LatLong?,
                                                layerXY: org.mapsforge.core.model.Point?,
                                                tapXY: org.mapsforge.core.model.Point?,
                                            ): Boolean {
                                                if (tapLatLong != null) {
                                                    onAction(
                                                        DetalleIncidenciaAction.OnMapLocationSelected(
                                                            tapLatLong.latitude,
                                                            tapLatLong.longitude,
                                                        ),
                                                    )
                                                }
                                                return true
                                            }
                                        }
                                    layerManager.layers.add(tapLayer)
                                }
                            },
                            onRelease = { mapView -> mapView.destroyAll() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text("Error al cargar mapa offline.", color = Color.Red)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onAction(DetalleIncidenciaAction.OnCerrarMapa) }) {
                    Text("Cerrar")
                }
            },
        )
    }

    if (showDateTimePicker) {
        Dialog(
            onDismissRequest = { showDateTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)   // ← permite crecer
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()            // se estira al máximo posible del diálogo
                    .wrapContentHeight()
                    .widthIn(max = 480.dp)     // límite para pantallas grandes
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {                    Text(
                    text = "Seleccionar fecha y hora",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = state.fechaHora
                    )
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val timePickerState = rememberTimePickerState(
                        initialHour = Calendar.getInstance().apply { timeInMillis = state.fechaHora }
                            .get(Calendar.HOUR_OF_DAY),
                        initialMinute = Calendar.getInstance().apply { timeInMillis = state.fechaHora }
                            .get(Calendar.MINUTE)
                    )
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDateTimePicker = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                val selectedDate = datePickerState.selectedDateMillis ?: state.fechaHora
                                val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                calendar.set(Calendar.MINUTE, timePickerState.minute)
                                calendar.set(Calendar.SECOND, 0)
                                calendar.set(Calendar.MILLISECOND, 0)
                                onAction(DetalleIncidenciaAction.OnFechaHoraChange(calendar.timeInMillis))
                                showDateTimePicker = false
                            }
                        ) {
                            Text("Aceptar")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalle de Incidencia",
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier =
                    Modifier.background(
                        brush = Brush.horizontalGradient(colors = listOf(AzulPrimario, AzulSecundario)),
                    ),
            )
        },
        containerColor = AzulSurface,
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AzulPrimario)
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .consumeWindowInsets(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .padding(16.dp),
            ) {
                SectionTitle("Información")
                InfoRow("Ticket", state.ticketNumber?.toString() ?: "—")
                InfoRow("Temática", state.themeName ?: state.selectedTematica?.name ?: "—")
                InfoRow("Localidad", state.localityName ?: state.selectedLocalidad?.let { "${it.name} (${it.municipality})" } ?: "—")
                InfoRow("Fecha", state.fechaFormateada)
                state.reporterName?.let { InfoRow("Reportante", it) }
                state.assignedToName?.let { InfoRow("Asignado a", it) }
                state.updatedAt?.let { InfoRow("Actualizado", it) }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EstadoChip(nombre = state.statusName, colorHex = state.statusColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    EstadoChip(nombre = state.criticality, colorHex = state.criticalityColor)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("Editar")
                TematicaDropdown(state, onAction)
                Spacer(modifier = Modifier.height(16.dp))
                LocalidadDropdown(state, onAction)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.descripcion,
                    onValueChange = { onAction(DetalleIncidenciaAction.OnDescripcionChange(it)) },
                    label = { Text("Descripción") },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(focusedBorderColor = AzulPrimario),
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.address,
                    onValueChange = { onAction(DetalleIncidenciaAction.OnAddressChange(it)) },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        OutlinedTextFieldDefaults.colors(focusedBorderColor = AzulPrimario),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDateTimePicker = true }
                ) {
                    // Colores "falsos" para que parezca habilitado
                    val dateFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulPrimario,
                        unfocusedBorderColor = AzulPrimario,
                        disabledBorderColor = AzulPrimario,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = dateFormatter.format(Date(state.fechaHora)),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,                        // ← deshabilitado
                        label = { Text("Fecha y hora") },
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                        },
                        colors = dateFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                /*Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.marcadaResuelta,
                        onCheckedChange = {
                            onAction(DetalleIncidenciaAction.OnMarcadaResueltaChange(it))
                        },
                    )
                    Text(
                        text = "Marcar como Resuelta",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }*/

                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle("Ubicación")
                if (state.latitud != null && state.longitud != null) {
                    Text(
                        text = "Coordenadas: ${state.latitud}, ${state.longitud}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (locationPermissionState.status.isGranted) {
                                onAction(DetalleIncidenciaAction.OnObtenerCoordenadas)
                                onAction(
                                    DetalleIncidenciaAction.OnCoordenadasObtenidas(
                                        23.113592,
                                        -82.366592,
                                    ),
                                )
                            } else {
                                locationPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AzulClaro),
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GPS")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAction(DetalleIncidenciaAction.OnSeleccionarEnMapa) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.outlinedButtonColors(contentColor = AzulPrimario),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AzulPrimario),
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mapa")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle("Evidencias de resolución (máx. 2)")

                if (state.evidenciasResolucionUris.isNotEmpty()) {
                    LazyRow(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.evidenciasResolucionUris) { uri ->
                            Box(modifier = Modifier.size(120.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Evidencia",
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                IconButton(
                                    onClick = {
                                        onAction(DetalleIncidenciaAction.OnFotoEliminada(uri))
                                    },
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.5f),
                                                RoundedCornerShape(12.dp),
                                            ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Eliminar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.evidenciasResolucionUris.size < 2) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (cameraPermissionState.status.isGranted) {
                                    photoUri = createDetalleTempImageUri(context)
                                    takePictureLauncher.launch(photoUri!!)
                                } else {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AzulSecundario),
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cámara")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                pickMedia.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario),
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Galería")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onAction(DetalleIncidenciaAction.OnGuardar) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Guardar cambios")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = AzulPrimario,
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.DarkGray,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TematicaDropdown(
    state: DetalleIncidenciaState,
    onAction: (DetalleIncidenciaAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = state.selectedTematica?.name ?: "Seleccionar Temática",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(focusedBorderColor = AzulPrimario),
            modifier =
                Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.tematicas.forEach { tematica ->
                DropdownMenuItem(
                    text = { Text(tematica.name) },
                    onClick = {
                        onAction(DetalleIncidenciaAction.OnTematicaChange(tematica.id))
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalidadDropdown(
    state: DetalleIncidenciaState,
    onAction: (DetalleIncidenciaAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value =
                state.selectedLocalidad?.let { "${it.name} (${it.municipality})" }
                    ?: "Seleccionar Localidad",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(focusedBorderColor = AzulPrimario),
            modifier =
                Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.localidades.forEach { localidad ->
                DropdownMenuItem(
                    text = { Text("${localidad.name} (${localidad.municipality})") },
                    onClick = {
                        onAction(DetalleIncidenciaAction.OnLocalidadChange(localidad.id))
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun createDetalleTempImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images")
    if (!imagesDir.exists()) {
        imagesDir.mkdirs()
    }
    val tempFile =
        File.createTempFile(
            "detalle_evidencia_${System.currentTimeMillis()}",
            ".jpg",
            imagesDir,
        ).apply { createNewFile() }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile,
    )
}
