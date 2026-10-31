# Verificación de la base de Foro Contenido

Actualizado: 30 de octubre de 2026. Alcance: Tasks 1 a 5 del plan. Las funciones locales del backend están implementadas; la integración con persistencia Oracle, Postman y evidencia de entrega siguen pendientes. Se conserva la evidencia histórica de cada bloque.

| Comprobación | Resultado observado |
|---|---|
| Java | Microsoft OpenJDK 17.0.20.1 |
| Maven | Wrapper propio configurado para 3.9.14 |
| Spring Boot | 3.5.16 |
| Prueba previa al código principal | Error esperado por falta de @SpringBootConfiguration, código 1 |
| clean verify | BUILD SUCCESS, código 0 |
| AplicacionTest | 1 prueba, 0 fallos, 0 errores, 0 omitidas |
| JDBC | SELECT 1 correcto en H2 real |
| JAR | target/foro-contenido-0.0.1-SNAPSHOT.jar generado y ejecutado |
| HTTP | GET / devuelve 404 esperado, porque todavía no hay controladores de negocio |
| Datos locales | Base jdbc:h2:mem:foro_contenido, independiente de la de usuarios |
| Git | Repositorio propio en foro-contenido, rama desarrollo |
| Revisión independiente | Sin hallazgos en Task 1 |
| Oracle | Solo configuración preparada; conexión real pendiente |

En Task 1 el JAR se inició con perfil local, loopback 127.0.0.1 y puerto asignado automáticamente. Se comprobó su respuesta HTTP y se cerró el proceso al terminar. En ese bloque todavía no existía cliente HTTP a usuarios; la verificación de Task 2 siguiente lo amplía. Contenido no necesita una clave JWT.

## Autonomía comprobada

El pom utiliza el parent oficial Spring Boot con relativePath vacío. No declara módulos del foro ni dependencias cl.duoc hacia otro servicio. La única clase principal propia está en cl.duoc.forocontenido; los recursos no incluyen rutas a código o compilados de otros repositorios.

El comando de verificación reutilizó el JDK y una caché Maven ya disponibles en este equipo mediante JAVA_HOME y -Dmaven.repo.local; estas rutas no forman parte de la configuración ni de las instrucciones de instalación del proyecto. La aplicación se construyó desde su propia raíz y se ejecutó desde su JAR, sin importar clases del servicio de usuarios ni llamar a su API.

Se confirmaron raíces Git distintas para foro-usuarios y foro-contenido, cada una con su propio directorio .git, sin historial compartido. foro-usuarios permaneció limpio y conservó su último commit 1dcc2af. No se configuraron remotos ni se publicó código.

## Reproducción

Desde esta raíz, con JDK 17 y las dependencias disponibles:

```powershell
.\mvnw.cmd clean verify
java -jar target/foro-contenido-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=0
```

Consultar el puerto de Tomcat. El 404 anónimo de Task 1 es histórico: la versión actual exige autenticación y devuelve 401 sin token. Con token válido y usuarios accesible, `/` llega al 404 de una ruta sin controlador; no se añade una ruta de negocio para simular esta fase. Detener con Ctrl+C.

Las pruebas Maven se ejecutaron con permisos autorizados por las restricciones de instrumentación Java del entorno. OpenJDK emitió la advertencia conocida de class sharing; la suite terminó sin errores. Logs locales: .verificacion/base-red.log, .verificacion/base-verify.log y .verificacion/base-http.log. Informes JUnit: target/surefire-reports/. Estos artefactos están excluidos de Git.

## Pendientes

Colección Postman, ejecución SQL y conexión/verificación Oracle, integración con persistencia real y evidencia de entrega. No se implementa frontend.

## Verificación de identidad HTTP

| Comprobación | Resultado observado |
|---|---|
| Suite completa | 36 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Compilación y JAR | clean verify: BUILD SUCCESS, código 0 |
| Cliente real | Petición HTTP a servidor de prueba, sin simular RestClient |
| Credencial | Solo se reenvía Bearer; no cookies; sin cabecera no se consulta usuarios |
| Seguridad | 401 por token inválido/ausente y 403 por rol insuficiente |
| Fallos de usuarios | 503 por estado inesperado, desconexión, demora, JSON o identidad inválidos |
| Timeouts | Probados antes de recibir cabeceras y durante la lectura del cuerpo |
| Redirección | 302 rechazada con 503, sin enviar token al destino redirigido |
| Efectos | Cero escrituras H2 cuando falla la identidad o el permiso |
| Rol vigente | Nueva consulta en cada petición; cambio de rol y rechazo posterior reflejados sin caché |
| Dos JAR reales | Token emitido por usuarios aceptado por contenido; usuarios detenido produce 503 |
| Separación | Contenido arrancó sin clave JWT y no tiene dependencia Java de usuarios |
| Revisión independiente | Sin defectos bloqueantes ni observaciones accionables |
| Oracle | Pendiente de comprobación real |

