# Progreso de Foro Contenido

Estado vigente: Task 5 completada el 30 de octubre de 2026. Los apartados anteriores conservan la evidencia histórica de cada bloque.

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

## Registro de Task 3

Plan: docs/plan-implementacion.md, Task 3: Categorías. Base del bloque: b7412c3.

Pre-flight: consume Identidad y seguridad de métodos de Task 2. Produce Categoria y su repositorio para la relación de temas de Task 4. No hay cambios de contrato HTTP ni dependencias de archivos de usuarios. La protección de categorías con temas se añade cuando exista esa relación, en Task 4.

- Pruebas adminCreaCategoria201 y usuarioNoCreaCategoria403 ejecutadas antes del controlador: ambas fallaron por 404 en lugar de 201/403. Log: .verificacion/categorias-red.log. Contrato completo ejecutado antes de implementar: fallos por API, tabla y SQL ausentes; sin marcar ese resultado como verificación válida de funcionalidad.
- CRUD en capas entidad, repositorio, DTO, servicio y controlador. Consulta autenticada; escritura protegida por ADMINISTRADOR. Respuestas 201 con Location, 200, 204, 400, 403, 404 y 409. Sin exponer entidad o clave interna.
- Decisión técnica: CLAVE_NOMBRE almacena nombre.strip().toUpperCase(Locale.ROOT), con unique en la base y longitud 300 para expansiones Unicode. La escritura SQL manual debe conservar esa equivalencia. Los acentos siguen siendo significativos. La restricción de base protege duplicados simultáneos además de la comprobación previa.
- SQL propio de estructura Oracle y cinco categorías, sin IDs de usuarios ni credenciales. La carga de prueba detectó caracteres alterados al usar la codificación predeterminada de Windows; lectura explícita UTF-8 corrigió la causa. README documenta UTF-8 también para el cliente SQL.
- clean verify final: BUILD SUCCESS, código 0; 66 pruebas, 0 fallos, 0 errores, 0 omitidas. Distribución: AplicacionTest 1, IdentidadHttpTest 35, CategoriasApiTest 30. JAR generado. Log: .verificacion/categorias-verify.log.
- Pruebas de CRUD/persistencia H2, límites y tipos JSON, ausencia de escrituras rechazadas, duplicados normalizados, edición sobre sí misma, dos creaciones simultáneas (201 y 409; un registro) y carga de cinco categorías con acentos correctos.
- Revisión independiente en solo lectura: sin defectos funcionales concretos. Oracle queda para su verificación real; temas/comentarios y su integridad corresponden a los bloques siguientes. Los resultados de pruebas los verificó el implementador.
- Integración real de los dos JAR: administrador inicial y usuario normal en usuarios; CRUD completo de categorías en contenido con los tokens respectivos. Se verificaron 201/Location, lectura y edición 200, eliminación 204 y consulta posterior 404, permiso 403, duplicado 409, campo extra 400, falta de token 401 y caída de usuarios 503. El script esperó a que terminara ApplicationRunner antes de iniciar sesión; el puerto abre antes del bootstrap. Contenido arrancó sin clave JWT. Procesos cerrados y secretos aleatorios solo en memoria.
- Logs de integración: .verificacion/categorias-usuarios-http.log y .verificacion/categorias-contenido-http.log. Herramientas diagnósticas excluidas de Git.

Task 3: complete. Evidencia: clean verify 66/66, CRUD HTTP entre JAR reales y revisión independiente. Commit local sin publicación. Oracle aún no comprobado.

## Registro de Task 4

Plan: docs/plan-implementacion.md, Task 4: Temas y búsqueda. Base del bloque: 4b2f282. Ejecución directa en el repositorio existente, sin modificar usuarios.

Pre-flight: consume Identidad de Task 2 y Categoria de Task 3; produce Tema para los comentarios de Task 5. No modifica el contrato de identidad ni agrega dependencias Maven de otros repositorios. Los datos de temas y comentarios parametrizados con autores reales se preparan juntos en Task 5, como indica el plan.

