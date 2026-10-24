# Comparación de historias de usuario con la APK Android

**Documento de referencia:** `HU APK SMA 13-10.pdf`, versión 1.2, fechada 13/10/2026.  
**Código revisado:** proyecto Android `MonitoreoAmbiental`, árbol de trabajo al 24/10/2026; HU 12, HU 15 y HU 16 actualizadas tras los cambios Android.
**Alcance:** experiencia Android, pantallas, navegación, estado local, validaciones y puntos donde la APK depende de servicios externos. No se evalúa la implementación del backend ni se afirma que una llamada remota funcione en producción.

## Resumen

La APK ya tiene una base funcional extensa para reportar y consultar incidencias: autenticación, alta básica de cuenta, perfil básico, filtros y búsqueda, detalle, borradores, cola offline, sincronización en segundo plano, guía local y apariencia remota cacheada.

Las brechas Android de mayor impacto identificadas en código son:

1. **HU 15 Georreferenciar incidencia:** se conectó el botón GPS al proveedor fused de Google Play Services; falta validar el comportamiento en un dispositivo y completar casos de GPS desactivado/permiso denegado.
2. **HU 4 Crear usuario:** Android incorpora país, CI/pasaporte condicional, confirmación de contraseña y validaciones locales. La persistencia del país e identificador queda pendiente en el backend; ver [`cambios-backend-HU4-registro.md`](cambios-backend-HU4-registro.md).
3. **HU 6 Cambiar contraseña:** no se encontró pantalla ni flujo Android para cambiar la contraseña desde el perfil.
4. **HU 5 Editar perfil:** hay perfil editable, pero los campos y reglas difieren de la historia; por ejemplo, email es editable y no aparecen usuario, país, CI o pasaporte.
5. **HU 1/2 Sesión:** se guardan y limpian tokens y hay resolución de conflictos de sesiones al iniciar sesión; no se encontró expiración por inactividad en la APK.
6. **HU 8 Estadísticas:** existe tablero y contadores locales; no se confirmó en la UI una variante específica para administrador de datos con totales globales.

## Cómo leer la comparación

- **Cubierta:** se encontró flujo Android que corresponde al objetivo principal; la conexión con servidor se considera una dependencia externa.
- **Parcial:** existe pantalla o parte del flujo, pero hay criterios visibles que no están implementados, difieren de la historia o dependen de una integración no verificable desde Android.
- **No localizada:** no se encontró pantalla, ruta ni comportamiento Android que cubra el flujo descrito.

Los nombres de archivo bajo «Evidencia» son rutas relativas al proyecto Android. «Pendiente de backend/integración» identifica criterios que la APK invoca o presenta, pero cuya lógica autoritativa ocurre fuera del alcance de esta revisión.

## Matriz de las 21 historias

