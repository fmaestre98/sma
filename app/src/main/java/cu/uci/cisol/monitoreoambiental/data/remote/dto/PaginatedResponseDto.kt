package cu.uci.cisol.monitoreoambiental.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponseDto<T>(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T> = emptyList(),
)
