package cu.uci.cisol.monitoreoambiental.data.util

import android.content.Context
import android.net.Uri
import cu.uci.cisol.monitoreoambiental.domain.model.Incidencia
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
object IncidenciaMultipartBuilder {

    fun buildCreateForm(context: Context, incidencia: Incidencia): List<PartData> {
        val ocurredAt = IsoInstantFormat.formatFromEpochMilli(incidencia.fechaHora)

        return formData {
            append("theme", incidencia.tematicaId)
            append("locality", requireNotNull(incidencia.localidadId) { "localidadId requerida" })
            append("description", incidencia.descripcion)
            incidencia.latitud?.let { append("latitude", it.toString().take(10)) }
            incidencia.longitud?.let { append("longitude", it.toString().take(10)) }
            append("ocurred_at", ocurredAt)

            incidencia.imagenesPaths.forEachIndexed { index, path ->
                val bytes = readBytes(context, path) ?: return@forEachIndexed
                append(
                    key = "evidences",
                    value = bytes,
                    headers = Headers.build {
                        append(
                            HttpHeaders.ContentDisposition,
                            "form-data; name=\"evidences\"; filename=\"evidence_$index.jpg\"",
                        )
                        append(HttpHeaders.ContentType, "image/jpeg")
                    },
                )
            }
        }
    }

    private fun readBytes(context: Context, path: String): ByteArray? {
        return try {
            val uri = Uri.parse(path)
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }
}
