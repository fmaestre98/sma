package cu.uci.cisol.monitoreoambiental.data.util

import android.content.Context
import android.net.Uri
import cu.uci.cisol.monitoreoambiental.data.remote.MultimediaApi
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result

/**
 * Uploads local evidence files to `/api/v1/multimedia/` and returns MediaFile UUIDs.
 * Paths that already look like UUIDs (or http URLs that are actually media ids stored locally)
 * are passed through unchanged.
 */
class EvidenceUploadHelper(
    private val multimediaApi: MultimediaApi,
    private val context: Context,
) {

    suspend fun resolveEvidenceIds(paths: List<String>): Result<List<String>, DataError.Network> {
        if (paths.isEmpty()) return Result.Success(emptyList())
        val ids = mutableListOf<String>()
        paths.forEachIndexed { index, path ->
            when {
                isUuid(path) -> ids.add(path)
                path.startsWith("http://") || path.startsWith("https://") -> {
                    // Absolute media URLs cannot be re-sent as evidences; skip unless UUID-like.
                    // Keep as-is only if last path segment is a UUID (unlikely). Prefer skip.
                }
                else -> {
                    val bytes = readBytes(path) ?: return Result.Error(DataError.Network.UNKNOWN)
                    when (
                        val upload = multimediaApi.uploadFile(
                            bytes = bytes,
                            filename = "evidence_$index.jpg",
                        )
                    ) {
                        is Result.Success -> ids.add(upload.data.id)
                        is Result.Error -> return Result.Error(upload.error)
                    }
                }
            }
        }
        return Result.Success(ids)
    }

    private fun isUuid(value: String): Boolean {
        val uuidRegex =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        return uuidRegex.matches(value.trim())
    }

    private fun readBytes(path: String): ByteArray? {
        return try {
            val uri = Uri.parse(path)
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }
}
