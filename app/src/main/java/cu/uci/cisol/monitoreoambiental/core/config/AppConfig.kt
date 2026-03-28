package cu.uci.cisol.monitoreoambiental.core.config

object AppConfig {
    /**
     * Flag to enable/disable mock authentication for local development and testing. When true,
     * [cu.uci.cisol.monitoreoambiental.domain.repository.AuthRepository] will use
     * [cu.uci.cisol.monitoreoambiental.data.repository.MockAuthRepositoryImpl].
     */
    const val USE_MOCK_AUTH = true
}
