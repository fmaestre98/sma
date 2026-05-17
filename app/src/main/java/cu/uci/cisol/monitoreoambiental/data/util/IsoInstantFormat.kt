package cu.uci.cisol.monitoreoambiental.data.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** ISO-8601 instant formatting compatible with API 24 (no java.time). */
object IsoInstantFormat {
    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    private val formatOut = utcFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    private val parseFormats =
        listOf(
            utcFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
            utcFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            offsetFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
            offsetFormat("yyyy-MM-dd'T'HH:mm:ssXXX"),
        )

    fun parseToEpochMilli(iso: String): Long? {
        for (parser in parseFormats) {
            try {
                return parser.parse(iso)?.time
            } catch (_: ParseException) {
            }
        }
        return null
    }

    fun formatFromEpochMilli(epochMillis: Long): String = formatOut.format(Date(epochMillis))

    private fun utcFormat(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = UTC }

    private fun offsetFormat(pattern: String): SimpleDateFormat = SimpleDateFormat(pattern, Locale.US)
}
