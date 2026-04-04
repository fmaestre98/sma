package cu.uci.cisol.monitoreoambiental.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.uci.cisol.monitoreoambiental.domain.model.EstadoIncidencia
import cu.uci.cisol.monitoreoambiental.ui.home.MonthlyData
import cu.uci.cisol.monitoreoambiental.ui.theme.ColorNueva
import cu.uci.cisol.monitoreoambiental.ui.theme.ColorAceptada
import cu.uci.cisol.monitoreoambiental.ui.theme.ColorRechazada

// Estados a mostrar en el gráfico de barras (los más relevantes para seguimiento)
private val ESTADOS_BARRA = listOf(
    EstadoIncidencia.NUEVA to ColorNueva,
    EstadoIncidencia.ACEPTADA to ColorAceptada,
    EstadoIncidencia.RECHAZADA to ColorRechazada
)

@Composable
fun BarChart(
    datos: List<MonthlyData>,
    modifier: Modifier = Modifier
) {
    if (datos.isEmpty()) return

    val maxValor = datos.maxOf { mes ->
        ESTADOS_BARRA.sumOf { (estado, _) -> mes.counts.getOrDefault(estado, 0) }
    }.takeIf { it > 0 } ?: 1

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(datos) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 8.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val availableHeight = chartHeight - 24.dp.toPx()
            val slotWidth = chartWidth / datos.size
            val barGroupWidth = slotWidth * 0.8f
            val barWidth = barGroupWidth / ESTADOS_BARRA.size
            val cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

            // Líneas guía horizontales
            val guideCount = 4
            for (i in 0..guideCount) {
                val y = availableHeight - (availableHeight * i / guideCount)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Barras
            datos.forEachIndexed { index, mes ->
                val slotLeft = index * slotWidth
                val groupLeft = slotLeft + (slotWidth - barGroupWidth) / 2f

                ESTADOS_BARRA.forEachIndexed { barIndex, (estado, color) ->
                    val valor = mes.counts.getOrDefault(estado, 0)
                    val barHeight = (valor.toFloat() / maxValor) * availableHeight * animProgress.value
                    val barLeft = groupLeft + barIndex * barWidth
                    val barTop = availableHeight - barHeight

                    if (barHeight > 0f) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth - 1.dp.toPx(), barHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }

                // Etiqueta del mes
                drawContext.canvas.nativeCanvas.drawText(
                    mes.mes,
                    slotLeft + slotWidth / 2f,
                    chartHeight,
                    android.graphics.Paint().apply {
                        textSize = 11.sp.toPx()
                        color = android.graphics.Color.GRAY
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Leyenda del gráfico de barras
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ESTADOS_BARRA.forEach { (estado, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.width(12.dp).height(12.dp)) {
                        drawRoundRect(
                            color = color,
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = estado.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}
