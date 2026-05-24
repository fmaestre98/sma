package cu.uci.cisol.monitoreoambiental.ui.incidencia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A small colored chip displaying a status or criticality label.
 *
 * @param nombre The text to show inside the chip.
 * @param colorHex A hex color string (e.g. "#FF9800") used as background tint.
 */
@Composable
fun EstadoChip(
    nombre: String,
    colorHex: String,
    modifier: Modifier = Modifier,
) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        Color(0xFF9E9E9E)
    }

    Text(
        text = nombre,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