- TemasApiTest escrito antes de la implementación y ejecutado: 37 casos, fallos esperados por rutas de temas y tabla inexistentes. usuarioPublicaTemaConSuIdentidad recibió 404 en lugar de 201; buscaPorTextoYCategoria no pudo crear el tema inicial. Log: .verificacion/temas-red.log.
- CRUD de temas con DTO; autor asignado desde Identidad y autor/fecha inmutables. Solo el autor edita o elimina, aunque la otra cuenta sea administradora. Título 1–200, contenido no blanco de hasta 10000, categoría positiva obligatoria y existente. Validación estricta rechaza autorId/fechaCreacion/id del body.
- Búsqueda opcional por título/contenido sin distinguir mayúsculas, con AND de categoría; texto vacío no filtra. Escape de !, % y _ como literales. Orden fecha DESC e ID DESC. Categoría inexistente como filtro devuelve []; para crear o editar devuelve 404.
- Decisión técnica: contenido CLOB con consultas SQL nativas LOWER/LIKE, sin convertir ni truncar el texto. Documentación Oracle enlazada en README admite esas operaciones; la comprobación real en Oracle permanece pendiente. H2 verificó coincidencias después del carácter 9500 y contenido de 10000 caracteres acentuados.
- Se agregó FK_TEMAS_CATEGORIA y su índice; categoría con temas produce 409. Una primera carrera dio POST 201 y DELETE 204. La traza SQL confirmó la presencia de la FK; una prueba controlada con borrado JDBC pendiente reprodujo publicación 201 pese al borrado posterior. La causa fue confiar solo en comprobaciones previas/FK bajo esa concurrencia en H2.
- Corrección de concurrencia: crear/mover tema y eliminar categoría adquieren PESSIMISTIC_WRITE sobre la misma categoría hasta el commit. Prueba borradoPendienteImpidePublicarEnCategoriaEliminada RED→GREEN, además de carreraPublicacionYBorradoNoDejaHuerfanos. La FK permanece como garantía adicional. Coste: las publicaciones o cambios que usan una misma categoría se serializan durante esa breve transacción; la llamada HTTP de identidad ocurre antes de ella.
- La integración con JAR detectó un cambio de precisión entre la fecha recién creada y su lectura. Se reprodujo en fechaCreacionNoCambiaEntrePublicacionConsultaYEdicion: fracción de segundo .8665338Z frente a .866534Z. Se ajustó la generación UTC a microsegundos, igual a TIMESTAMP(6). Prueba RED→GREEN y coincidencia exacta en HTTP al crear/consultar/editar.
- Verificación final: clean verify, BUILD SUCCESS, código 0; 105 pruebas, 0 fallos, 0 errores, 0 omitidas. AplicacionTest 1, IdentidadHttpTest 35, CategoriasApiTest 30, TemasApiTest 39. JAR propio generado. Log: .verificacion/temas-verify.log.
- Revisión independiente estática: sin defectos concretos de Task 4. La ejecución de pruebas y el historial RED/GREEN los comprobó el implementador; Oracle queda pendiente. Los dos defectos detectados ejecutando las pruebas fueron corregidos con reproducción y suite completa.
- Integración de ambos JAR reales en loopback: registro/login y administrador inicial en usuarios; categoría, publicación, consulta, búsqueda combinada sobre CLOB largo y edición/eliminación por el autor. Administrador ajeno recibe 403, categoría en uso 409, autorId falsificado 400, tema eliminado 404 y usuarios detenido 503 sin ejecutar publicación. Fecha exacta conservada. Contenido no recibió clave JWT. Todos los procesos se cerraron; contraseñas/tokens aleatorios solo en memoria.
- Logs diagnósticos locales: .verificacion/temas-red.log, temas-carrera-diagnostico.log, temas-carrera-red.log, temas-fecha-red.log, temas-verify.log, temas-usuarios-http.log y temas-contenido-http.log. Se mantienen fuera de Git.

Task 4: complete. Evidencia: clean verify 105/105, integración HTTP real y revisión independiente. Commit local sin publicación. Oracle no se presenta como verificado.

## Registro de Task 5

Plan: docs/plan-implementacion.md, Task 5: Comentarios y reglas de eliminación. Base del bloque: 62bd33b. Se continúa en desarrollo dentro del repositorio independiente existente; usuarios no se modifica.

Pre-flight: consume Identidad, Tema y la seguridad existentes; produce comentarios y completa el borrado de temas con sus comentarios. Todas las escrituras de comentarios y el borrado del tema coordinan la misma fila de tema. El SQL de datos usa parámetros de autores sin acceso al esquema de usuarios.

