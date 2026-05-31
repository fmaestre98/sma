package cu.uci.cisol.monitoreoambiental.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulPrimario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSecundario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoAppScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guía de Uso", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulPrimario
                )
            )
        },
        containerColor = AzulSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            InfoSection(
                title = "Funcionamiento Offline",
                icon = Icons.Default.Info,
                description = "La aplicación permite crear y guardar incidencias de forma local, incluso si no tienes conexión a internet. Estas incidencias se guardarán como 'Borradores' o 'Pendientes' hasta que se envíen al servidor."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoSection(
                title = "Proceso de Sincronización",
                icon = Icons.Default.Refresh,
                description = "Cuando el dispositivo recupere la conexión a internet, la aplicación sincronizará automáticamente las incidencias pendientes en segundo plano. También puedes intentar enviarlas manualmente desde la lista de incidencias."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoSection(
                title = "Permisos Necesarios",
                icon = Icons.Default.Lock,
                description = "Para la correcta creación de incidencias, la app requiere permisos de: \n• Cámara / Galería: Para adjuntar fotos de la incidencia.\n• Ubicación: Para registrar el lugar exacto de la incidencia de manera automática."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoSection(
                title = "Flujo de Creación",
                icon = Icons.Default.Info,
                description = "1. Selecciona 'Nueva Incidencia'.\n2. Completa los datos requeridos (título, descripción, tipo).\n3. Opcionalmente añade fotos de evidencia.\n4. La ubicación se obtendrá automáticamente si tienes el GPS activado.\n5. Guarda la incidencia. Si hay internet se enviará enseguida, de lo contrario quedará pendiente."
            )
        }
    }
}

@Composable
fun InfoSection(title: String, icon: ImageVector, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AzulSecundario
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrimario
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
    }
}
