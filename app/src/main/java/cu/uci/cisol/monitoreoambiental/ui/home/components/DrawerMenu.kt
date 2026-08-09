package cu.uci.cisol.monitoreoambiental.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulClaro
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulPrimario
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulSecundario
import cu.uci.cisol.monitoreoambiental.ui.theme.LocalSystemAppearance

sealed class DrawerDestino(val route: String, val label: String, val icon: ImageVector) {
    data object Principal : DrawerDestino("home", "Principal", Icons.Default.Home)
    data object NuevaIncidencia : DrawerDestino("nueva_incidencia", "Nueva Incidencia", Icons.Default.Add)
    data object MisIncidencias : DrawerDestino("mis_incidencias", "Mis Incidencias", Icons.AutoMirrored.Filled.List)
    data object AtencionDirecta : DrawerDestino("atencion_directa", "Atención Directa", Icons.Default.SupportAgent)
    data object InfoApp : DrawerDestino("info_app", "Guía de Uso", Icons.Default.Info)
    data object Logout : DrawerDestino("logout", "Cerrar Sesión", Icons.AutoMirrored.Filled.ExitToApp)
}

private val opcionesNavegacion = listOf(
    DrawerDestino.Principal,
    DrawerDestino.NuevaIncidencia,
    DrawerDestino.MisIncidencias,
    DrawerDestino.AtencionDirecta,
    DrawerDestino.InfoApp
)

@Composable
fun DrawerMenu(
    rutaActual: String,
    onNavegar: (DrawerDestino) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appearance = LocalSystemAppearance.current

    ModalDrawerSheet(modifier = modifier) {
        // Cabecera del Drawer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AzulPrimario, AzulSecundario)
                    )
                )
        ) {
            if (appearance?.backgroundImage != null) {
                AsyncImage(
                    model = appearance.backgroundImage,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.6f
                )
            }
            Column(
                modifier = Modifier.padding(vertical = 28.dp, horizontal = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AzulClaro.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MA",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Monitoreo Ambiental",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Panel de Estadísticas",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Opciones de navegación
        opcionesNavegacion.forEach { destino ->
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = destino.icon,
                        contentDescription = destino.label,
                        tint = if (rutaActual == destino.route) AzulPrimario
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = destino.label,
                        fontWeight = if (rutaActual == destino.route) FontWeight.SemiBold
                                     else FontWeight.Normal
                    )
                },
                selected = rutaActual == destino.route,
                onClick = { onNavegar(destino) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = AzulPrimario.copy(alpha = 0.12f),
                    selectedTextColor = AzulPrimario,
                    selectedIconColor = AzulPrimario
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // Opción de logout al fondo
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = DrawerDestino.Logout.icon,
                    contentDescription = "Cerrar Sesión",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            label = {
                Text(
                    text = "Cerrar Sesión",
                    color = MaterialTheme.colorScheme.error
                )
            },
            selected = false,
            onClick = onLogout,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}
