package cu.uci.cisol.monitoreoambiental.domain.repository

import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Error

sealed interface ProfileError : Error {
    data class Validation(val message: String) : ProfileError
    data class Network(val error: DataError.Network) : ProfileError
}
