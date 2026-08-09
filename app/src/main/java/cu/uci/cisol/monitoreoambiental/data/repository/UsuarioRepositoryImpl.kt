import android.content.Context
import android.net.Uri
import cu.uci.cisol.monitoreoambiental.data.local.db.UsuarioDao
import cu.uci.cisol.monitoreoambiental.data.local.entity.UsuarioEntity
import cu.uci.cisol.monitoreoambiental.domain.model.Usuario
import cu.uci.cisol.monitoreoambiental.domain.repository.UsuarioRepository
import cu.uci.cisol.monitoreoambiental.data.remote.UsersApi
import cu.uci.cisol.monitoreoambiental.data.remote.dto.UpdateUserRequestDto
import cu.uci.cisol.monitoreoambiental.domain.util.DataError
import cu.uci.cisol.monitoreoambiental.domain.util.Result as DomainResult
import cu.uci.cisol.monitoreoambiental.domain.util.map
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
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
                            avatarUrl = usuario.avatarUrl
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
                        avatarUrl = entity.avatarUrl
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
                        avatarUrl = it.avatarUrl
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
                avatarUrl = formatAvatarUrl(dto.avatarUrl)
            )
            guardar(user)
            Unit
        }
    }

    override suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        phone: String,
        email: String,
        avatarUri: String?
    ): DomainResult<Unit, DataError.Network> {
        val currentUser = obtener() ?: return DomainResult.Error(DataError.Network.UNKNOWN)

        val result = if (avatarUri != null) {
            val parts = formData {
                firstName?.let { append("first_name", it) }
                lastName?.let { append("last_name", it) }
                append("phone", phone)
                append("email", email)

                try {
                    val uri = Uri.parse(avatarUri)
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val extension = when {
                        mimeType.contains("png", ignoreCase = true) -> "png"
                        mimeType.contains("webp", ignoreCase = true) -> "webp"
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
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                email = email
            )
            usersApi.updateUser(currentUser.id, request)
        }

        return result.map { response ->
            val dto = response.user
            val user = Usuario(
                id = dto.id,
                email = dto.email,
                userName = dto.userName,
                firstName = dto.firstName,
                lastName = dto.lastName,
                phone = dto.phone,
                role = dto.role,
                avatarUrl = formatAvatarUrl(dto.avatarUrl)
            )
            guardar(user)
            Unit
        }
    }
}
