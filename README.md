# Foro Contenido

Backend de contenido para el sistema de foros de DSY2205, Semana 2. Aplicación Spring Boot con paquete base `cl.duoc.forocontenido`, puerto predeterminado **8082** y repositorio Git independiente.

## Estado actual

Primer bloque: configuración Maven, clase principal, perfiles local/Oracle y prueba de arranque con JDBC. Categorías, temas, comentarios y búsqueda se implementarán en los siguientes bloques. No hay endpoints de negocio todavía; `/` devuelve 404.

Este repositorio contiene su propio pom.xml, Maven Wrapper, código, configuración y pruebas. No existe un proyecto padre del foro ni una dependencia Java o Maven hacia foro-usuarios. Se compila y arranca por separado.

La integración de identidad se añadirá en el siguiente bloque mediante HTTP a GET `/api/auth/me` de foro-usuarios. Contenido verificará los Bearer tokens consultando ese servicio: no emitirá tokens, no compartirá la clave de firma y no accederá a USUARIOS. Sus tablas de categorías, temas y comentarios estarán en un esquema Oracle propio.

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

## Pruebas y documentación

`AplicacionTest` activa local, inicia el contexto real y verifica SELECT 1 mediante JDBC contra H2. Los informes están en target/surefire-reports; clean verify genera el JAR ejecutable propio.

- [Diseño y separación de servicios](docs/diseno.md).
- [Contrato HTTP de identidad](docs/contrato-identidad.md).
- [Plan de implementación de contenido](docs/plan-implementacion.md).
- [Progreso del trabajo](docs/progreso.md).
- [Verificación](docs/verificacion.md).

Rama inicial de trabajo: desarrollo. Los repositorios remotos y su publicación se prepararán con autorización del usuario.
