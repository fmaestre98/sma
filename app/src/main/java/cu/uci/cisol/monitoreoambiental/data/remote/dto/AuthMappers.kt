package cu.uci.cisol.monitoreoambiental.data.remote.dto

import cu.uci.cisol.monitoreoambiental.domain.model.Session

fun SessionDto.toSession(): Session {
    return Session(
        sessionId = sessionId,
        ipAddress = ipAddress,
        activeTime = activeTime,
        userAgent = userAgent
    )
}
