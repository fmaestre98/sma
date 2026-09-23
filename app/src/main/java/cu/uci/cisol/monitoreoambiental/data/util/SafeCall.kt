package cu.uci.cisol.monitoreoambiental.data.util

import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.content.PartData
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.nio.channels.UnresolvedAddressException

suspend inline fun <reified Response : Any> HttpClient.getSafe(
    route: String,
    queryParameters: Map<String, Any?> = mapOf()
): Result<Response, DataError.Network> {
    return safeCall {
        get {
            url(route)
            queryParameters.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }
}

suspend inline fun <reified Request, reified Response : Any> HttpClient.postSafe(
    route: String,
    body: Request
): Result<Response, DataError.Network> {
    return safeCall {
        post {
            url(route)
            setBody(body)
            contentType(ContentType.Application.Json)
        }
    }
}

suspend inline fun <reified Request, reified Response : Any> HttpClient.patchSafe(
    route: String,
    body: Request
): Result<Response, DataError.Network> {
    return safeCall {
        patch {
            url(route)
            setBody(body)
            contentType(ContentType.Application.Json)
        }
    }
}

suspend inline fun <reified Response : Any> HttpClient.postMultipartSafe(
    route: String,
    crossinline formData: suspend () -> List<PartData>,
): Result<Response, DataError.Network> {
    return safeCall {
        post {
            url(route)
            setBody(MultiPartFormDataContent(formData()))
        }
    }
}

suspend inline fun <reified Response : Any> HttpClient.patchMultipartSafe(
    route: String,
    crossinline formData: suspend () -> List<PartData>,
): Result<Response, DataError.Network> {
    return safeCall {
        patch {
            url(route)
            setBody(MultiPartFormDataContent(formData()))
        }
    }
}

suspend inline fun <reified Response : Any> HttpClient.deleteSafe(
    route: String,
    queryParameters: Map<String, Any?> = mapOf()
): Result<Response, DataError.Network> {
    return safeCall {
        delete {
            url(route)
            queryParameters.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }
}

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, DataError.Network> {
    var attempt = 0
    while (true) {
        val response = try {
            execute()
        } catch (e: UnresolvedAddressException) {
            e.printStackTrace()
            return Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: java.net.UnknownHostException) {
            e.printStackTrace()
            return Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: java.net.ConnectException) {
            e.printStackTrace()
            return Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: java.net.SocketTimeoutException) {
            e.printStackTrace()
            return Result.Error(DataError.Network.REQUEST_TIMEOUT)
        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
            e.printStackTrace()
            return Result.Error(DataError.Network.REQUEST_TIMEOUT)
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            return Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: SerializationException) {
            e.printStackTrace()
            return Result.Error(DataError.Network.SERIALIZATION)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            return Result.Error(DataError.Network.UNKNOWN)
        }

        val result = responseToResult<T>(response)
        if (result is Result.Success) return result
        if (result is Result.Error &&
            result.error == DataError.Network.TOO_MANY_REQUESTS &&
            attempt < 2
        ) {
            kotlinx.coroutines.delay(400L * (attempt + 1))
            attempt++
        } else {
            return result
        }
    }
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse
): Result<T, DataError.Network> {
    return when (response.status.value) {
        in 200..299 -> {
            try {
                if (T::class == Unit::class) {
                    Result.Success(Unit as T)
                } else {
                    Result.Success(response.body<T>())
                }
            } catch (e: Exception) {
                Result.Error(DataError.Network.SERIALIZATION)
            }
        }
        401 -> Result.Error(DataError.Network.UNAUTHORIZED)
        408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
        409 -> Result.Error(DataError.Network.CONFLICT)
        413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
        429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
        in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
        else -> Result.Error(DataError.Network.UNKNOWN)
    }
}
