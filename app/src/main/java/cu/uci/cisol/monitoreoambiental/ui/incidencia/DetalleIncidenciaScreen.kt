package cu.uci.cisol.monitoreoambiental.ui.incidencia

import android.app.Application
import android.net.Uri
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import cu.uci.cisol.monitoreoambiental.R
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulPrimario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSecundario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSurface
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import java.io.File
import java.io.FileOutputStream

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
                is DetalleIncidenciaEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    DetalleIncidenciaScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleIncidenciaScreen(
    state: DetalleIncidenciaState,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    var imagenAmpliada by remember { mutableStateOf<Uri?>(null) }
    var mostrarMapa by remember { mutableStateOf(false) }

    if (mostrarMapa && state.latitud != null && state.longitud != null) {
        MapaIncidenciaDialog(
            latitud = state.latitud,
            longitud = state.longitud,
            onDismiss = { mostrarMapa = false },
        )
    }

    imagenAmpliada?.let { uri ->
        Dialog(onDismissRequest = { imagenAmpliada = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Evidencia ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                )
                IconButton(
                    onClick = { imagenAmpliada = null },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar imagen", tint = Color.White)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Incidencia", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.horizontalGradient(colors = listOf(AzulPrimario, AzulSecundario)),
                ),
            )
        },
        containerColor = AzulSurface,
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AzulPrimario)
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .consumeWindowInsets(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.ticketNumber?.let { InfoRow("Ticket", it.toString()) }
                InfoRow("Temática", state.tematicaVisible)
                InfoRow("Localidad", state.localidadVisible)
                InfoRow("Fecha", state.fechaFormateada)
                state.reporterName?.let { InfoRow("Reportante", it) }
                state.assignedToName?.let { InfoRow("Asignado a", it) }
                state.updatedAt?.let { InfoRow("Actualizado", it) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    EstadoChip(nombre = state.statusName, colorHex = state.statusColor)
                    Spacer(modifier = Modifier.size(8.dp))
                    EstadoChip(nombre = state.criticality, colorHex = state.criticalityColor)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Descripción", style = MaterialTheme.typography.titleMedium)
                Text(state.descripcion, style = MaterialTheme.typography.bodyLarge)
                if (state.address.isNotBlank()) InfoRow("Dirección", state.address)

                if (state.latitud != null && state.longitud != null) {
                    InfoRow("Ubicación", "${state.latitud}, ${state.longitud}")
                    TextButton(
                        onClick = { mostrarMapa = true },
                    ) {
                        Text("Ver en el mapa")
                    }
                }

                Text("Evidencias", style = MaterialTheme.typography.titleMedium)
                if (state.imagenesUris.isEmpty()) {
                    Text("Esta incidencia no tiene imágenes disponibles.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.imagenesUris, key = { it.toString() }) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Foto de evidencia",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { imagenAmpliada = uri },
                                onError = {
                                    android.util.Log.w("DetalleIncidencia", "No se pudo cargar evidencia: $uri", it.result.throwable)
                                },
                                error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.BrokenImage),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapaIncidenciaDialog(
    latitud: Double,
    longitud: Double,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var mapFile by remember { mutableStateOf<File?>(null) }
    var isMapPreparing by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            AndroidGraphicFactory.createInstance(context.applicationContext as Application)
        } catch (_: Exception) {
            // Mapsforge puede haberse inicializado ya en el selector de ubicación.
        }
        mapFile = withContext(Dispatchers.IO) {
            val destination = File(context.filesDir, "Cuba_oam.osm.map")
            if (!destination.exists()) {
                try {
                    context.assets.open("Cuba_oam.osm.map").use { input ->
                        FileOutputStream(destination).use { output -> input.copyTo(output) }
                    }
                } catch (_: Exception) {
                    return@withContext null
                }
            }
            destination
        }
        isMapPreparing = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubicación de la incidencia") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth().height(480.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isMapPreparing -> CircularProgressIndicator(color = AzulPrimario)
                    mapFile == null -> Text("No se pudo cargar el mapa offline.")
                    else -> AndroidView(
                        factory = { ctx ->
                            val offlineMap = requireNotNull(mapFile)
                            MapView(ctx).apply {
                                mapScaleBar.isVisible = true
                                setCenter(LatLong(latitud, longitud))
                                setZoomLevel(17.toByte())

                                val tileCache: TileCache = AndroidUtil.createTileCache(
                                    ctx,
                                    "incidencia-map-cache",
                                    model.displayModel.tileSize,
                                    1f,
                                    model.frameBufferModel.overdrawFactor,
                                )
                                val mapDataStore: MapDataStore = MapFile(offlineMap)
                                val renderer = TileRendererLayer(
                                    tileCache,
                                    mapDataStore,
                                    model.mapViewPosition,
                                    AndroidGraphicFactory.INSTANCE,
                                )
                                renderer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER)
                                layerManager.layers.add(renderer)

                                val markerDrawable = requireNotNull(ctx.getDrawable(R.drawable.ic_map_marker))
                                val markerBitmap = AndroidGraphicFactory.convertToBitmap(markerDrawable)
                                layerManager.layers.add(
                                    Marker(LatLong(latitud, longitud), markerBitmap, -markerBitmap.width / 2, -markerBitmap.height),
                                )
                            }
                        },
                        onRelease = { it.destroyAll() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        modifier = Modifier.widthIn(max = 640.dp),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