Distribución: AplicacionTest 1 e IdentidadHttpTest 35. Las pruebas de identidad incluyen ID ausente/inválido, tipos numéricos/textuales incorrectos, nombre/correo inválidos, rol desconocido/nulo, cuerpo de más de 16 KiB y estados 204/3xx/403/404/429/500/503. Los estados distintos del 200 válido y del 401 se tratan como identidad no verificable.

La tabla PRUEBA_ESCRITURAS y las rutas /prueba existen solo en los recursos de prueba. Un controlador de test intenta insertar con JDBC, permitiendo comprobar ausencia de efectos cuando la seguridad rechaza la solicitud. No forman parte del código productivo ni del esquema SQL del foro.

La integración real ejecutó los dos JAR en loopback y puertos aleatorios. Se registró una cuenta en usuarios y se obtuvo su accessToken. Contenido, con USUARIOS_BASE_URL dirigido a esa instancia y sin JWT_SECRET_BASE64 en su entorno, devolvió 401 sin token, 404 para la ruta inexistente con token válido, 401 para token inválido y 503 después de detener usuarios. No se presenta el 404 como una consulta de negocio implementada: demuestra el paso correcto del filtro de identidad. Ambos procesos se cerraron y las credenciales temporales solo existieron en memoria.

Para reproducir la suite, ejecutar clean verify; no requiere el otro servicio. Para la integración real, iniciar ambos JAR según sus README y usar un token de usuarios. La aplicación no comparte clases, tablas, Wallet de esquema ni clave de firma. La prueba local usa el JAR precompilado de usuarios como contraparte HTTP, no como dependencia de compilación.

Evidencia local: .verificacion/identidad-red.log, .verificacion/identidad-verify.log, .verificacion/integracion-usuarios.log y .verificacion/integracion-contenido.log. Scripts y logs diagnósticos excluidos de Git; informes JUnit en target/surefire-reports/. No se publicaron remotos.

## Verificación de categorías

| Comprobación | Resultado observado |
|---|---|
| Pruebas iniciales antes de implementar | POST devolvía 404; esperados 201 para administrador y 403 para usuario |
| Suite final completa | 66 pruebas aprobadas, 0 fallos/errores/omitidas; clean verify código 0 |
| Distribución | Aplicación 1, identidad 35, categorías 30 |
| CRUD H2/JPA | Crear, consultar, listar, editar y eliminar; datos comprobados después de cada operación |
| Permisos | Usuario consulta; solo administrador crea, edita y elimina |
| Validación | Nombre obligatorio 1–100, descripción opcional hasta 500, JSON estricto, ID entero positivo |
| Duplicados | 409 sin distinguir mayúsculas o espacios exteriores; acentos conservados |
| Concurrencia | Dos creaciones simultáneas del mismo nombre: 201 y 409; un registro |
| SQL de datos | Cinco categorías cargadas como UTF-8 en H2, con nombres y acentos comprobados |
| JAR | Generado por este repositorio con clean verify |
| Dos JAR reales | CRUD HTTP con tokens emitidos por usuarios y roles vigentes |
| Códigos HTTP reales | 200, 201 con Location, 204, 400, 401, 403, 404, 409 y 503 |
| Revisión independiente | Sin defectos funcionales concretos en Task 3 |
| Oracle | Scripts preparados; ejecución y persistencia real pendientes |

La integración real inició usuarios con administrador inicial y una cuenta normal. Contenido usó únicamente USUARIOS_BASE_URL, sin clave JWT. Después del CRUD se detuvo usuarios y se comprobó 503 para una eliminación solicitada a contenido. Todos los procesos iniciados por la prueba se cerraron; no se guardaron contraseñas o tokens.

Se corrigió una lectura de SQL con codificación Windows que alteraba los acentos: ResourceDatabasePopulator usa explícitamente UTF-8. También se ajustó el diagnóstico HTTP para esperar la finalización del administrador inicial, posterior a la apertura del puerto. No fue necesario cambiar la aplicación de usuarios.

Evidencia: .verificacion/categorias-red.log, .verificacion/categorias-contrato-red.log, .verificacion/categorias-verify.log, .verificacion/categorias-usuarios-http.log y .verificacion/categorias-contenido-http.log; informes JUnit target/surefire-reports/. Logs y diagnóstico son locales e ignorados. El SQL Oracle de estructura no se presenta como probado por haber cargado los datos sobre H2; su ejecución real y las reglas de categorías con temas siguen pendientes.

