import android.content.Context
import android.net.Uri
import cu.uci.cisol.monitoreoambiental.data.local.db.UsuarioDao
import cu.uci.cisol.monitoreoambiental.data.local.entity.UsuarioEntity
import cu.uci.cisol.monitoreoambiental.domain.model.Usuario
import cu.uci.cisol.monitoreoambiental.domain.repository.UsuarioRepository
import cu.uci.cisol.monitoreoambiental.data.remote.UsersApi
import cu.uci.cisol.monitoreoambiental.data.remote.dto.UpdateUserRequestDto
import cu.uci.cisol.monitoreoambiental.data.remote.dto.ChangePasswordRequestDto
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result as DomainResult
import cu.uci.cisol.monitoreoambiental.domain.repository.ProfileError
import cu.uci.cisol.monitoreoambiental.domain.util.map
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import io.ktor.client.call.body
import java.io.IOException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class UsuarioRepositoryImpl(
    private val usuarioDao: UsuarioDao,
    private val usersApi: UsersApi,
    private val context: Context
) : UsuarioRepository {

    override suspend fun guardar(usuario: Usuario): kotlin.Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val entity =
                    UsuarioEntity(
                            id = usuario.id,
                            email = usuario.email,
                            userName = usuario.userName,
                            firstName = usuario.firstName,
                            lastName = usuario.lastName,
                            phone = usuario.phone,
                            role = usuario.role,
                            avatarUrl = usuario.avatarUrl,
                            country = usuario.country,
                            ci = usuario.ci,
                            passport = usuario.passport,
                    )
            usuarioDao.guardar(entity)
        }
    }

    override suspend fun obtener(): Usuario? {
        return withContext(Dispatchers.IO) {
            usuarioDao.obtener()?.let { entity ->
                Usuario(
                        id = entity.id,
                        email = entity.email,
                        userName = entity.userName,
                        firstName = entity.firstName,
                        lastName = entity.lastName,
                        phone = entity.phone,
                        role = entity.role,
                        avatarUrl = entity.avatarUrl,
                        country = entity.country,
                        ci = entity.ci,
                        passport = entity.passport,
                )
            }
        }
    }

    override fun obtenerFlow(): Flow<Usuario?> {
        return usuarioDao.obtenerFlow().map { entity ->
            entity?.let {
                Usuario(
                        id = it.id,
                        email = it.email,
                        userName = it.userName,
                        firstName = it.firstName,
                        lastName = it.lastName,
                        phone = it.phone,
                        role = it.role,
                        avatarUrl = it.avatarUrl,
                        country = it.country,
                        ci = it.ci,
                        passport = it.passport,
                )
            }
        }
    }

    override suspend fun eliminar(): kotlin.Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { usuarioDao.eliminar() }
    }

    private fun formatAvatarUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            val path = if (url.startsWith("/")) url else "/$url"
            "${cu.uci.cisol.monitoreoambiental.data.remote.ApiConfig.BASE_URL}$path"
        }
    }

    override suspend fun refreshProfile(): DomainResult<Unit, DataError.Network> {
        return usersApi.getMe().map { response ->
            val dto = response.user
            val user = Usuario(
                id = dto.id,
                email = dto.email,
                userName = dto.userName,
                firstName = dto.firstName,
                lastName = dto.lastName,
                phone = dto.phone,
                role = dto.role,
                avatarUrl = formatAvatarUrl(dto.avatarUrl),
                country = dto.country,
                ci = dto.ci,
                passport = dto.passport,
            )
            guardar(user)
            Unit
        }
    }

    override suspend fun updateProfile(
        userName: String,
        firstName: String?,
        lastName: String?,
        phone: String,
        ci: String?,
        passport: String?,
        avatarUri: String?
    ): DomainResult<Unit, ProfileError> {
        val currentUser = obtener()
            ?: return DomainResult.Error(ProfileError.Network(DataError.Network.UNKNOWN))

        return try {
            val response = if (avatarUri != null) {
                val parts = formData {
                firstName?.let { append("first_name", it) }
                lastName?.let { append("last_name", it) }
                append("user_name", userName)
                append("phone", phone)
                ci?.let { append("ci", it) }
                passport?.let { append("passport", it) }

                try {
                    val uri = Uri.parse(avatarUri)
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val extension = when {
                        mimeType.contains("png", ignoreCase = true) -> "png"
                        mimeType.contains("webp", ignoreCase = true) -> "webp"
                        mimeType.contains("svg", ignoreCase = true) -> "svg"
                        else -> "jpg"
                    }
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        append(
                            key = "avatar",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"avatar.$extension\"")
                                append(HttpHeaders.ContentType, mimeType)
                            }
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                }
                usersApi.updateUserMultipart(currentUser.id, parts)
            } else {
                val request = UpdateUserRequestDto(
                    userName = userName,
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone,
                    ci = ci,
                    passport = passport,
                )
                usersApi.updateUser(currentUser.id, request)
            }
            if (response.status.value !in 200..299) {
                if (response.status.value == 400) {
                    val errors = runCatching {
                        Json.parseToJsonElement(response.bodyAsText()) as? JsonObject
                    }.getOrNull()
                    val message = when {
                        errors?.containsKey("user_name") == true -> "El nombre de usuario ya se encuentra registrado o no es válido."
                        errors?.containsKey("ci") == true -> "El CI ya se encuentra registrado o no es válido."
                        errors?.containsKey("passport") == true -> "El pasaporte ya se encuentra registrado o no es válido."
                        errors?.containsKey("phone") == true -> "El teléfono solo admite dígitos y un máximo de 15."
                        else -> "No se pudieron actualizar los datos del perfil. Revise los campos."
                    }
                    return DomainResult.Error(ProfileError.Validation(message))
                }
                val networkError = when (response.status.value) {
                    401 -> DataError.Network.UNAUTHORIZED
                    403 -> DataError.Network.FORBIDDEN
                    408 -> DataError.Network.REQUEST_TIMEOUT
                    413 -> DataError.Network.PAYLOAD_TOO_LARGE
                    in 500..599 -> DataError.Network.SERVER_ERROR
                    else -> DataError.Network.UNKNOWN
                }
                return DomainResult.Error(ProfileError.Network(networkError))
            }

            val dto = response.body<cu.uci.cisol.monitoreoambiental.data.remote.dto.UserResponseDto>().user
            guardar(
                Usuario(
                    id = dto.id,
                    email = dto.email,
                    userName = dto.userName,
                    firstName = dto.firstName,
                    lastName = dto.lastName,
                    phone = dto.phone,
                    role = dto.role,
                    avatarUrl = formatAvatarUrl(dto.avatarUrl),
                    country = dto.country,
                    ci = dto.ci,
                    passport = dto.passport,
                ),
            )
            if ((ci != null && dto.ci != ci) || (passport != null && dto.passport != passport)) {
                return DomainResult.Error(
                    ProfileError.Validation("El servidor no confirmó el guardado del CI o pasaporte. Contacte al administrador."),
                )
            }
            DomainResult.Success(Unit)
        } catch (error: UnresolvedAddressException) {
            DomainResult.Error(ProfileError.Network(DataError.Network.NO_INTERNET))
        } catch (error: UnknownHostException) {
            DomainResult.Error(ProfileError.Network(DataError.Network.NO_INTERNET))
        } catch (error: IOException) {
            DomainResult.Error(ProfileError.Network(DataError.Network.NO_INTERNET))
        } catch (error: Exception) {
            DomainResult.Error(ProfileError.Network(DataError.Network.UNKNOWN))
        }
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmation: String,
    ): DomainResult<Unit, ProfileError> {
        val currentUser = obtener()
            ?: return DomainResult.Error(ProfileError.Network(DataError.Network.UNKNOWN))
        return try {
            val response = usersApi.changePassword(
                currentUser.id,
                ChangePasswordRequestDto(currentPassword, newPassword, confirmation),
            )
            if (response.status.value in 200..299) {
                DomainResult.Success(Unit)
            } else if (response.status.value == 400) {
                val payload = response.bodyAsText()
                val errors = runCatching { Json.parseToJsonElement(payload) as? JsonObject }.getOrNull()
                val message = when {
                    errors?.containsKey("current_password") == true -> "La contraseña actual es incorrecta."
                    errors?.containsKey("confirm_password") == true -> "Las contraseñas no coinciden."
                    errors?.containsKey("new_password") == true -> "La nueva contraseña no cumple la política de seguridad."
                    else -> "No se pudo actualizar la contraseña. Revise los datos e inténtelo de nuevo."
                }
                DomainResult.Error(ProfileError.Validation(message))
            } else {
                val networkError = when (response.status.value) {
                    401 -> DataError.Network.UNAUTHORIZED
                    403 -> DataError.Network.FORBIDDEN
                    408 -> DataError.Network.REQUEST_TIMEOUT
                    in 500..599 -> DataError.Network.SERVER_ERROR
                    else -> DataError.Network.UNKNOWN
                }
                DomainResult.Error(ProfileError.Network(networkError))
            }
        } catch (error: UnresolvedAddressException) {
            DomainResult.Error(ProfileError.Network(DataError.Network.NO_INTERNET))
        } catch (error: UnknownHostException) {
            DomainResult.Error(ProfileError.Network(DataError.Network.NO_INTERNET))
        } catch (error: IOException) {
            DomainResult.Error(ProfileError.Network(DataError.Network.NO_INTERNET))
        } catch (error: Exception) {
            DomainResult.Error(ProfileError.Network(DataError.Network.UNKNOWN))
        }
    }
}
