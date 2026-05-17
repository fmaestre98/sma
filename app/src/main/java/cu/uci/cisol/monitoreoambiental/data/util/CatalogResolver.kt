package cu.uci.cisol.monitoreoambiental.data.util

import cu.uci.cisol.monitoreoambiental.data.local.entity.EstadoEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.LocalidadEntity
import cu.uci.cisol.monitoreoambiental.data.local.entity.TematicaEntity

object CatalogResolver {

    fun resolveTematicaId(nombre: String, tematicas: List<TematicaEntity>): String? =
        tematicas.find { it.name.equals(nombre.trim(), ignoreCase = true) }?.id

    fun resolveEstadoId(nombre: String, estados: List<EstadoEntity>): String? =
        estados.find { it.name.equals(nombre.trim(), ignoreCase = true) }?.id

    fun resolveLocalidadId(nombre: String, localidades: List<LocalidadEntity>): String? =
        localidades.find { it.name.equals(nombre.trim(), ignoreCase = true) }?.id
}