## Verificación de temas y búsqueda

| Comprobación | Resultado observado |
|---|---|
| Pruebas antes de implementar | Publicación y búsqueda fallaban por API inexistente |
| Suite final | clean verify: BUILD SUCCESS, código 0; 105 aprobadas, 0 fallos/errores/omitidas |
| Distribución | Aplicación 1, identidad 35, categorías 30, temas 39 |
| CRUD | 201 con Location, consulta/edición 200 y borrado 204; inexistente 404 |
| Autoría | ID obtenido de identidad HTTP; usuario ajeno y administrador ajeno reciben 403 |
| Inmutabilidad | Autor y fecha idénticos al crear, consultar y editar; campos ajenos en JSON producen 400 |
| Validación | Título 1–200; contenido no blanco hasta 10000; tipos y categoría inválidos rechazados |
| Búsqueda | Título o contenido; texto y categoría con AND; mayúsculas ignoradas, acentos conservados |
| Literales | %, _ y ! no amplían coincidencias como comodines |
| Texto largo | Coincidencia después del carácter 9500 y lectura de todo el CLOB |
| Orden | Fecha descendente y, con fecha igual, ID descendente |
| Categoría | Movimiento a categoría existente; inexistente al escribir 404, como filtro [] |
| Categoría en uso | 409 sin eliminar tema o categoría |
| Concurrencia | Borrado pendiente y carrera de publicación/borrado sin temas huérfanos |
| Precisión de fecha | Microsegundos UTC; se corrigió diferencia de redondeo detectada por HTTP |
| Integración real | Dos JAR, tokens de usuarios, CRUD y búsqueda de temas por HTTP con las restricciones anteriores |
| Caída de usuarios | 503 antes de publicar; sin compartir JWT_SECRET_BASE64 |
| Revisión independiente | Sin defectos concretos encontrados mediante inspección estática |
| Oracle | SQL preparado, con CLOB/FK/índice; ejecución real, concurrencia y búsqueda pendientes |

La primera ejecución completa detectó una carrera (POST 201 y DELETE 204). Una reproducción con eliminación JDBC pendiente demostró la insuficiencia del chequeo previo/FK en H2 bajo esa secuencia. Se añadió bloqueo de la categoría en las operaciones transaccionales de publicar, mover y eliminar categoría. La reproducción y la suite final aprobaron. No se extrapola el comportamiento observado de H2 al motor Oracle sin probarlo allí.

La primera integración HTTP también detectó fracciones de segundo diferentes entre el objeto recién creado y el registro leído. Una prueba de API reprodujo la diferencia; generar la fecha con precisión de microsegundos la corrigió. La segunda integración verificó igualdad exacta entre las respuestas de creación y edición.

Logs locales: .verificacion/temas-red.log, .verificacion/temas-carrera-diagnostico.log, .verificacion/temas-carrera-red.log, .verificacion/temas-fecha-red.log, .verificacion/temas-verify.log, .verificacion/temas-usuarios-http.log y .verificacion/temas-contenido-http.log. Todos ignorados por Git. Los procesos de integración se cerraron y los secretos temporales no se guardaron. Usuarios permanece sin cambios; contenido conserva construcción y repositorio propios.

## Verificación de comentarios y datos de demostración

| Comprobación | Resultado observado |
|---|---|
| Pruebas previas a implementación | 32 casos; 28 fallos por rutas ausentes, 0 errores |
| Suite final | clean verify: BUILD SUCCESS, código 0; 138 aprobadas, 0 fallos/errores/omitidas |
| Distribución | Aplicación 1, identidad 35, categorías 30, temas 39, comentarios 32, datos 1 |
| CRUD | Crear 201/Location, consultar/listar/editar 200, borrar 204 y consulta posterior 404 |
| Propiedad | Solo autor modifica; dueño del tema y administrador ajenos reciben 403 |
| Inmutabilidad | Autor, tema y fecha conservados; intentos de cambiarlos por JSON producen 400 |
| Contenido | No blanco, máximo 2000; rechaza tipos incorrectos y campos desconocidos |
| Listado | Solo del tema indicado, fecha ASC e ID ASC; [] si existe sin comentarios |
| Borrado de tema | Elimina sus comentarios, incluidos autores distintos; conserva otros temas y sus comentarios |
| Borrado ajeno | 403 sin eliminar comentarios |
| Atomicidad ante fallo | FK auxiliar de test impide eliminar tema; tema y comentarios permanecen tras 409 |
| Concurrencia | Borrado pendiente del tema coordinado con publicación, sin comentarios huérfanos |
| SQL de datos en H2 | 5 categorías, 3 temas y 3 comentarios; autores 41/73/109 parametrizados y relaciones correctas |
| Dataset por HTTP real | 3 cuentas activas reales, 5 categorías, 3 temas y 3 comentarios después del CRUD |
| Integración JAR | CRUD, permisos, inmutabilidad, cascada y rechazo 503 al detener usuarios |
| Separación | JAR propio; contenido no recibió clave JWT ni consultó tablas de usuarios |
| Revisión independiente | Sin defectos concretos de Task 5 por inspección estática |
| Oracle | Esquema y datos preparados; ejecución de scripts, búsqueda, concurrencia y persistencia reales pendientes |