| HU | Historia | Estado | Flujo Android encontrado | Brecha principal |
|---:|---|---|---|---|
| 1 | Autenticar usuario | Parcial | Login, validación de vacíos, mostrar/ocultar contraseña, persistencia de tokens y diálogo de conflicto multisesión. | Sin control local de expiración por inactividad. Política de contraseña para login, usuario bloqueado/eliminado, IP/tiempo de sesiones y auditoría son principalmente servidor; no se confirmó su presentación específica en Android. |
| 2 | Cerrar sesión | Parcial | Opción «Cerrar Sesión» al pie del drawer; el `HomeViewModel` cierra sesión y la navegación vuelve a login; se limpian tokens locales. | No se encontró temporizador de inactividad. La invalidación remota del token debe verificarse contra el servicio. |
| 3 | Recuperar contraseña | Parcial, alineada al alcance APK de la HU | «¿Olvidó su contraseña?» abre diálogo con email, valida formato y envía una solicitud; mensaje genérico de confirmación. | La HU indica que el restablecimiento mediante enlace se completa fuera de la APK. El formulario de nueva clave, expiración y reglas pertenecen a ese flujo externo. |
| 4 | Crear usuario | Parcial | Pantalla de registro con usuario, nombre/apellidos opcionales, email, selector de país, CI para Cuba o pasaporte para otro país, teléfono opcional, contraseña y confirmación. Valida campos y políticas localmente; mantiene OTP y control de registro habilitado. | El cliente envía `country`, `ci` o `passport`, pero el servicio OTP actual no los persiste ni valida. La unicidad de CI/pasaporte y el almacenamiento requieren cambios backend. |
| 5 | Editar perfil de usuario | Parcial | Perfil con usuario editable, nombre/apellidos opcionales, email y país en solo lectura, CI/pasaporte condicional, teléfono opcional, avatar desde cámara/galería, validaciones y guardado. La caché Room preserva nuevos campos de identidad con migración 7→8. | El backend actual no devuelve país/CI/pasaporte y su serializer no permite guardar esos campos; por ello el flujo de identidad queda pendiente de cambios backend. |
| 6 | Cambiar contraseña | Parcial, conectado al endpoint existente | Acción desde perfil abre formulario enmascarado para contraseña actual, nueva y confirmación; valida campos, política y diferencia con la actual, permite cancelar y envía `current_password`, `new_password`, `confirm_password` al PATCH de usuario. | El backend ya compara contraseña actual, confirmación, valida contraseña y audita. La APK traduce el error de contraseña actual y necesita verificar la configuración de política del servidor y el flujo en dispositivo. |
| 7 | Gestionar sesiones activas | Parcial | Al detectar múltiples sesiones durante login se muestra resolución de conflicto, permite seleccionar sesiones y solicitar cierre; cancelar limpia la selección. | Solo se cubre al iniciar sesión. No hay gestión desde perfil; presentación de dispositivo/IP/tiempo y fallos individuales dependen de la respuesta remota. |
| 8 | Consultar estadísticas de incidencias | Parcial | Home incluye estadísticas, distribución por estado, meses, borradores y pendientes, usando incidencias observadas localmente. | No se encontró una UI Android diferenciada que demuestre contadores globales del rol administrador de datos. Confirmar alcance por rol y fuente/consistencia de los conteos. |
| 9 | Listar incidencias | Cubierta en lo esencial | Home y «Mis Incidencias» muestran tarjetas; hay paginación incremental, estado, grupo por fecha, acceso a detalle y recuperación de borradores. Solo los elementos marcados como borrador abren el formulario; pendientes locales abren lectura. | La lista principal es propia/local-first. El listado global de administrador y mensajes concretos de error/vacío requieren validar la respuesta remota y permisos por rol. |
| 10 | Filtrar incidencias | Parcial | «Mis Incidencias» ofrece filtros de estado, incluidos borrador y pendientes; Home también mantiene filtros y mes/estado. | No se encontró un rango de fechas completo como el descrito en la HU ni equivalencia comprobada de opciones para administrador. |
| 11 | Buscar incidencias | Parcial | Campo de búsqueda en listado/Home; filtra datos disponibles localmente y puede combinarse con estado. | La historia pide búsqueda sin distinguir acentos; no se observó normalización explícita de diacríticos. Alcance de resultados remotos/globales pendiente de integración. |
| 12 | Mostrar incidencia | Alineada con lectura | Detalle muestra datos remotos/locales, estado, coordenadas y evidencias en miniaturas ampliables. La pantalla no ofrece acciones de edición ni resolución. | Validar carga de URLs de evidencia en dispositivo y el comportamiento sin red; la entrega de URLs y permisos por rol dependen del servicio. |
| 13 | Crear incidencia | Parcial | Formulario con temática, localidad, descripción, dirección, selector de fecha/hora, evidencia y ubicación; catálogos se mantienen localmente y se pueden sincronizar. | Aunque hay selector de fecha/hora, el guardado construye la incidencia con `System.currentTimeMillis()`, ignorando el valor seleccionado. El GPS real está pendiente (HU 15). |
| 14 | Adjuntar foto de evidencia | Parcial | Cámara mediante contrato Android y selección múltiple de imágenes; vista previa y acción de eliminar; fotos se copian a almacenamiento interno. | La APK permite hasta 5 imágenes, mientras la HU describe una foto por incidencia. El borrado es directo sin confirmación. No se confirmó validación de formato/tamaño de 5 MB. |
| 15 | Georreferenciar incidencia | Parcial | Permiso de ubicación, mapa offline Mapsforge con selección de punto y coordenadas visibles; con permiso concedido, el botón solicita la ubicación actual al proveedor fused y guarda latitud/longitud recibidas. | Si el proveedor no devuelve posición o falla, se muestra el error genérico. Queda validar en dispositivo, explicar permisos y cubrir GPS apagado/denegación permanente. |
| 16 | Guardar incidencia como borrador | Parcial | Botón «Guardar Borrador», persistencia local Room, navegación para reabrir formulario desde listado. | No se localizó confirmación completa al salir con cambios sin guardar, ni flujo completo de confirmar/cancelar descarte y borrado como exige la HU. |
| 17 | Enviar incidencia | Parcial | Valida campos, foto y ubicación; guarda localmente, intenta envío inmediato y conserva la incidencia para cola cuando falla o no hay conexión. | El resultado visible puede diferir de los mensajes y estados exactos de aceptación. La recepción, estado inicial y autor final los confirma el servidor. |
| 18 | Sincronizar incidencias pendientes | Parcial | Worker de subida con restricción de red, WorkManager, reintento y actualización local; también hay sincronización de lectura periódica. | No se encontró claramente en la UI el botón «Sincronizar ahora» ni el reporte detallado éxito/parcial/error. Verificar orden, sesión expirada y transición de contadores con el servicio. |
| 19 | Consultar la guía de uso | Cubierta en contenido principal | Drawer «Guía de Uso» abre pantalla local con funcionamiento offline, sincronización, permisos y flujo de creación. | Revisar que las instrucciones sigan reflejando el flujo actual: el texto de ubicación describe obtención automática aunque el GPS actual es simulado. |
| 20 | Solicitar permisos de cámara, galería y ubicación | Parcial | Cámara y ubicación usan permisos runtime contextuales; selección de imágenes emplea Android Photo Picker. | No se encontró diálogo explicativo previo, mensajes diferenciados para denegación permanente ni botón para abrir ajustes. El Photo Picker del sistema puede no requerir permiso de galería; revisar permisos declarados heredados en manifest. |
| 21 | Aplicar apariencia configurada desde el sistema | Parcial | La app sincroniza apariencia al abrirse, observa estado local, aplica color primario y muestra imagen configurada en el drawer; existe worker periódico. | Confirmar aplicación del logotipo/paleta en todas las pantallas, fallback ante error/sin red, contraste 4.5:1 y modo claro fijo. No se revisa la entrega pública del endpoint. |