- ComentariosApiTest escrito y ejecutado antes de implementar: 32 casos, 28 fallos esperados por rutas ausentes y 0 errores. comentaTemaExistenteConAutorAutenticado esperaba 201 y recibió 404; los casos de propiedad y cascada también fallaron al intentar crear el comentario. Log: .verificacion/comentarios-red.log.
- CRUD de comentarios con entidad, repositorio, DTO, servicio y controlador propios. Contenido obligatorio no blanco de hasta 2000, autor exclusivamente de identidad HTTP, tema tomado de la ruta; autor, tema y fecha inmutables al editar. Se conservan espacios/saltos de línea; fecha generada en UTC con microsegundos.
- Consulta autenticada; modificación directa solo por autor. El dueño del tema y un administrador ajeno reciben 403 al editar/borrar comentarios. Un administrador sí gestiona sus propios comentarios. Listado solo del tema solicitado, fecha ASC e ID ASC; tema existente vacío devuelve [], relaciones ausentes 404 e ID/JSON inválidos 400.
- Decisión técnica: comentario con CLOB para conservar texto Unicode sin depender del límite de bytes de VARCHAR2. FK_COMENTARIOS_TEMA con ON DELETE CASCADE tanto en SQL como en el esquema Hibernate local mediante OnDelete. TemaService exige autor antes de borrar. La base elimina tema e hijos en una misma sentencia/transacción; no se agrega una colección bidireccional a Tema.
- Coordinación de escritura: PESSIMISTIC_WRITE sobre el tema al crear/editar/borrar comentarios y al borrar el tema. Para editar/borrar comentario se obtiene primero una proyección de temaId, se toma el bloqueo y recién se carga el comentario, evitando una entidad obsoleta en el contexto de persistencia. Coste: escrituras sobre comentarios del mismo tema se serializan brevemente; la autenticación HTTP ocurre antes de la transacción.
- Se probaron comentarios de varios autores, conservación de comentarios de otros temas, borrado ajeno sin efectos, rollback del tema y comentarios ante una FK adicional exclusiva de test que impide borrar, y publicación con eliminación JDBC pendiente sin comentarios huérfanos. No se añadieron rutas o tablas de diagnóstico al producto.
- DatosDemostracionTest escrito y ejecutado antes del nuevo script: error esperado por archivo 02b inexistente. Luego se creó 02b_temas_comentarios_demostracion.sql con IDs solicitados por ACCEPT NUMBER en SQLcl/SQL*Plus; no asume 1/2/3. Se ejecuta tras cinco categorías y con tablas de temas/comentarios vacías. El cliente aborta con rollback ante error SQL.
- Prueba del SQL de datos sobre H2: sustituye únicamente parámetros y retira directivas del cliente Oracle; usa IDs simulados 41/73/109 y comprueba cinco categorías, tres temas, tres comentarios, autores y relaciones. No se presenta ese resultado como ejecución de SQLcl ni de Oracle.
- Verificación final: clean verify, BUILD SUCCESS, código 0; 138 pruebas, 0 fallos, 0 errores, 0 omitidas. AplicacionTest 1, IdentidadHttpTest 35, CategoriasApiTest 30, TemasApiTest 39, ComentariosApiTest 32 y DatosDemostracionTest 1. JAR propio generado. Log: .verificacion/comentarios-verify.log.
- Revisión independiente estática: sin defectos concretos en Task 5. Ejecución de TDD e integración acreditadas por el implementador; Oracle no acreditado.
- Integración real de ambos JAR con H2: administrador inicial y dos usuarios normales registrados/iniciados por HTTP; IDs obtenidos por me, sin asumir valores. Se cargaron cinco categorías, tres temas y tres comentarios con autores reales. Un cuarto tema temporal permitió probar edición/borrado de comentario propio, rechazo 403 a dueño del tema y administrador ajenos, suplantación 400, fecha inmutable y cascada al borrar el tema. Los mínimos 3/5/3/3 se comprobaron después del CRUD. Usuarios detenido produjo 503 antes de comentar.
- Contenido se inició sin JWT_SECRET_BASE64; procesos cerrados al terminar y secretos aleatorios únicamente en memoria. Logs: .verificacion/comentarios-usuarios-http.log y .verificacion/comentarios-contenido-http.log. Scripts/logs diagnósticos permanecen fuera de Git.

Task 5: complete. Evidencia: clean verify 138/138, carga de datos SQL sobre H2, dataset con cuentas reales por HTTP, CRUD/cascada entre JAR y revisión independiente. Commit local sin publicación. Oracle sigue pendiente de ejecución real.

Task 6 pendiente: Oracle, Postman, integración con persistencia real y evidencia de entrega. Sin frontend y sin publicación automática.