La prueba de datos lee los archivos UTF-8, sustituye los parámetros de autores por IDs simulados y retira WHENEVER/ACCEPT para ejecutar las sentencias SQL en H2. No verifica los comandos del cliente Oracle. La integración de JAR usa tres cuentas reales y sus IDs devueltos por HTTP; carga los datos a través de las API y comprueba los mínimos después de operar sobre un cuarto tema temporal. Estos dos resultados son complementarios y no acreditan persistencia Oracle.

El borrado en cascada se verifica con datos persistidos reales de H2, incluyendo comentarios de otros autores y de otro tema. Una referencia auxiliar exclusiva de test provoca un fallo de base y permite comprobar rollback. El esquema de producción contiene únicamente CATEGORIAS, TEMAS y COMENTARIOS; no incluye esa tabla de prueba.

La integración arrancó ambos JAR por separado, con claves/contraseñas aleatorias solo en memoria. Contenido recibió exclusivamente la URL de usuarios. Los procesos se cerraron y los logs no contienen tokens o contraseñas. Evidencia local: .verificacion/comentarios-red.log, .verificacion/datos-demo-red.log, .verificacion/comentarios-verify.log, .verificacion/comentarios-usuarios-http.log y .verificacion/comentarios-contenido-http.log. Informes JUnit en target/surefire-reports/; todo diagnóstico excluido de Git.

## Preparación de Oracle y Postman — 31 de octubre de 2026

- clean verify offline ejecutado de nuevo desde cada raíz: usuarios 85 y contenido 138 pruebas, todas aprobadas, sin fallos/errores/omitidas. Ambos JAR propios generados. Log en cada repositorio: .verificacion/preparacion-entrega-verify.log.
- Colecciones y entornos independientes preparados, con 31 solicitudes de usuarios y 54 de contenido. Contraseñas de administrador vacías en los archivos; tokens y contraseñas generadas solo en pm.variables durante la ejecución. Las operaciones de baja y borrado se limitan a registros creados por el recorrido; se mantienen mínimos de demostración.
- JSON leído y scripts compilados con Node/vm: 32 scripts de usuarios y 55 de contenido (incluyen preparación inicial). No se ejecutó el runtime Postman ni sus aserciones.
- Reproductor diagnóstico HTTP: 31 + 54 solicitudes tomadas de los archivos de colección, estados esperados comprobados contra ambos JAR con H2. Además comprobó búsqueda literal combinada, autoría/fechas y mínimos de datos. Los IDs/tokens se extrajeron de respuestas reales. Los scripts Postman no se ejecutaron en este reproductor; sus aserciones requieren Collection Runner.
- La descarga de Newman fue rechazada y no se instaló; no se intentó instalarlo por otra vía. Se usaron únicamente herramientas existentes. Logs HTTP en contenido: .verificacion/postman-usuarios-http.log y .verificacion/postman-contenido-http.log. Diagnósticos, scripts de generación/reproducción y logs permanecen ignorados por Git.
- Se preparó database/03_verificacion.sql, solo lectura y sin mostrar hashes, junto con documentación de esquemas, matrices y guion. Existe un Wallet local, pero no hay credenciales Oracle configuradas en esta sesión ni confirmación de instancia/esquemas. No se inició sesión ni se ejecutó DDL o carga sobre Oracle.
- Oracle real, bloqueos, persistencia tras reiniciar, runtime/aserciones Postman, enlaces remotos, video y entrega final permanecen pendientes. No se asignan puntos de la pauta por anticipado ni se marca cerrado ese bloque.

Actualización: BIBLIOTECA activa según el usuario; FORO_USUARIOS y FORO_CONTENIDO aún sin crear. La revisión de colecciones identificó la parada incompleta ante aserciones; corregida junto con el escape JSON de credenciales. Ocho pruebas focalizadas en Node reprodujeron rol/estructura/JSON inválidos y contraseña con comillas/barras en ambas colecciones, y aprobaron tras corregir. Usan un doble mínimo de pm.test/variables y no sustituyen ejecutar Postman. Recompilación de scripts y 85 flujos HTTP finales aprobados. Oracle no se ha conectado ni modificado.
