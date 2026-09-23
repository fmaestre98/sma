package cu.uci.cisol.monitoreoambiental.data.util

import cu.uci.cisol.monitoreoambiental.data.local.entity.EstadoEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.LocalidadEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.TematicaEntity

object CatalogResolver {

    fun resolveTematicaId(value: String, tematicas: List<TematicaEntity>): String? {
        val trimmed = value.trim()
        return tematicas.find { it.id == trimmed }?.id
            ?: tematicas.find { it.name.equals(trimmed, ignoreCase = true) }?.id
    }

    fun resolveEstadoId(value: String, estados: List<EstadoEntity>): String? {
        val trimmed = value.trim()
        return estados.find { it.id == trimmed }?.id
            ?: estados.find { it.name.equals(trimmed, ignoreCase = true) }?.id
    }

    fun resolveLocalidadId(value: String?, localidades: List<LocalidadEntity>): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        return localidades.find { it.id == trimmed }?.id
            ?: localidades.find { it.name.equals(trimmed, ignoreCase = true) }?.id
    }
}