## Preparación parcial de Task 6 — 31 de octubre de 2026

Se prepararon colección/entorno Postman propios, SQL de verificación, documentación Oracle, matriz y guion del video. Ver docs/verificacion.md para resultados: compilación independiente verde (85 usuarios / 138 contenido), 85 flujos HTTP locales verificados y scripts JSON/JS revisados. Se eligió carga de usuarios mediante inicializador + registro, alternativa prevista por el plan, en lugar de SQL con hashes predefinidos.

Newman no se instaló porque su descarga fue rechazada. El reproductor HTTP local no se presenta como ejecución del runtime Postman. Wallet existente sin credenciales ni confirmación de instancia/esquemas; se solicitó esa información sin pedir contraseñas por chat. No se conectó ni modificó Oracle. Este avance se registra en commit local propio; el bloque sigue incompleto y no se publican remotos ni una entrega final.

Actualización de preparación: el usuario confirmó BIBLIOTECA activa y ambos esquemas pendientes. Las instrucciones SQL como administrador están preparadas en docs/oracle-y-entrega.md; no se ejecutaron. La revisión independiente encontró que una aserción fallida con HTTP esperado no detenía la colección. Se reprodujo y corrigió: las guardas ahora detienen el recorrido ante aserciones, JSON o estructura inválidos. También se reprodujo y corrigió el escape de comillas/barras en la contraseña del login administrativo mediante JSON.stringify. Ocho casos Node focalizados pasaron RED→GREEN sobre esos scripts; son dobles mínimos del contrato de callbacks/variables, no el runtime Postman. Los 85 flujos HTTP se volvieron a verificar con los archivos finales. No hubo cambios de Java ni necesidad de repetir la suite Maven ya aprobada después de esos ajustes de colección.

## Evidencia aportada: Postman de contenido sobre Oracle

31 de octubre de 2026: tras confirmar arranque de ambos JAR con perfil oracle, el usuario aportó el resumen de contenido: 66 aserciones aprobadas, cero fallos/omitidas/errores, una iteración. La colección tiene 54 solicitudes. Se conserva la captura sin secretos en [evidencias/2026-10-31-postman-contenido.md](evidencias/2026-10-31-postman-contenido.md).

El Runner real de contenido deja de estar pendiente; usuarios ya cuenta con evidencia propia de 31 solicitudes y 36 aserciones aprobadas. Restan conteos/relaciones SQL finales, reinicios y persistencia, prueba CLOB larga, caída de usuarios sin escrituras, concurrencia Oracle y entrega. La evidencia aportada no se extrapola a comprobaciones todavía no ejecutadas.

## Conteos Oracle confirmados por SQL — antes del reinicio

31 de octubre de 2026, evidencia aportada por el usuario: CATEGORIAS: 10; TEMAS: 3; COMENTARIOS: 3. Se cumplen los mínimos de cinco categorías y tres registros por tabla. Captura conservada en [evidencias/2026-10-31-oracle-conteos-contenido.md](evidencias/2026-10-31-oracle-conteos-contenido.md). El siguiente paso es reiniciar ambos servidores conservando sus variables y repetir las consultas sin volver a ejecutar las colecciones, para no agregar datos nuevos. Persistencia tras reinicio, integridad/bloqueos y verificaciones adicionales continúan pendientes.

## Persistencia tras reiniciar aplicaciones — confirmación del usuario

31 de octubre de 2026: el usuario confirmó que ambas consultas SQL mantienen los conteos anteriores tras el reinicio guiado de los JAR, sin repetir colecciones. 10 categorías, 3 temas y 3 comentarios. Ver [confirmación y límites de evidencia](evidencias/2026-10-31-oracle-reinicio.md). No hay captura posterior todavía ni comprobación directa del agente. Se registra conservación de conteos al reiniciar aplicaciones; no se extrapola a reinicio del servicio Oracle ni a igualdad de cada campo. Las comprobaciones de integridad, búsqueda CLOB larga, caída de identidad y concurrencia Oracle siguen pendientes.

## Relaciones Oracle sin huérfanos

31 de octubre de 2026: captura SQL aportada por el usuario confirma 0 temas sin categoría y 0 comentarios sin tema. Evidencia en [evidencias/2026-10-31-oracle-integridad.md](evidencias/2026-10-31-oracle-integridad.md). Restan metadatos de FK, búsqueda CLOB larga, caída de identidad sin escrituras y concurrencia/bloqueos; esta consulta no se extrapola a esos casos.
