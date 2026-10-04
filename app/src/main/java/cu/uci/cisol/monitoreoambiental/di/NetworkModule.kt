package cu.uci.cisol.monitoreoambiental.di

import cu.uci.cisol.monitoreoambiental.data.remote.ApiConfig
import cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.http.ContentType
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.dsl.module

val networkModule = module {
    single {
        val sessionRepository: SessionRepository = get()
        val refreshMutex = Mutex()
        HttpClient(Android) {
            // Media uploads and incidence creation are separate requests. A short
            // default timeout makes the complete offline sync fail on slow links.
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 120_000
            }
            install(DefaultRequest) {
                header("x-client-type", "apk")
                header("X-App-Secret", ApiConfig.APP_SECRET)
            }
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
        }.apply {
            plugin(HttpSend).intercept { request ->
                if (!ApiConfig.isAuthPublicPath(request.url.encodedPath)) {
                    var access = sessionRepository.getAccessToken()
                    if (cu.uci.cisol.monitoreoambiental.data.util.JwtUtils.isTokenExpired(access)) {
                        refreshMutex.withLock {
                            // Another request may have refreshed the session while this
                            // request waited for the lock, so read the latest tokens again.
                            access = sessionRepository.getAccessToken()
                            if (cu.uci.cisol.monitoreoambiental.data.util.JwtUtils.isTokenExpired(access)) {
                                val refresh = sessionRepository.getRefreshToken()
                                if (refresh == null) {
                                    sessionRepository.clearSession()
                                    access = null
                                } else {
                                    val refreshClient = HttpClient(Android) {
                                        install(DefaultRequest) {
                                            header("x-client-type", "apk")
                                            header("X-App-Secret", ApiConfig.APP_SECRET)
                                        }
                                        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                                    }
                                    try {
                                        val res = refreshClient.post("${ApiConfig.AUTH_BASE}/token/refresh/") {
                                            contentType(ContentType.Application.Json)
                                            setBody(cu.uci.cisol.monitoreoambiental.data.remote.dto.RefreshRequestDto(refresh))
                                        }
                                        if (res.status.value in 200..299) {
                                            val tokenResponse = res.body<cu.uci.cisol.monitoreoambiental.data.remote.dto.TokenResponseDto>()
                                            sessionRepository.saveTokens(tokenResponse.access, tokenResponse.refresh)
                                            access = tokenResponse.access
                                        } else {
                                            sessionRepository.clearSession()
                                            access = null
                                        }
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        sessionRepository.clearSession()
                                        access = null
                                    } finally {
                                        refreshClient.close()
                                    }
                                }
                            }
                        }
                    }
                    request.headers.remove(HttpHeaders.Authorization)
                    if (!access.isNullOrBlank() &&
                        !cu.uci.cisol.monitoreoambiental.data.util.JwtUtils.isTokenExpired(access)
                    ) {
                        request.headers.append(HttpHeaders.Authorization, "Bearer $access")
                    }
                }
                val call = this.execute(request)
                if (!ApiConfig.isAuthPublicPath(request.url.encodedPath) && call.response.status.value == 401) {
                    sessionRepository.clearSession()
                }
                call
            }
        }
    }

    single { cu.uci.cisol.monitoreoambiental.data.remote.AuthApi(get()) }
    single { cu.uci.cisol.monitoreoambiental.data.remote.UsersApi(get()) }
    single { cu.uci.cisol.monitoreoambiental.data.remote.ManagementApi(get()) }
    single { cu.uci.cisol.monitoreoambiental.data.remote.LocationsApi(get()) }
    single { cu.uci.cisol.monitoreoambiental.data.remote.MultimediaApi(get()) }
}
