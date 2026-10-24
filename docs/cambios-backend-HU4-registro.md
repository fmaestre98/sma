# Cambios necesarios en backend para HU 4: Crear usuario

**Alcance:** contrato y persistencia del endpoint OTP de registro (`POST /api/v1/auth/register/`). Este documento describe el trabajo backend pendiente; no modifica el repositorio backend.

## Contrato enviado por Android

El formulario APK envía JSON con estas keys:

| Key | Tipo | Regla de HU | Uso esperado |
|---|---|---|---|
| `email` | string | Obligatorio, email válido, máximo 255 | Ya se consume y comprueba unicidad en el servicio OTP. |
| `user_name` | string | Obligatorio, 8–15 caracteres | Ya se consume y comprueba unicidad en el servicio OTP. |
| `first_name` | string | Opcional, letras y espacios | Ya se almacena; el servicio normaliza vacío a `""`. |
| `last_name` | string | Opcional, letras y espacios | Ya se almacena; el servicio normaliza vacío a `""`. |
| `country` | string | Obligatorio, código ISO 3166-1 alpha-2 | **Nuevo:** guardar el país seleccionado (por ejemplo, `CU`). |
| `ci` | string o null | Para `country == "CU"`: exactamente 11 dígitos y único | **Nuevo:** guardar y validar para Cuba; Android omite/nullifica cuando el país no es Cuba. Se envía como string para preservar ceros iniciales. |
| `passport` | string o null | Para `country != "CU"`: obligatorio, máximo 9 caracteres y único | **Nuevo:** guardar y validar para países distintos de Cuba; Android omite/nullifica para Cuba. |
| `phone` | string o null | Opcional, solo dígitos, máximo 15 | El servicio OTP ya lo lee y lo persiste; Android lo manda solo si fue informado. |
| `password` | string | Obligatorio, mínimo 12; mayúscula, minúscula, dígito y carácter especial | Ya se valida con `validate_password`; confirmar que la configuración del backend impone toda la política HU. |
| `password2` | string | Obligatorio e idéntico a `password` | Ya se lee si está presente, pero `OtpService.register` permite omitirlo; debe hacerse obligatorio en el contrato backend. |

`country` usa el código ISO de dos letras, aunque la interfaz muestre el nombre localizado. La historia enumera CI y pasaporte como obligatorios a la vez, pero sus escenarios y observación 4 exigen mostrar uno solo según el país; el APK sigue ese flujo condicional.

## Estado comprobado en backend

- `RegisterView` pasa `request.data` directamente a `OtpService.register`; el endpoint no instancia `UserRegistrationSerializer`.
- `OtpService.register` lee email, usuario, nombre, apellidos, contraseña, confirmación y teléfono. Comprueba duplicados de email y usuario, valida la contraseña según los validadores Django configurados y crea la cuenta inactiva pendiente de OTP.
- Como la vista no usa un serializer de entrada, no se aplican automáticamente validación de formato/longitud de email, límites de usuario (8–15), caracteres válidos de nombres ni formato de teléfono. La unicidad de email/usuario sí se comprueba en el servicio.
- Las keys desconocidas no provocan un rechazo explícito, pero `country`, `ci` y `passport` hoy se ignoran. Una respuesta 201 no significa que estos datos hayan sido guardados.
- `UserAccount` no define columnas para país, CI ni pasaporte. El serializer `UserRegistrationSerializer` tampoco los declara y exige nombre/apellidos, aunque la HU los hace opcionales; además, no es el serializer usado por el endpoint OTP actual.

## Trabajo backend requerido