## Detalle por historia y anclas de código

### HU 1 — Autenticar usuario

**Flujo observado:** al abrir sin token se muestra Login. La pantalla recoge email y contraseña, permite alternar visibilidad y muestra errores. `AuthViewModel` rechaza campos vacíos y delega autenticación; si recibe un error de múltiples sesiones abre la resolución de conflicto. `DataStoreSessionManager` persiste y elimina access/refresh tokens. La navegación reacciona a la sesión local.

**Cobertura:** login, campos vacíos, contraseña visible/oculta, persistencia de sesión y punto de entrada de conflicto están presentes. No se encontró temporizador de inactividad. El estado bloqueado/eliminado, invalidación de sesiones, auditoría, IP y fecha de último acceso son decisiones de servidor; revisar que sus errores se traduzcan a mensajes distintos en el cliente.

**Evidencia:** [`LoginScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/auth/LoginScreen.kt), [`AuthViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/auth/AuthViewModel.kt), [`DataStoreSessionManager.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/local/DataStoreSessionManager.kt), [`MainActivity.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/MainActivity.kt).

### HU 2 — Cerrar sesión

**Flujo observado:** el drawer coloca «Cerrar Sesión» al pie. Home invoca logout; la navegación vuelve a login al completarse. El repositorio de sesión expone `clearSession`, que elimina tokens locales.

**Brecha:** no se encontró control por tiempo sin interacción. La revocación del token en servidor no se puede certificar solo desde el frontend.

