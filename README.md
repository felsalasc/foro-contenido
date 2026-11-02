# Foro Contenido

Microservicio REST para gestionar categorías, temas y comentarios de un foro. Aplicación Spring Boot con puerto predeterminado **8082** y repositorio Git independiente.

## Funcionamiento

El servicio ofrece autenticación mediante HTTP, CRUD de categorías, temas y comentarios, búsqueda y eliminación transaccional de temas con sus comentarios. Las rutas requieren Bearer y contenido consulta la identidad vigente en usuarios antes de ejecutarlas. Sin token `/` devuelve 401; con identidad válida devuelve 404 porque no hay controlador para esa ruta.

Este repositorio contiene su propio pom.xml, Maven Wrapper, código, configuración y pruebas. No existe un proyecto padre del foro ni una dependencia Java o Maven hacia foro-usuarios. Se compila y arranca por separado.

La identidad se verifica mediante HTTP a GET `/api/auth/me` de foro-usuarios. Contenido no emite ni descifra JWT, no necesita JWT_SECRET_BASE64 y no accede a USUARIOS. Sus tablas de categorías, temas y comentarios se almacenan en un esquema Oracle propio.

## Requisitos y compilación

- JDK 17 con JAVA_HOME definido.
- Maven Wrapper incluido, versión Maven 3.9.14. Su primer uso y las dependencias requieren Maven Central si no están en la caché del equipo.
- Spring Boot 3.5.16, Web, JPA, Validation y dependencias Oracle administradas mediante BOM.

Desde la raíz de este repositorio, en PowerShell:

```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

En Linux o macOS: `sh ./mvnw clean verify`, con JDK 17 disponible. Las pruebas actuales no requieren que foro-usuarios esté iniciado.

## Ejecución local

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

El arranque y la compilación no necesitan usuarios activo. Para autorizar peticiones, definir la URL HTTP de usuarios; por defecto se usa http://localhost:8081:

```powershell
$env:USUARIOS_BASE_URL = 'http://localhost:8081'
$env:USUARIOS_CONNECT_TIMEOUT = '2s'
$env:USUARIOS_READ_TIMEOUT = '3s'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

El perfil predeterminado es local. H2 en memoria usa una base propia, foro_contenido, y modo Oracle; los datos se pierden al detener la aplicación. Hibernate usa create-drop y la consola H2 está deshabilitada. Para conservar los datos entre reinicios, usar el perfil `oracle`.

Si el entorno define SPRING_PROFILES_ACTIVE, ese valor prevalece sobre el perfil predeterminado; usar el perfil explícito anterior para evitar confusiones. No activar local y oracle simultáneamente.

Ejecutar el JAR después de compilar:

