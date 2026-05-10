package cu.uci.cisol.monitoreoambiental.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.uci.cisol.monitoreoambiental.ui.home.EstadoCount
import cu.uci.cisol.monitoreoambiental.ui.theme.AzulPrimario
fun colorFromHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}

@Composable
fun DonutChart(
    conteos: List<EstadoCount>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 28.dp
) {
    val total = conteos.sumOf { it.count }.takeIf { it > 0 } ?: 1
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(conteos) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size)) {
                val canvasSize = this.size
                val radius = canvasSize.minDimension / 2f
                val stroke = strokeWidth.toPx()
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val arcSize = Size(
                    canvasSize.width - stroke,
                    canvasSize.height - stroke
                )

                // Fondo del donut
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )

                var startAngle = -90f
                conteos.forEach { estadoCount ->
                    val sweep = (estadoCount.count.toFloat() / total) * 360f * animProgress.value
                    drawArc(
                        color = colorFromHex(estadoCount.colorHex),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            // Texto central
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AzulPrimario
                    )
                )
                Text(
                    text = "Incidencias\nTotales",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray,
                        fontSize = 11.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Leyenda en grid 2 columnas
        val chunks = conteos.chunked(2)
        chunks.forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pair.forEach { estadoCount ->
                    LeyendaItem(
                        color = colorFromHex(estadoCount.colorHex),
                        label = estadoCount.nombre,
                        count = estadoCount.count,
                        total = total,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Rellena si el par tiene solo 1 elemento
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun LeyendaItem(
    color: Color,
    label: String,
    count: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val porcentaje = if (total > 0) (count * 100f / total).toInt() else 0
    Row(
        modifier = modifier.wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.DarkGray),
                maxLines = 1
            )
            Text(
                text = "$count ($porcentaje%)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
