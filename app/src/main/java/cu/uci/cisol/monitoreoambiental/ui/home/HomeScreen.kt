package cu.uci.cisol.monitoreoambiental.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cu.uci.cisol.monitoreoambiental.ui.home.components.CrearIncidenciaCard
import cu.uci.cisol.monitoreoambiental.ui.home.components.DrawerDestino
import cu.uci.cisol.monitoreoambiental.ui.home.components.DrawerMenu
import cu.uci.cisol.monitoreoambiental.ui.home.components.EstadisticasSection
import cu.uci.cisol.monitoreoambiental.ui.home.components.HomeHeader
import cu.uci.cisol.monitoreoambiental.ui.home.components.HomeIncidenciaCard
import cu.uci.cisol.monitoreoambiental.ui.home.components.MisIncidenciasFilters
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulPrimario
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNuevaIncidencia: () -> Unit = {},
    onMisIncidencias: () -> Unit = {},
    onVerBorradores: () -> Unit = {},
    onInfoApp: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToDetail: (String, Boolean) -> Unit = { _, _ -> },
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
                        is DrawerDestino.InfoApp -> onInfoApp()
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
            containerColor = Color.White,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentPadding = PaddingValues(
                        bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + 16.dp
                    )
                ) {
                    item {
                        HomeHeader(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                    }

                    item {
                        CrearIncidenciaCard(
                            onClick = onNuevaIncidencia,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }

                    item {
                        EstadisticasSection(
                            estadisticas = uiState.estadisticas,
                            selectedFiltro = uiState.selectedFiltro,
                            onEstadisticaClick = viewModel::onFiltroChange,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        MisIncidenciasFilters(
                            selectedFiltro = uiState.selectedFiltro,
                            selectedMes = uiState.selectedMes,
                            selectedEstadoDropdown = uiState.selectedEstadoDropdown,
                            mesesDisponibles = uiState.mesesDisponibles,
                            estados = uiState.conteosPorEstado,
                            onTodasClick = viewModel::onTodasIncidenciasClick,
                            onMesChange = viewModel::onMesChange,
                            onEstadoChange = viewModel::onEstadoDropdownChange,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (uiState.incidenciasFiltradas.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay incidencias para mostrar",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        items(
                            items = uiState.incidenciasFiltradas,
                            key = { it.id }
                        ) { incidencia ->
                            HomeIncidenciaCard(
                                incidencia = incidencia,
                                onClick = {
                                    onNavigateToDetail(incidencia.id, incidencia.esBorrador ||
                                            (!incidencia.sincronizado && incidencia.serverId == null))
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
