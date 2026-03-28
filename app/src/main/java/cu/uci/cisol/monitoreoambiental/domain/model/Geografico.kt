package cu.uci.cisol.monitoreoambiental.domain.model

import java.time.LocalDate

val PROVINCIAS = listOf(
    "Pinar del Río", "Artemisa", "La Habana", "Mayabeque", "Matanzas",
    "Villa Clara", "Cienfuegos", "Sancti Spíritus", "Ciego de Ávila",
    "Camagüey", "Las Tunas", "Holguín", "Granma", "Santiago de Cuba",
    "Guantánamo", "Isla de la Juventud"
)

val MUNICIPIOS_POR_PROVINCIA: Map<String, List<String>> = mapOf(
    "La Habana" to listOf(
        "Playa", "Plaza de la Revolución", "Centro Habana", "La Habana Vieja",
        "Regla", "La Habana del Este", "Guanabacoa", "San Miguel del Padrón",
        "Diez de Octubre", "Cerro", "Marianao", "La Lisa", "Boyeros",
        "Arroyo Naranjo", "Cotorro"
    ),
    "Pinar del Río" to listOf(
        "Consolación del Sur", "Guane", "La Palma", "Los Palacios",
        "Mantua", "Minas de Matahambre", "Pinar del Río", "San Juan y Martínez",
        "San Luis", "Sandino", "Viñales"
    ),
    "Artemisa" to listOf(
        "Alquízar", "Artemisa", "Bahía Honda", "Bauta", "Caimito",
        "Candelaria", "Güira de Melena", "Mariel", "San Antonio de los Baños",
        "San Cristóbal", "Soroa"
    ),
    "Matanzas" to listOf(
        "Calimete", "Cárdenas", "Ciénaga de Zapata", "Colón",
        "Jagüey Grande", "Jovellanos", "Limonar", "Los Arabos",
        "Martí", "Matanzas", "Pedro Betancourt", "Perico", "Unión de Reyes"
    )
    // Simplificado para el MVP - se puede ampliar
)