**Evidencia:** [`DrawerMenu.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/home/components/DrawerMenu.kt), [`MainActivity.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/MainActivity.kt), [`DataStoreSessionManager.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/local/DataStoreSessionManager.kt).

### HU 3 — Recuperar contraseña

**Flujo observado:** enlace en Login abre un diálogo para introducir email; la APK valida vacío/formato y manda una solicitud. El resultado comunica que, si la cuenta existe, se envió un enlace. Esto coincide con la observación de la HU de que la APK solo inicia la solicitud.

**Límite:** cambio de contraseña por enlace, reglas de contraseña y expiración ocurren fuera de la APK según el documento. Confirmar el enlace y mensajes con el flujo web/backend.

**Evidencia:** [`LoginScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/auth/LoginScreen.kt), [`AuthViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/auth/AuthViewModel.kt).

### HU 4 — Crear usuario

**Flujo implementado en Android:** registro muestra nombre de usuario obligatorio (8–15), nombre y apellidos opcionales (letras/espacios), email, país ISO, CI de 11 dígitos si el país es Cuba o pasaporte (máximo 9) para otros países, teléfono numérico opcional (máximo 15), contraseña con mínimo 12 caracteres y mayúscula, minúscula, número y carácter especial, y confirmación. Valida formato de email, campos obligatorios, CI, coincidencia y contraseña; mantiene disponibilidad condicionada por configuración pública y verificación OTP. El cliente manda `country` como código ISO 3166-1 alpha-2 y las keys `ci` o `passport` según el país.

**Pendiente de backend:** el endpoint OTP recibe el JSON directamente y actualmente solo usa email, usuario, nombre, apellidos, teléfono y contraseñas; ignora `country`, `ci` y `passport`. El modelo `UserAccount` tampoco contiene esos campos. El registro puede responder éxito sin guardar país ni identificador y no puede detectar duplicados de CI/pasaporte. Requiere los cambios descritos en [`cambios-backend-HU4-registro.md`](cambios-backend-HU4-registro.md). Email y usuario duplicados sí tienen comprobación en el servicio actual.

**Evidencia:** [`RegisterScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/auth/RegisterScreen.kt), [`AuthViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/auth/AuthViewModel.kt), [`RegistrationRequest.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/domain/model/RegistrationRequest.kt).

### HU 5 — Editar perfil de usuario

**Flujo Android:** el perfil edita nombre de usuario (8–15), nombre y apellidos opcionales (letras/espacios), teléfono opcional (solo dígitos, máximo 15), CI/pasaporte cuando el dato está vacío y avatar desde cámara/galería. País, correo e identificadores ya informados se muestran de solo lectura. Email ya no forma parte del DTO de actualización. La foto valida JPG/JPEG, PNG, WEBP o SVG hasta 5 MB. Los campos locales del perfil se guardan en Room con migración 7→8.

**Pendiente de backend:** `GET /users/me/` (`LogguedUserSerializer`) no devuelve `country`, `ci` ni `passport`; `UserAccount` no tiene esos campos y el serializer actual no los persiste en `PATCH /users/{id}/` (puede ignorarlos y responder éxito). Android ya representa y manda el contrato esperado, pero esos valores no se pueden leer/guardar hasta extender modelo, migración y serializer. El detalle está en [`cambios-backend-HU4-registro.md`](cambios-backend-HU4-registro.md), sección HU 5.

**Evidencia:** [`ProfileScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/profile/ProfileScreen.kt), [`ProfileViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/profile/ProfileViewModel.kt).

### HU 6 — Cambiar contraseña

**Flujo Android:** desde el perfil se abre un diálogo con contraseña actual, nueva y confirmación, todos enmascarados y con visibilidad opcional. La APK comprueba campos obligatorios, política de 12 caracteres con mayúscula/minúscula/número/carácter especial, confirmación y que la nueva sea distinta de la actual. Guardar envía `current_password`, `new_password` y `confirm_password` al endpoint de actualización del usuario; cancelar limpia los valores y cierra el diálogo.

**Contrato backend comprobado:** `UserAccountSerializer` ya admite estas tres keys, verifica que la contraseña actual sea correcta, compara confirmación, ejecuta `validate_password` y audita el cambio. `UserService.update` conserva la sesión actual y cierra las demás. El flujo todavía requiere verificar con una instancia configurada del servicio y confirmar que sus validadores cubren exactamente la política HU.

**Evidencia revisada:** [`ProfileScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/profile/ProfileScreen.kt), [`ProfileViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/profile/ProfileViewModel.kt), [`UsuarioRepositoryImpl.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/repository/UsuarioRepositoryImpl.kt), [`MainActivity.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/MainActivity.kt).

