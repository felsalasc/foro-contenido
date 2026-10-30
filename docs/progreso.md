# Progreso de Foro Contenido

Plan: docs/plan-implementacion.md, Task 1: Base autónoma.

Fecha: 29 de octubre de 2026. El usuario pide avanzar paso a paso y confirma dos repositorios separados, con alcance exclusivo de backend.

## Decisiones e interfaces

- Repositorio Git nuevo en foro-contenido, rama desarrollo, separado de foro-usuarios. Sin repositorio contenedor ni proyecto Maven padre propio.
- Paquete cl.duoc.forocontenido, puerto predeterminado 8082, H2 propio foro_contenido y perfil Oracle por variables de entorno.
- Ejecución directa de Task 1 en este chat. JDK y caché Maven existentes se reutilizan solo como herramientas locales; no se agregan rutas de otros proyectos a la aplicación.
- No se agregan todavía Security, cliente de identidad, JWT, categorías, temas ni comentarios. Task 2 consume esta base y añadirá el cliente HTTP de identidad.
- Sin conflictos entre los interfaces de este bloque y el contrato de identidad. No se modifica el repositorio de usuarios.

## Pasos ejecutados

1. Git propio inicializado y Wrapper copiado como archivos propios. pom.xml declara exclusivamente dependencias estándar, con relativePath vacío para el parent oficial Spring Boot.
2. AplicacionTest escrito y ejecutado antes de la clase principal. Fallo esperado por ausencia de @SpringBootConfiguration, código 1. Log local: .verificacion/base-red.log.
3. Clase principal, perfiles y documentación creados.
4. clean verify: BUILD SUCCESS, código 0, una prueba aprobada sin fallos/errores/omitidas. JAR ejecutable generado. Log local: .verificacion/base-verify.log.
5. Arranque HTTP del JAR comprobado en loopback con H2 propio, GET / devuelve 404 previsto. Proceso cerrado al terminar. Log: .verificacion/base-http.log.
6. Comprobación de POM y JAR: sin dependencia cl.duoc hacia usuarios y sin clases de usuarios empaquetadas. Enlaces de documentación válidos y exclusiones de archivos privados comprobadas.
7. Revisión independiente: sin hallazgos en el alcance de Task 1. Repositorio de usuarios permanece limpio, con su historial sin cambios.

Task 1: complete. Evidencia: clean verify, 1/1 prueba y arranque HTTP del JAR. Primer commit local propio del servicio, sin remoto ni publicación. Oracle no se ha comprobado.

## Registro de Task 2

Plan: docs/plan-implementacion.md, Task 2: Identidad HTTP y errores.

Pre-flight: consume la base de Task 1 y GET /api/auth/me definido en el contrato. Produce principal Identidad con rol y autoridades para categorías/temas/comentarios. La compilación sigue sin depender de foro-usuarios. No se agrega clave JWT al servicio de contenido.

- Prueba de contrato escrita primero con servidor HTTP local real. Fallo inicial: 200 esperado y 401 recibido; el token aún no se validaba mediante usuarios. Log: .verificacion/identidad-red.log.
- Cliente RestClient con URL fija desde configuración, conexión 2 segundos y lectura 3 segundos por defecto. Usa timeouts de lectura también sobre el cuerpo. No sigue redirecciones, no incorpora caché ni reintentos de aplicación. Se reenvía únicamente Authorization como Bearer; no se copian cookies de la solicitud.
- Respuesta 200 exigida, media type JSON, cuatro campos estrictos, validación de ID positivo, nombre/correo y rol USUARIO o ADMINISTRADOR. JSON incorrecto, estado inesperado, timeout o fallo de transporte producen 503. Un 401 de usuarios produce 401 sin copiar su cuerpo ni mensajes internos.
- Ruling: agregar UsuariosProperties, JsonConfig y ApiSecurityErrorWriter como unidades separadas de configuración y errores. La URL permite HTTP/HTTPS sin credenciales, query ni fragmento; timeouts positivos y representables en milisegundos. Limitar respuesta de identidad a 16 KiB evita leer un cuerpo inesperadamente grande; supera ampliamente el tamaño válido del contrato. No cambia el formato público de identidad.
- Filtro previo a los controladores, autenticación stateless y seguridad de métodos. Sin Basic, formulario, logout de sesión o usuario automático en memoria. Cabecera ausente, malformada o duplicada: 401. Permiso insuficiente: 403. Fallo de identidad: 503 antes de ejecutar la ruta.
- Suite completa: clean verify, BUILD SUCCESS, código 0; 36 pruebas, 0 fallos, 0 errores, 0 omitidas. IdentidadHttpTest 35 y AplicacionTest 1. Log: .verificacion/identidad-verify.log.
- Las pruebas verifican cero escrituras reales en H2 cuando falla la identidad o el permiso, utilizando controlador/tabla exclusivos de src/test. Incluyen redirección 302 sin seguir, respuesta lenta antes de cabeceras y durante cuerpo, desconexión, JSON incompleto/tipos incorrectos, respuesta excesiva, rol vigente y rechazo posterior sin caché.
- Revisión independiente: sin defectos bloqueantes ni observaciones accionables en Task 2.
- Integración de ambos JAR reales: registro y login en usuarios, token válido aceptado por contenido hasta el 404 de la ruta todavía inexistente; sin token e inválido 401; al detener usuarios, contenido devuelve 503. Contenido arrancó sin JWT_SECRET_BASE64. Se usó H2 y secretos aleatorios en memoria; ambos procesos se cerraron al terminar. Logs: .verificacion/integracion-usuarios.log y .verificacion/integracion-contenido.log.

Task 2: complete. Código verificado con 36/36 pruebas y comunicación HTTP real entre JAR independientes. Commit local sin publicación. Oracle continúa pendiente.

Tasks 3 a 6 pendientes. Próximo bloque: categorías y sus permisos administrativos.
