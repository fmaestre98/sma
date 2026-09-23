package cu.uci.cisol.monitoreoambiental.di

import cu.uci.cisol.monitoreoambiental.data.remote.ApiConfig
import cu.uci.cisol.monitoreoambiental.domain.repository.SessionRepository
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.http.ContentType
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.encodedPath
import org.koin.dsl.module

val networkModule = module {
    single {
        val sessionRepository: SessionRepository = get()
        HttpClient(Android) {
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
            install(Auth) {
                bearer {
                    loadTokens {
                        runBlocking {
                            val access = sessionRepository.getAccessToken()
                            val refresh = sessionRepository.getRefreshToken()
                            if (access != null && refresh != null) {
                                BearerTokens(access, refresh)
                            } else null
                        }
                    }
                    refreshTokens {
                        val refresh = sessionRepository.getRefreshToken() ?: return@refreshTokens null
                        try {
                            val client = HttpClient(Android) {
                                install(DefaultRequest) {
                                    header("x-client-type", "apk")
                                    header("X-App-Secret", ApiConfig.APP_SECRET)
                                }
                                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                            }
                            val response = client.post("${ApiConfig.AUTH_BASE}/token/refresh/") {
                                contentType(ContentType.Application.Json)
                                setBody(cu.uci.cisol.monitoreoambiental.data.remote.dto.RefreshRequestDto(refresh))
                            }
                            if (response.status.value in 200..299) {
                                val tokenResponse = response.body<cu.uci.cisol.monitoreoambiental.data.remote.dto.TokenResponseDto>()
                                sessionRepository.saveTokens(tokenResponse.access, tokenResponse.refresh)
                                BearerTokens(tokenResponse.access, tokenResponse.refresh)
                            } else {
                                sessionRepository.clearSession()
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    sendWithoutRequest { request ->
                        !ApiConfig.isAuthPublicPath(request.url.encodedPath)
                    }
                }
            }
        }.apply {
            plugin(HttpSend).intercept { request ->
                if (!ApiConfig.isAuthPublicPath(request.url.encodedPath)) {
                    val access = sessionRepository.getAccessToken()
                    if (cu.uci.cisol.monitoreoambiental.data.util.JwtUtils.isTokenExpired(access)) {
                        val refresh = sessionRepository.getRefreshToken()
                        if (refresh != null) {
                            try {
                                val refreshClient = HttpClient(Android) {
                                    install(DefaultRequest) {
                                        header("x-client-type", "apk")
                                        header("X-App-Secret", ApiConfig.APP_SECRET)
                                    }
                                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                                }
                                val res = refreshClient.post("${ApiConfig.AUTH_BASE}/token/refresh/") {
                                    contentType(ContentType.Application.Json)
                                    setBody(cu.uci.cisol.monitoreoambiental.data.remote.dto.RefreshRequestDto(refresh))
                                }
                                if (res.status.value in 200..299) {
                                    val tokenResponse = res.body<cu.uci.cisol.monitoreoambiental.data.remote.dto.TokenResponseDto>()
                                    sessionRepository.saveTokens(tokenResponse.access, tokenResponse.refresh)
                                    request.headers.remove(io.ktor.http.HttpHeaders.Authorization)
                                    request.headers.append(
                                        io.ktor.http.HttpHeaders.Authorization,
                                        "Bearer ${tokenResponse.access}",
                                    )
                                } else {
                                    sessionRepository.clearSession()
                                }
                            } catch (e: Exception) {
                                // Ignore, let the original request proceed and fail
                            }
                        }
                    }
                }
                this.execute(request)
            }
        }
    }

    single { cu.uci.cisol.monitoreoambiental.data.remote.AuthApi(get()) }
    single { cu.uci.cisol.monitoreoambiental.data.remote.UsersApi(get()) }
    single { cu.uci.cisol.monitoreoambiental.data.remote.ManagementApi(get()) }
    single { cu.uci.cisol.monitoreoambiental.data.remote.LocationsApi(get()) }
    single { cu.uci.cisol.monitoreoambiental.data.remote.MultimediaApi(get()) }
}