### HU 7 — Gestionar sesiones activas

**Flujo observado:** el login presenta `ConflictResolutionDialog` cuando el repositorio devuelve sesiones activas. El usuario puede seleccionar y solicitar cierre; si no selecciona ninguna, el ViewModel no envía la acción; cancelar limpia el estado.

**Brecha:** solo está disponible como resolución del login. No se observó una sección de sesiones en el perfil. La tarjeta y sus metadatos dependen del objeto de sesión recibido; fallos de cierre y revocación deben confirmarse en integración.

**Evidencia:** [`LoginScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/auth/LoginScreen.kt), [`AuthViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/auth/AuthViewModel.kt), [`AuthMvi.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/auth/AuthMvi.kt).

### HU 8 — Consultar estadísticas de incidencias

**Flujo observado:** Home calcula y presenta estadísticas/contadores a partir de incidencias, estados y datos locales; distingue borradores y pendientes por sincronizar.

**Brecha:** el código consultado no evidencia una pantalla/resumen separado para el administrador de datos con métricas globales. La autorización y el agregado global son principalmente integración con API; la UI debe confirmar qué roles y estados representa.

**Evidencia:** [`HomeScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/home/HomeScreen.kt), [`HomeViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/home/HomeViewModel.kt), componentes de estadísticas bajo [`ui/home/components`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/home/components/).

### HU 9 — Listar incidencias

**Flujo observado:** Home y Mis Incidencias presentan tarjetas por fecha, estado, búsqueda/filtros, acceso al detalle y acceso al borrador. Mis Incidencias tiene carga de siguiente página.

**Brecha:** verificar contra el APK instalado la experiencia de lista vacía/error, paginación completa y si el rol administrador debe ver todas las incidencias. Los datos y permisos remotos no se validan en este análisis.

**Evidencia:** [`MisIncidenciasScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/MisIncidenciasScreen.kt), [`MisIncidenciasViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/MisIncidenciasViewModel.kt), [`HomeScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/home/HomeScreen.kt).

### HU 10 — Filtrar incidencias

**Flujo observado:** lista e inicio gestionan estado y, en Home, período mensual. La lista conserva filtro mediante argumento de navegación para abrir borradores.

**Brecha:** la HU plantea filtros temporales/rango combinados y opciones por rol. No se identificó un selector de rango de fechas en Mis Incidencias; comprobar el detalle de opciones antes de considerar completa.

**Evidencia:** [`MisIncidenciasScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/MisIncidenciasScreen.kt), [`MisIncidenciasViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/MisIncidenciasViewModel.kt), [`HomeViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/home/HomeViewModel.kt).

### HU 11 — Buscar incidencias

**Flujo observado:** búsqueda en Home/lista y borrado de consulta; filtrado combinado con estado opera sobre los elementos observados.

**Brecha:** el criterio de no distinguir acentos exige eliminar diacríticos antes de comparar; no se encontró normalización explícita. Los resultados disponibles sin conexión reflejan almacenamiento local.

**Evidencia:** [`MisIncidenciasScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/MisIncidenciasScreen.kt), [`MisIncidenciasViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/MisIncidenciasViewModel.kt), [`HomeViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/home/HomeViewModel.kt).

### HU 12 — Mostrar incidencia

**Flujo observado:** navegación abre un detalle de solo lectura para incidencias enviadas y pendientes locales. Muestra sus datos, estado, coordenadas y las URLs de evidencias recibidas al sincronizar el detalle. Las miniaturas se pueden ampliar. «Ver en el mapa» abre el mapa offline de Mapsforge centrado en las coordenadas de la incidencia, con un marcador en el punto. Los borradores abren el formulario de continuación desde la lista.

**Alineación:** se retiraron los controles de edición, geolocalización modificable y resolución de esta pantalla. La carga de las miniaturas depende de que el detalle remoto devuelva `evidences.file_url`; probar URLs con la configuración real del servicio.

