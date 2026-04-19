package cu.uci.cisol.monitoreoambiental.ui.incidencia

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.domain.model.Tematica
import cu.uci.cisol.monitoreoambiental.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.io.File
import cu.uci.cisol.monitoreoambiental.ui.incidencia.EstadoChip
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import android.app.Application
import org.mapsforge.map.rendertheme.InternalRenderTheme
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaIncidenciaRoot(
    onNavigateBack: () -> Unit,
    viewModel: NuevaIncidenciaViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Manejar eventos
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NuevaIncidenciaEvent.NavigateBack -> onNavigateBack()
                is NuevaIncidenciaEvent.ShowSnackbar -> {
                    // El SnackbarHost lo manejamos en la pantalla
                }
            }
        }
    }

    NuevaIncidenciaScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        events = viewModel.events
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NuevaIncidenciaScreen(
    state: NuevaIncidenciaState,
    onAction: (NuevaIncidenciaAction) -> Unit,
    onNavigateBack: () -> Unit,
    events: kotlinx.coroutines.flow.Flow<NuevaIncidenciaEvent>
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        events.collect { event ->
            if (event is NuevaIncidenciaEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                onAction(NuevaIncidenciaAction.OnFotoTomada(photoUri))
            }
        }
    )

    var resolutionPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takeResolutionPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                onAction(NuevaIncidenciaAction.OnFotoResolucionTomada(resolutionPhotoUri))
            }
        }
    )

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    if (state.showMapDialog) {
        AlertDialog(
            onDismissRequest = { onAction(NuevaIncidenciaAction.OnCerrarMapa) },
            title = { Text("Seleccionar lugar") },
            text = {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)) {
                    var mapFile by remember { mutableStateOf<File?>(null) }
                    var isMapPreparing by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        try {
                            AndroidGraphicFactory.createInstance(context.applicationContext as Application)
                        } catch (e: Exception) {
                            // Ignorar si ya está inicializado
                        }
                        
                        mapFile = withContext(Dispatchers.IO) {
                            val filename = "Cuba_oam.osm.map"
                            val destFile = File(context.filesDir, filename)
                            if (!destFile.exists()) {
                                try {
                                    context.assets.open(filename).use { input ->
                                        FileOutputStream(destFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    return@withContext null
                                }
                            }
                            destFile
                        }
                        isMapPreparing = false
                    }

                    if (isMapPreparing) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(color = AzulPrimario)
                        }
                    } else if (mapFile != null) {
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    isClickable = true
                                    mapScaleBar.isVisible = true
                                    
                                    // Set default position
                                    setCenter(LatLong(23.1492, -81.2494))
                                    setZoomLevel(13.toByte())

                                    // Tile cache
                                    val tileCache: TileCache = AndroidUtil.createTileCache(
                                        ctx, "mapcache",
                                        model.displayModel.tileSize, 1f,
                                        model.frameBufferModel.overdrawFactor
                                    )

                                    // Load map
                                    val mapDataStore: MapDataStore = MapFile(mapFile)
                                    val tileRendererLayer = TileRendererLayer(
                                        tileCache, mapDataStore,
                                        model.mapViewPosition,
                                        AndroidGraphicFactory.INSTANCE
                                    )
                                    tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER)
                                    layerManager.layers.add(tileRendererLayer)
                                    
                                    // Touch listener via Layer
                                    val tapLayer = object : Layer() {
                                        override fun draw(boundingBox: org.mapsforge.core.model.BoundingBox?, zoomLevel: Byte, canvas: org.mapsforge.core.graphics.Canvas?, topLeftPoint: org.mapsforge.core.model.Point?) {}
                                        
                                        override fun onTap(tapLatLong: LatLong?, layerXY: org.mapsforge.core.model.Point?, tapXY: org.mapsforge.core.model.Point?): Boolean {
                                            if (tapLatLong != null) {
                                                onAction(NuevaIncidenciaAction.OnMapLocationSelected(tapLatLong.latitude, tapLatLong.longitude))
                                                // Optionally call OnCerrarMapa behavior if needed, currently done in the screen action handler or manual close
                                            }
                                            return true
                                        }
                                    }
                                    layerManager.layers.add(tapLayer)
                                }
                            },
                            onRelease = { mapView ->
                                mapView.destroyAll()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Error al cargar mapa offline.", color = Color.Red)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onAction(NuevaIncidenciaAction.OnCerrarMapa) }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.modo == FormMode.CREAR) "Nueva Incidencia" else "Detalle Incidencia",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(AzulPrimario, AzulSecundario)
                    )
                )
            )
        },
        containerColor = AzulSurface
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AzulPrimario)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (state.modo == FormMode.EDITAR) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estado actual:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        EstadoChip(estado = state.estado)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Dropdown Temática
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = state.tematica?.label ?: "Seleccionar Temática",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedBorderColor = AzulPrimario
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Tematica.entries.forEach { tematica ->
                            DropdownMenuItem(
                                text = { Text(tematica.label) },
                                onClick = {
                                    onAction(NuevaIncidenciaAction.OnTematicaChange(tematica))
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Descripción
                OutlinedTextField(
                    value = state.descripcion,
                    onValueChange = { onAction(NuevaIncidenciaAction.OnDescripcionChange(it)) },
                    label = { Text("Descripción") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulPrimario
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Foto de evidencia",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AzulPrimario
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Foto button and preview
                if (state.imagenUri != null) {
                    AsyncImage(
                        model = state.imagenUri,
                        contentDescription = "Foto de incidencia",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Button(
                    onClick = {
                        if (cameraPermissionState.status.isGranted) {
                            photoUri = createTempImageUri(context)
                            takePictureLauncher.launch(photoUri!!)
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulSecundario)
                ) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.imagenUri == null) "Tomar Foto" else "Cambiar Foto")
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Ubicación",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AzulPrimario
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.latitud != null && state.longitud != null) {
                    Text(
                        text = "Coordenadas: ${state.latitud}, ${state.longitud}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (locationPermissionState.status.isGranted) {
                                onAction(NuevaIncidenciaAction.OnObtenerCoordenadas)
                                // Simulando obtención de GPS
                                onAction(NuevaIncidenciaAction.OnCoordenadasObtenidas(23.113592, -82.366592)) // TODO: Hook real FusedLocationProviderClient
                            } else {
                                locationPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AzulClaro)
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GPS")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAction(NuevaIncidenciaAction.OnSeleccionarEnMapa) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrimario),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AzulPrimario)
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mapa")
                    }
                }

                // Sección para foto de resolución si estamos en EDITAR
                if (state.modo == FormMode.EDITAR) {
                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.estado == EstadoIncidencia.RESUELTA) {
                        Text(
                            text = "Foto de Resolución (Evidencia)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ColorResuelta
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.imagenResolucionUri != null) {
                            AsyncImage(
                                model = state.imagenResolucionUri,
                                contentDescription = "Foto de resolución",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else if (state.estado != EstadoIncidencia.RESUELTA && state.estado != EstadoIncidencia.BORRADOR) {
                        Button(
                            onClick = {
                                if (cameraPermissionState.status.isGranted) {
                                    resolutionPhotoUri = createTempImageUri(context)
                                    takeResolutionPictureLauncher.launch(resolutionPhotoUri!!)
                                    // MarcarResuelta should happen after we take the photo visually, but for now we do it directly:
                                    onAction(NuevaIncidenciaAction.OnMarcarComoResuelta)
                                } else {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorAceptada)
                        ) {
                            Text("Añadir foto y Marcar como Resuelta")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bottom Buttons
                if (state.modo == FormMode.CREAR || state.estado == EstadoIncidencia.BORRADOR) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onAction(NuevaIncidenciaAction.OnGuardarBorrador) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrimario)
                        ) {
                            Text("Guardar Borrador")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { onAction(NuevaIncidenciaAction.OnEnviarAlServidor) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario)
                        ) {
                            Text("Enviar")
                        }
                    }
                }
            }
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images")
    if (!imagesDir.exists()) {
        imagesDir.mkdirs()
    }
    val tempFile = File.createTempFile(
        "incidencia_img_${System.currentTimeMillis()}", ".jpg",
        imagesDir
    ).apply { createNewFile() }
    
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}
