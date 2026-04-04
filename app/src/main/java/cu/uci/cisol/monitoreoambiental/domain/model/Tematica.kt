package cu.uci.cisol.monitoreoambiental.domain.model

enum class Tematica(val label: String) {
    ZONA_PLAYA("Zona de Playa"),
    DESECHOS_SOLIDOS("Recogida de Desechos Sólidos"),
    ALUMBRADO_PUBLICO("Alumbrado Público"),
    ALCANTARILLADO("Alcantarillado"),
    SALIDEROS_AGUA("Salideros de Agua"),
    ESCOMBROS("Recogida de Escombros"),
    HIGIENE("Higiene"),
    FOCOS_CONTAMINANTES("Focos Contaminantes (Insectos y Roedores)"),
    PARQUES("Parques"),
    ARBOLES("Árboles"),
    PARQUEOS("Parqueos"),
    BACHES("Baches y Viales"),
    TRANSITO("Tránsito y Señalización"),
    ORDEN_PUBLICO("Orden Público"),
    OTROS("Otros"),
}