**Evidencia:** [`DetalleIncidenciaScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/DetalleIncidenciaScreen.kt), [`DetalleIncidenciaViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/DetalleIncidenciaViewModel.kt), [`MainActivity.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/MainActivity.kt).

### HU 13 — Crear incidencia

**Flujo observado:** formulario con temática, localidad, descripción, dirección, selector de fecha/hora, foto y coordenadas. Los catálogos tienen repositorios offline-first.

**Brecha:** aunque existe selector de fecha/hora, `guardar` persiste `System.currentTimeMillis()` y no `currentState.fechaHora`, por lo que la fecha escogida se pierde. Los criterios de fecha futura y los mensajes exactos también deben revisarse. Catálogos no disponibles/obsoletos y su origen se relacionan con sincronización externa. La brecha de GPS real se detalla en HU 15.

**Evidencia:** [`NuevaIncidenciaScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/NuevaIncidenciaScreen.kt), [`NuevaIncidenciaViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/NuevaIncidenciaViewModel.kt), repositorios `OfflineFirst*Repository.kt` bajo [`data/repository`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/repository/).

### HU 14 — Adjuntar foto de evidencia

**Flujo observado:** captura con cámara, selector múltiple Android, vista previa y eliminación; URI se conserva en almacenamiento interno para el registro local.

**Brechas y decisión funcional necesaria:** la historia en un lugar pide una foto, pero sus observaciones también enumeran WEBP/SVG y 5 MB; la APK admite hasta cinco imágenes por `PickMultipleVisualMedia`. El icono de quitar elimina directamente sin advertencia. Definir el criterio final (una o cinco) y adecuar validación de extensión/tamaño y confirmación de borrado.

**Evidencia:** [`NuevaIncidenciaScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/NuevaIncidenciaScreen.kt), [`NuevaIncidenciaViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/NuevaIncidenciaViewModel.kt), [`Incidencia.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/domain/model/Incidencia.kt).

### HU 15 — Georreferenciar incidencia

**Flujo observado:** el mapa offline permite marcar un punto. El botón GPS comprueba el permiso de ubicación y solicita una lectura de alta precisión a `FusedLocationProviderClient.getCurrentLocation`; si recibe una ubicación, pasa la latitud y longitud al estado de pantalla.

**Pendiente:** si la lectura retorna `null` o falla, el flujo emite `OnCoordenadasError` y presenta el mensaje genérico «Error al obtener ubicación». Probar con GPS desactivado/sin señal y completar mensajes específicos y orientación de permisos según HU 20. La implementación ya no utiliza coordenadas fijas.

**Evidencia:** [`NuevaIncidenciaScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/NuevaIncidenciaScreen.kt), [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml).

### HU 16 — Guardar incidencia como borrador

**Flujo observado:** guardar borrador persiste en Room; el listado lo abre en el formulario y las imágenes locales guardadas se muestran en la vista previa. Solo `esBorrador` permite entrar en el formulario de edición. El modelo distingue borrador y sincronización.

**Brecha:** no se localizó el diálogo exigido al salir con cambios sin guardar, con confirmar guardar/descartar/seguir editando y confirmación de borrado. Comprobar que borrador, foto y coordenadas sobreviven al cierre y están aislados por usuario.

**Evidencia:** [`NuevaIncidenciaScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/NuevaIncidenciaScreen.kt), [`NuevaIncidenciaViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/NuevaIncidenciaViewModel.kt), [`IncidenciaDao.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/local/db/IncidenciaDao.kt), [`MainActivity.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/MainActivity.kt).

### HU 17 — Enviar incidencia

**Flujo observado:** antes de guardar, el ViewModel valida temática, descripción, dirección, localidad, evidencia y ubicación; guarda localmente y luego intenta envío. Al fallar el envío inmediato conserva la fila y agenda trabajo de subida.

**Brecha:** el envío conserva la capacidad de reintentar, pero la fecha seleccionada se descarta al construir la incidencia (se guarda la hora actual). Comparar también el texto de resultado y la sesión con aceptación. El estado final «Nueva», autor e imagen en servidor dependen del servicio.

**Evidencia:** [`NuevaIncidenciaViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/NuevaIncidenciaViewModel.kt), [`IncidenciaRepositoryImpl.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/repository/IncidenciaRepositoryImpl.kt), [`EnviarIncidenciasPendientesWorker.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/work/EnviarIncidenciasPendientesWorker.kt).

### HU 18 — Sincronizar incidencias pendientes

