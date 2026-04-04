package cu.uci.cisol.monitoreoambiental.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cu.uci.cisol.monitoreoambiental.ui.home.components.BarChart
import cu.uci.cisol.monitoreoambiental.ui.home.components.DonutChart
import cu.uci.cisol.monitoreoambiental.ui.home.components.DrawerDestino
import cu.uci.cisol.monitoreoambiental.ui.home.components.DrawerMenu
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulPrimario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSecundario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSurface
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNuevaIncidencia: () -> Unit = {},
    onMisIncidencias: () -> Unit = {},
    onVerBorradores: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerMenu(
                rutaActual = "home",
                onNavegar = { destino ->
                    scope.launch { drawerState.close() }
                    when (destino) {
                        is DrawerDestino.Principal -> { /* ya estamos aquí */ }
                        is DrawerDestino.NuevaIncidencia -> onNuevaIncidencia()
                        is DrawerDestino.MisIncidencias -> onMisIncidencias()
                        else -> {}
                    }
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Estadísticas",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú",
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
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
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
                    // Tarjeta: Gráfico Donut
                    SectionCard(titulo = "Incidencias") {
                        DonutChart(
                            conteos = uiState.conteosPorEstado,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tarjeta: Gráfico de Barras por Mes
                    SectionCard(titulo = "Incidencias por meses") {
                        BarChart(
                            datos = uiState.datosMensuales,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tarjeta: resumen rápido de estados sin enviar
                    val pendientes = uiState.conteosPorEstado
                        .firstOrNull { it.estado == cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia.PENDIENTE }
                        ?.count ?: 0
                    if (pendientes > 0) {
                        PendientesBanner(cantidad = pendientes)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Banner de Borradores
                    val borradores = uiState.conteosPorEstado
                        .firstOrNull { it.estado == cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia.BORRADOR }
                        ?.count ?: 0
                    if (borradores > 0) {
                        BorradoresBanner(
                            cantidad = borradores,
                            onClick = onVerBorradores
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    titulo: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulPrimario
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun PendientesBanner(cantidad: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⏳ Pendientes de sincronización",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$cantidad incidencia${if (cantidad > 1) "s" else ""} aún no enviada${if (cantidad > 1) "s" else ""} al servidor.",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF795548))
            )
        }
    }
}

@Composable
private fun BorradoresBanner(
    cantidad: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD) // Light blue
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📝 Tienes $cantidad borrador${if (cantidad > 1) "es" else ""}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AzulPrimario
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Toca para abrir y terminar de editar",
                style = MaterialTheme.typography.bodySmall.copy(color = AzulSecundario)
            )
        }
    }
}