1. Añadir campos persistentes a `UserAccount`: `country` (código de dos letras), `ci` (nullable, longitud 11) y `passport` (nullable, longitud máxima 9). Crear y aplicar la migración correspondiente. Mantener el identificador como texto, no como número.
2. Validar en `OtpService.register` (o en un serializer de entrada usado explícitamente por `RegisterView`) el contrato completo: email con formato/máximo 255, usuario con longitud 8–15, nombre/apellidos solo letras y espacios si vienen, teléfono solo dígitos/máximo 15, país con código ISO admitido y exactamente uno de `ci`/`passport` según el país. Rechazar combinaciones incompatibles, ausencias y formatos inválidos con errores asociados a la key respectiva.
3. Comprobar unicidad de CI y pasaporte antes de crear la cuenta, incluyendo registros pendientes de verificación y respetando la política existente de email/usuario sobre cuentas soft-deleted. Añadir restricciones de base de datos que eviten duplicados en solicitudes concurrentes; los valores nulos del identificador inactivo no deben colisionar.
4. Guardar país e identificador en la misma creación transaccional que abre el flujo OTP. Evitar que el endpoint responda éxito si alguno de los datos nuevos no quedó validado/persistido.
5. Hacer `password2` obligatorio en el contrato del endpoint, comparar siempre ambos valores, y mantener la validación de contraseña de HU (12 caracteres, mayúscula, minúscula, número y carácter especial). Confirmar que la política del backend no sea más débil que la de APK.
6. Mantener `first_name`, `last_name` y `phone` opcionales de acuerdo con HU. Si se conserva `UserRegistrationSerializer` como contrato compartido/documentación, declararlos opcionales allí y añadir los nuevos campos; la ruta OTP debe usar una única validación autoritativa para no divergir.
7. Devolver errores de duplicidad con keys `ci` y `passport` y mensajes que la APK pueda presentar como «El CI ya se encuentra registrado» y «El pasaporte ya se encuentra registrado».
8. Añadir pruebas de API para registro por Cuba/no Cuba, required/formatos, campos incompatibles, duplicados (incluyendo concurrencia si la capa de pruebas lo permite), password confirmation obligatoria y persistencia de los nuevos valores antes/después de verificar OTP.

## Archivos backend a revisar al implementar

- `apps/users/models.py` — campos y restricciones de `UserAccount`.
- `apps/users/services.py` — `OtpService.register`, validación, unicidad y creación de usuario.
- `apps/users/serializers.py` y `apps/users/views.py` — contrato de registro; ahora la vista OTP entrega el payload directamente al servicio.
- `apps/users/migrations/` — migración de esquema.
- `apps/users/tests/test_otp_registration.py` — pruebas del flujo OTP y contrato HTTP.

**Condición para cerrar la brecha:** desplegar esos cambios backend y verificar que un registro acepta y conserva `country` más el identificador condicional, y que duplicados de CI/pasaporte producen errores de campo sin crear cuentas ni disparar OTP.

## Dependencias del perfil (HU 5)

El APK también necesita esos datos en el ciclo de perfil. El backend revisado no devuelve `country`, `ci` ni `passport` en `GET /api/v1/users/me/` (`LogguedUserSerializer`), y `UserAccount` no define esos campos. El serializer actual no persiste `ci`/`passport` porque no declara esos campos; DRF puede ignorarlos y responder éxito, por lo que el cliente no debe asumir que quedaron guardados. Para completar el perfil:

- Exponer `country`, `ci` y `passport` como campos de lectura en `LogguedUserSerializer` y en la respuesta de actualización.
- Permitir que el propio usuario complete una identificación vacía: CI para Cuba o pasaporte para otro país. Una identificación ya establecida debe quedar de solo lectura para el usuario. Validar formato y unicidad en serializer/servicio y base de datos, sin abrir edición de identidad para otras cuentas a roles normales.
- Mantener `email` como solo lectura para el usuario autenticado; `user_name` sí debe ser editable con las reglas HU (8–15 y unicidad). Hoy el serializer backend usa 5–20 caracteres; alinear el máximo/mínimo con HU evita diferencias entre clientes.
- Mantener país de cuenta autorregistrada en solo lectura. Las cuentas creadas por administrador necesitan país para determinar qué campo de identificación mostrar; hoy la respuesta no lo aporta.
- La APK actual envía `user_name`, `first_name`, `last_name`, `phone` y, al completar un vacío, `ci` o `passport`; deja de mandar `email`. Si hay foto, esos mismos datos viajan multipart.

La HU 6 sí cuenta con soporte de backend: `UserAccountSerializer` acepta `current_password`, `new_password`, `confirm_password`; valida contraseña actual, coincidencia y la política configurada por `validate_password`. `UserService.update` audita el cambio y termina otras sesiones, manteniendo la actual. Revisar que la configuración de validadores Django exija explícitamente mínimo 12 caracteres, mayúscula, minúscula, dígito y carácter especial, ya que el serializer usa validadores configurables.