**Flujo observado:** WorkManager programa la subida de pendientes con requisito de conexión; los workers usan repositorio y sesión y pueden reintentarse. La APK actualiza el estado local cuando procesa resultados.

**Brecha:** la HU exige «Sincronizar ahora» y mensajes de resultado para éxito, parcial, error, sin red y sesión vencida. No se identificó de forma concluyente un control manual en la UI; confirmar en Home/listado. Orden y respuesta del servicio quedan fuera de esta revisión.

**Evidencia:** [`IncidenciaSyncScheduler.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/work/IncidenciaSyncScheduler.kt), [`EnviarIncidenciasPendientesWorker.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/work/EnviarIncidenciasPendientesWorker.kt), [`HomeViewModel.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/home/HomeViewModel.kt).

### HU 19 — Consultar la guía de uso

**Flujo observado:** «Guía de Uso» figura en el drawer y abre contenido estático embebido, por lo que es visible offline. Están las cuatro secciones indicadas en la HU.

**Brecha:** el texto debe actualizarse para aclarar la ubicación real: hoy la pantalla indica obtención automática, pero el código Android usa coordenadas simuladas.

**Evidencia:** [`DrawerMenu.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/home/components/DrawerMenu.kt), [`InfoAppScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/info/InfoAppScreen.kt), [`MainActivity.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/MainActivity.kt).

### HU 20 — Solicitar permisos de cámara, galería y ubicación

**Flujo observado:** la pantalla solicita permiso runtime de cámara y ubicación cuando se pulsan los controles. La galería emplea el selector visual del sistema Android.

**Brecha:** no se encontró una explicación contextual previa ni una salida específica ante denegación permanente. El manifest declara CAMERA, ubicación y permisos de almacenamiento/imagen; revisar la necesidad de cada permiso según versiones Android, especialmente porque Photo Picker funciona con URI seleccionado sin acceso amplio a galería. No se detectó botón de apertura de ajustes.

**Evidencia:** [`NuevaIncidenciaScreen.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/incidencia/NuevaIncidenciaScreen.kt), [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml).

### HU 21 — Aplicar apariencia configurada desde el sistema

**Flujo observado:** `MainActivity` observa apariencia local y lanza sincronización al crear la app. El tema toma el color primario, y el drawer muestra imagen de fondo; Room guarda el estado local. Existe trabajo periódico de apariencia.

**Brecha:** el alcance visible no demuestra que logotipo/paleta se apliquen a todas las pantallas ni el cálculo de contraste mínimo. Tampoco se verificó el fallback ante primera apertura offline, error de servicio o preferencia de modo claro. El endpoint y el payload son dependencias fuera de esta revisión.

**Evidencia:** [`MainActivity.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/MainActivity.kt), [`AppearanceRepositoryImpl.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/repository/AppearanceRepositoryImpl.kt), [`Theme.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/ui/theme/Theme.kt), [`SincronizarAparienciaWorker.kt`](../app/src/main/java/cu/uci/cisol/monitoreoambiental/data/work/SincronizarAparienciaWorker.kt).

## Próximas comprobaciones recomendadas en Android

1. Sustituir el punto GPS fijo por una lectura real de ubicación y cubrir permisos denegados, GPS apagado y falta de señal.
2. Completar campos y validaciones de registro y perfil contra las reglas que queden aprobadas.
3. Añadir flujo de cambio de contraseña desde Perfil.
4. Definir la cardinalidad/formato/tamaño permitidos para evidencias y adecuar el selector y validadores.
5. Completar confirmaciones de salida, descarte y borrado de borrador.
6. Confirmar controles visibles de sincronización manual y resultado al usuario.
7. Ejecutar aceptación por rol (ciudadano/gestor/administrador) en un dispositivo con y sin conexión, incluyendo sesión expirada y permisos.

## Fuera de alcance / dependencias externas

Esta comparación no audita endpoints, permisos del servidor, reglas de negocio autoritativas, registros de auditoría, envío de correo, estado global de incidencias ni disponibilidad de catálogos/configuración. Se mencionan cuando condicionan el resultado que verá la APK. La evidencia es inspección estática del proyecto Android disponible; no se instaló ni ejecutó la APK durante esta revisión.
