package cu.uci.cisol.monitoreoambiental.ui.incidencia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulPrimario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSecundario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSurface
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisIncidenciasRoot(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String, Boolean) -> Unit,
    viewModel: MisIncidenciasViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    MisIncidenciasScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        onNavigateToDetail = onNavigateToDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisIncidenciasScreen(
    state: MisIncidenciasState,
    onAction: (MisIncidenciasAction) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String, Boolean) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis Incidencias",
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
                actions = {
                    IconButton(onClick = { onAction(MisIncidenciasAction.OnRefresh) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = Color.White,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .consumeWindowInsets(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onAction(MisIncidenciasAction.OnSearchQueryChange(it)) },
                placeholder = { Text("Buscar...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar")
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onAction(MisIncidenciasAction.OnSearchQueryChange("")) }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzulPrimario,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                IncidenciaFiltro.entries.forEach { filtro ->
                    FilterChip(
                        selected = state.filtro == filtro,
                        onClick = { onAction(MisIncidenciasAction.OnFiltroChange(filtro)) },
                        label = { Text(filtro.label) },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AzulPrimario.copy(alpha = 0.15f),
                            selectedLabelColor = AzulPrimario
                        )
                    )
                }
            }

            if (state.isLoading && state.incidenciasPorFecha.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AzulPrimario)
                }
            } else if (state.incidenciasPorFecha.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay incidencias para mostrar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp)
                ) {
                    state.incidenciasPorFecha.forEach { (fecha, incidencias) ->
                        item {
                            Text(
                                text = fecha,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AzulPrimario
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(incidencias, key = { it.id }) { incidencia ->
                            IncidenciaCard(
                                incidencia = incidencia,
                                onClick = { onNavigateToDetail(incidencia.id, incidencia.esBorrador ||
                                        (!incidencia.sincronizado && incidencia.serverId == null)) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (state.hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(color = AzulPrimario, modifier = Modifier.size(24.dp))
                                } else {
                                    Button(
                                        onClick = { onAction(MisIncidenciasAction.OnLoadNextPage) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Cargar más")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncidenciaCard(
    incidencia: IncidenciaUi,
    onClick: () -> Unit
) {
    val themeColorHex = incidencia.tematicaColor ?: "#64748b"
    val themeColor = try {
        Color(android.graphics.Color.parseColor(themeColorHex))
    } catch (e: Exception) {
        Color(0xFF64748B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left stripe for theme color
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(130.dp) // Wait, use fillMaxHeight/Spacer or layout constraint instead of hardcoded if possible, but 130.dp or aligning height works well. Let's make it match the content height.
                    .background(themeColor)
            )

            Column(modifier = Modifier.padding(12.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = incidencia.tematica,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = incidencia.horaFormateada,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = incidencia.descripcionCorta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Chip
                    EstadoChip(nombre = incidencia.estadoNombre, colorHex = incidencia.estadoColorHex)
                    Spacer(modifier = Modifier.width(8.dp))
                    // Criticality Chip
                    val critColorHex = incidencia.criticalityColor ?: "#808080"
                    val critName = incidencia.criticality ?: "No evaluada"
                    EstadoChip(nombre = critName, colorHex = critColorHex)
                }
            }
        }
    }
}
