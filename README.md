# Foro Contenido

Backend de contenido para el sistema de foros de DSY2205, Semana 2. Aplicación Spring Boot con paquete base `cl.duoc.forocontenido`, puerto predeterminado **8082** y repositorio Git independiente.

## Estado actual

Base Maven y autenticación HTTP implementadas. Las rutas requieren Bearer y contenido consulta la identidad vigente en usuarios antes de ejecutarlas. Categorías, temas, comentarios y búsqueda se implementarán en los siguientes bloques. Todavía no hay endpoints de negocio: sin token `/` devuelve 401; con identidad válida llega al 404 de la ruta aún inexistente.

Este repositorio contiene su propio pom.xml, Maven Wrapper, código, configuración y pruebas. No existe un proyecto padre del foro ni una dependencia Java o Maven hacia foro-usuarios. Se compila y arranca por separado.

La identidad se verifica mediante HTTP a GET `/api/auth/me` de foro-usuarios. Contenido no emite ni descifra JWT, no necesita JWT_SECRET_BASE64 y no accede a USUARIOS. Sus tablas de categorías, temas y comentarios estarán en un esquema Oracle propio.

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

## Desarrollo local

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

El perfil predeterminado es local. H2 en memoria usa una base propia, foro_contenido, y modo Oracle; los datos se pierden al detener la aplicación. Hibernate usa create-drop y la consola H2 está deshabilitada. No acredita conexión real a Oracle.

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

Servicio, ruta y usuario del ejemplo deben ajustarse a la instancia real. Oracle usa ddl-auto=validate y no ejecuta scripts automáticamente. Los scripts de contenido se añadirán con sus entidades. La conexión Oracle está pendiente de comprobación.

No hay valores predeterminados de credenciales Oracle. Spring Boot no carga archivos .env automáticamente. Wallet, variables privadas y certificados quedan fuera de Git y de la entrega.

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

Cada petición vuelve a consultar usuarios. Los cambios de rol y la desactivación de cuentas se reflejan sin esperar a que expire el token. La API usa autenticación stateless; no hay sesiones, cookies de autenticación, formulario ni HTTP Basic. La cuenta en memoria predeterminada de Spring Boot está deshabilitada. No se configura CORS abierto; el alcance sigue siendo solo backend.

## Pruebas y documentación

`AplicacionTest` activa local, inicia el contexto real y verifica SELECT 1 mediante JDBC contra H2. Los informes están en target/surefire-reports; clean verify genera el JAR ejecutable propio.

`IdentidadHttpTest` usa un servidor HTTP local de prueba en un puerto libre, servicios reales y H2. Comprueba identidad válida, rechazo de tokens, permisos, cambios de rol, redirecciones, demoras de cabeceras/cuerpo, caída de conexión y JSON inválido. Un controlador y una tabla exclusivos de test permiten verificar que las operaciones rechazadas no escriben datos; no forman parte del JAR ni del esquema Oracle. La suite no requiere que el otro repositorio exista o esté iniciado.

- [Diseño y separación de servicios](docs/diseno.md).
- [Contrato HTTP de identidad](docs/contrato-identidad.md).
- [Plan de implementación de contenido](docs/plan-implementacion.md).
- [Progreso del trabajo](docs/progreso.md).
- [Verificación](docs/verificacion.md).

Rama inicial de trabajo: desarrollo. Los repositorios remotos y su publicación se prepararán con autorización del usuario.
