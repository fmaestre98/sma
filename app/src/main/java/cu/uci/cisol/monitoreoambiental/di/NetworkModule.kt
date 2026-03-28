package cu.uci.cisol.monitoreoambiental.di

import cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        val sessionRepository: SessionRepository = get()
        HttpClient(Android) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }
            install(ContentNegotiation) {
                json(
                        Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                        }
                )
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        runBlocking {
                            val token = sessionRepository.getToken()
                            if (token != null) {
                                BearerTokens(token, token)
                            } else null
                        }
                    }
                }
            }
        }
    }

    single { cu.uci.cisol.monitoreoambiental.data.remote.AuthApi(get()) }
}
