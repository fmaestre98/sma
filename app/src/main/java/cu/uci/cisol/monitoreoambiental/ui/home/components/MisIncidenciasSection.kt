package cu.uci.cisol.monitoreoambiental.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cu.uci.cisol.monitoreoambiental.ui.home.EstadoCount
import cu.uci.cisol.monitoreoambiental.ui.home.HomeFiltro
import cu.uci.cisol.monitoreoambiental.ui.home.MesDisponible
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulPrimario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisIncidenciasFilters(
    selectedFiltro: String,
    selectedMes: MesDisponible?,
    selectedEstadoDropdown: String?,
    mesesDisponibles: List<MesDisponible>,
    estados: List<EstadoCount>,
    onTodasClick: () -> Unit,
    onMesChange: (MesDisponible?) -> Unit,
    onEstadoChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mesExpanded by remember { mutableStateOf(false) }
    var estadoExpanded by remember { mutableStateOf(false) }

    val mesLabel = selectedMes?.label ?: "Mes"
    val estadoLabel = estados.find { it.id == selectedEstadoDropdown }?.nombre ?: "Estado"
    val todasSelected = selectedFiltro == HomeFiltro.TODAS && selectedEstadoDropdown == null

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Mis incidencias",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AzulPrimario
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .clickable(onClick = onTodasClick)
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = "Todas las incidencias",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (todasSelected) FontWeight.Bold else FontWeight.Normal,
                        color = AzulPrimario
                    )
                )
                if (todasSelected) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(100.dp)
                            .height(2.dp)
                            .background(AzulPrimario, RoundedCornerShape(1.dp))
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = mesExpanded,
                onExpandedChange = { mesExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = mesLabel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mesExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedBorderColor = AzulPrimario,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    textStyle = MaterialTheme.typography.bodySmall,
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = mesExpanded,
                    onDismissRequest = { mesExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todos") },
                        onClick = {
                            onMesChange(null)
                            mesExpanded = false
                        }
                    )
                    mesesDisponibles.forEach { mes ->
                        DropdownMenuItem(
                            text = { Text(mes.label) },
                            onClick = {
                                onMesChange(mes)
                                mesExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = estadoExpanded,
                onExpandedChange = { estadoExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = estadoLabel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = estadoExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedBorderColor = AzulPrimario,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    textStyle = MaterialTheme.typography.bodySmall,
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = estadoExpanded,
                    onDismissRequest = { estadoExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todos") },
                        onClick = {
                            onEstadoChange(null)
                            estadoExpanded = false
                        }
                    )
                    estados.forEach { estado ->
                        DropdownMenuItem(
                            text = { Text(estado.nombre) },
                            onClick = {
                                onEstadoChange(estado.id)
                                estadoExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