```powershell
java -jar target/foro-contenido-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

PORT permite cambiar el puerto. Si usuarios y contenido se ejecutan desde terminales diferentes, cada uno debe recibir su propio PORT, o dejar la variable sin definir para usar 8081 y 8082 respectivamente. Se puede usar `--server.port=0` para asignar un puerto libre y consultar el registro de Tomcat.

## Perfil Oracle

Definir ORACLE_URL, ORACLE_USERNAME y ORACLE_PASSWORD para el esquema de contenido. No usar las credenciales del esquema de usuarios. Si corresponde, la URL incluye TNS_ADMIN apuntando a un Wallet externo al repositorio.

```powershell
$env:ORACLE_URL = 'jdbc:oracle:thin:@SERVICIO?TNS_ADMIN=C:/Oracle/Wallet_Foro'
$env:ORACLE_USERNAME = 'FORO_CONTENIDO'
$taskCredential = Get-Credential -UserName $env:ORACLE_USERNAME -Message 'Credenciales del esquema de contenido'
$env:ORACLE_PASSWORD = $taskCredential.GetNetworkCredential().Password
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=oracle'
# Al detener el servidor:
Remove-Item Env:ORACLE_PASSWORD
```

Servicio, ruta y usuario del ejemplo deben ajustarse a la instancia real. Oracle usa ddl-auto=validate y no ejecuta scripts automáticamente. Los scripts de database/ incluyen categorías, temas y comentarios.

No hay valores predeterminados de credenciales Oracle. Spring Boot no carga archivos .env automáticamente. Wallet, variables privadas y certificados quedan fuera del control de versiones.

## Identidad y seguridad

Obtener un accessToken mediante POST `/api/auth/login` en usuarios y enviarlo a contenido con `Authorization: Bearer <accessToken>`. Contenido reenvía únicamente esa credencial a GET `/api/auth/me`; no reenvía cookies ni usa una URL indicada en la petición entrante.

| Situación | Respuesta de contenido |
|---|---|
| Falta Bearer, cabecera inválida o usuarios responde 401 | 401, con WWW-Authenticate: Bearer |
| Identidad vigente válida | Se ejecuta la ruta solicitada con el ID y rol devueltos por usuarios |
| La ruta exige un permiso que el rol no tiene | 403 |
| Usuarios no responde, vence un timeout o devuelve un estado inesperado | 503; no se ejecuta la operación |
| Identidad incompleta, tipos incorrectos, rol desconocido o respuesta no JSON | 503; no se ejecuta la operación |

Los errores usan application/problem+json con status, title y detail, sin copiar mensajes internos de usuarios ni incluir el token. Se exige una única cabecera Authorization y un valor Bearer válido. Los roles admitidos son USUARIO y ADMINISTRADOR.

USUARIOS_BASE_URL debe ser una URL HTTP o HTTPS sin credenciales, query ni fragmento. El cliente usa 2 segundos para conexión y 3 para lectura por defecto, configurables con las variables anteriores; deben ser mayores que cero. No sigue redirecciones ni incorpora reintentos de aplicación o caché de identidad. La respuesta de identidad se limita a 16 KiB y se valida con el contrato de cuatro campos y las restricciones de nombre/correo/rol.

Cada petición vuelve a consultar usuarios. Los cambios de rol y la desactivación de cuentas se reflejan sin esperar a que expire el token. La API usa autenticación stateless; no hay sesiones, cookies de autenticación, formulario ni HTTP Basic. La cuenta en memoria predeterminada de Spring Boot está deshabilitada. No se configura CORS abierto. El servicio expone una API REST y no incluye interfaz web.

## Categorías

Todas las operaciones requieren un token vigente emitido por usuarios.

| Método y ruta | Permiso | Respuesta correcta |
|---|---|---|
| GET /api/categorias | Usuario o administrador | 200, lista ordenada por ID ascendente; [] si está vacía |
| GET /api/categorias/{id} | Usuario o administrador | 200, categoría |
| POST /api/categorias | Administrador | 201, categoría y Location |
| PUT /api/categorias/{id} | Administrador | 200, categoría actualizada |
| DELETE /api/categorias/{id} | Administrador | 204, sin cuerpo |

POST y PUT reciben el mismo JSON; PUT reemplaza nombre y descripción:

```json
{"nombre":"Programación","descripcion":"Consultas sobre Java y algoritmos"}
```

Nombre obligatorio de 1 a 100 caracteres después de quitar espacios exteriores. Descripción opcional de hasta 500 caracteres; omitida, null o en blanco se guarda como null. Las respuestas contienen solo id, nombre y descripcion. Campos desconocidos y tipos incorrectos producen 400. Un ID debe ser entero positivo; categoría inexistente produce 404. Los cambios requieren rol ADMINISTRADOR (403 para USUARIO).

Los nombres son únicos sin distinguir mayúsculas ni espacios exteriores, conservando acentos: ` programación ` duplica `Programación`, pero `Programacion` es diferente. Una clave interna con mayúsculas Locale.ROOT y restricción única protege también escrituras simultáneas; el duplicado produce 409. No se expone esa clave por HTTP.

Scripts propios en [database/01_estructura.sql](database/01_estructura.sql) y [database/02_datos_demostracion.sql](database/02_datos_demostracion.sql). Abrirlos como **UTF-8** en el cliente SQL. Ejecutar primero la estructura y luego los datos, una sola vez sobre la tabla vacía del esquema exclusivo de contenido. El segundo script inserta cinco categorías con IDs generados; no requiere IDs de usuarios. Mantener CLAVE_NOMBRE coherente con la normalización documentada al cargar SQL manualmente. No se cargan automáticamente al arrancar; el perfil local usa H2 en memoria y pierde sus datos al reiniciar.

Una categoría que contiene temas no se puede eliminar (409). Publicar o mover un tema y eliminar su categoría se coordinan mediante un bloqueo de fila durante la transacción; la base conserva además la clave foránea sin borrado en cascada.

## Temas y búsqueda

Todas las rutas requieren identidad vigente. Tanto USUARIO como ADMINISTRADOR pueden publicar y consultar. Solo el autor puede editar o eliminar su tema; el administrador no modifica temas ajenos.

| Método y ruta | Resultado correcto |
|---|---|
| POST /api/temas | 201, tema creado y Location |
| GET /api/temas | 200, lista de temas |
| GET /api/temas/{id} | 200, tema |
| PUT /api/temas/{id} | 200, tema actualizado por su autor |
| DELETE /api/temas/{id} | 204, sin cuerpo; solo su autor |

POST y PUT reciben estos tres campos:

```json
{"titulo":"Consulta sobre Java","contenido":"¿Cómo se usa una interfaz?","categoriaId":1}
```

Usar un categoriaId existente obtenido de GET /api/categorias. Título obligatorio de 1 a 200 caracteres después de quitar espacios exteriores; contenido obligatorio, no blanco, hasta 10000 caracteres. El contenido conserva sus saltos de línea y espacios. La respuesta incluye id, titulo, contenido, categoriaId, autorId y fechaCreacion. El autor se obtiene exclusivamente de la identidad HTTP; autor y fecha no cambian al editar. La fecha se expresa en UTC con precisión de microsegundos.

Enviar autorId, fechaCreacion, id u otros campos desconocidos produce 400. Un tema o una categoría solicitada al publicar/editar que no existe produce 404. Los ID deben ser enteros positivos (400 si son inválidos). Modificar un tema ajeno produce 403 y no cambia los datos. PUT permite mover el tema a otra categoría existente.

Para buscar: `GET /api/temas?texto=java&categoriaId=1`. Ambos filtros son opcionales y se combinan con AND. El texto busca una subcadena sin distinguir mayúsculas en título o contenido completo; conserva los acentos y trata `%`, `_` y `!` como caracteres literales. Codificar los parámetros en la URL mediante el cliente HTTP. Texto ausente o en blanco no filtra; categoriaId inexistente devuelve `[]`, al igual que cualquier búsqueda sin resultados. El orden siempre es fechaCreacion descendente y, en empate, id descendente. Se devuelve la lista completa, sin paginación.

El SQL de estructura incluye TEMAS con contenido CLOB, clave foránea a CATEGORIAS y AUTOR_ID sin relación SQL con usuarios. Las operaciones SQL LOWER y LIKE sobre CLOB se eligen según la [documentación de Oracle](https://docs.oracle.com/en/database/oracle/oracle-database/21/adlob/supported-functions-and-operators.html).

## Comentarios y eliminación de temas

Todas las rutas requieren identidad vigente. USUARIO y ADMINISTRADOR pueden comentar y consultar; solo el autor del comentario lo edita o elimina. El dueño del tema y un administrador ajeno no adquieren permiso sobre ese comentario.

| Método y ruta | Resultado correcto |
|---|---|
| POST /api/temas/{temaId}/comentarios | 201, comentario y Location |
| GET /api/temas/{temaId}/comentarios | 200, lista del tema ordenada por fecha e ID ascendentes |
| GET /api/comentarios/{id} | 200, comentario |
| PUT /api/comentarios/{id} | 200, comentario actualizado por su autor |
| DELETE /api/comentarios/{id} | 204, sin cuerpo; solo su autor |

POST y PUT reciben exclusivamente:

```json
{"contenido":"Podríamos separar el contrato de su implementación."}
```

Contenido obligatorio, no blanco, de hasta 2000 caracteres; se conservan sus espacios y saltos de línea. La respuesta contiene id, contenido, temaId, autorId y fechaCreacion. El autor proviene de la identidad HTTP. El tema se toma de la ruta al crear y no se puede cambiar al editar; autor y fecha UTC con microsegundos también son inmutables. Campos desconocidos o tipos inválidos producen 400; incluye intentar enviar temaId o autorId en el JSON.

Un tema existente sin comentarios devuelve `[]`; tema o comentario inexistente produce 404. ID inválido produce 400. Modificar un comentario ajeno produce 403 sin cambiarlo ni borrarlo. Eliminar un comentario conserva el tema.

DELETE /api/temas/{id}, autorizado exclusivamente a su autor, elimina el tema y todos sus comentarios mediante ON DELETE CASCADE en la misma transacción. Esto incluye los comentarios de otros autores que pertenecen al tema eliminado. Un borrado rechazado o fallido conserva los datos. Publicar, editar o borrar un comentario y eliminar su tema se coordinan bloqueando la fila del tema durante la escritura. No hay relación Java bidireccional ni entidades expuestas en JSON.

## Datos de demostración

En el esquema Oracle exclusivo de contenido, usar un cliente configurado para **UTF-8** y ejecutar en este orden:

1. [01_estructura.sql](database/01_estructura.sql): crea CATEGORIAS, TEMAS y COMENTARIOS con sus claves e índices. Ejecutar sobre un esquema sin esas tablas; no realiza migraciones de una estructura previa.
2. [02_datos_demostracion.sql](database/02_datos_demostracion.sql): inserta cinco categorías, una sola vez sobre la tabla vacía.
3. [02b_temas_comentarios_demostracion.sql](database/02b_temas_comentarios_demostracion.sql): en SQLcl o SQL*Plus, solicita los IDs numéricos reales de un administrador activo y dos usuarios activos distintos. Inserta tres temas y tres comentarios; ejecutarlo una sola vez con TEMAS y COMENTARIOS vacías.

Obtener los IDs mediante GET /api/usuarios como administrador o GET /api/auth/me de cada cuenta en foro-usuarios. El script no consulta tablas de usuarios, no contiene contraseñas y no presupone IDs 1/2/3. Verificar esas cuentas antes de cargar; al no existir FK entre servicios, el esquema de contenido no comprueba sus roles o estado por SQL. Los vínculos de categorías y temas usan los IDs generados por la base. La carga 02b aborta con rollback si ocurre un error SQL en el cliente indicado.

La carga de datos de ejemplo es opcional. También se pueden crear categorías, temas y comentarios mediante la API. Para probar modificaciones y eliminaciones, utilizar registros de prueba propios.

## Pruebas

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

Las pruebas cubren la conexión local, validación de identidad HTTP, permisos, CRUD de categorías, temas y comentarios, búsqueda en contenido CLOB, validación JSON, unicidad, autoría, eliminación en cascada y operaciones concurrentes. También verifican la carga de los datos de ejemplo.

Las pruebas de integración utilizan H2 y un servidor HTTP de identidad de prueba. No requieren Oracle ni que el servicio real de usuarios esté iniciado. Los informes se generan en `target/surefire-reports/` y `clean verify` genera el JAR ejecutable en `target/`.

## Base de datos y archivos locales

Las tablas pertenecen exclusivamente al esquema de contenido. AUTOR_ID guarda el identificador recibido del servicio de usuarios y no tiene una clave foránea hacia su esquema.

Las [consultas de verificación](database/03_verificacion.sql) permiten revisar cantidades, relaciones y restricciones del esquema.

El repositorio excluye compilados, cachés, variables de entorno, credenciales, Wallet y certificados. Mantener estos archivos fuera del control de versiones.
