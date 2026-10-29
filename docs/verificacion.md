# Verificación de la base de Foro Contenido

Fecha: 29 de octubre de 2026. Alcance: Task 1 del plan, base autónoma. No constituye aún el desarrollo funcional completo del foro ni acredita Oracle.

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

El JAR se inició con perfil local, loopback 127.0.0.1 y puerto asignado automáticamente. Se comprobó su respuesta HTTP y se cerró el proceso al terminar. En este bloque no existe cliente HTTP a usuarios ni se necesita una clave JWT.

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

Consultar el puerto de Tomcat y solicitar `/`: durante este bloque devuelve 404. Detener con Ctrl+C. La futura seguridad cambiará la respuesta anónima de las rutas protegidas; ese comportamiento se verificará en Task 2.

Las pruebas Maven se ejecutaron con permisos autorizados por las restricciones de instrumentación Java del entorno. OpenJDK emitió la advertencia conocida de class sharing; la suite terminó sin errores. Logs locales: .verificacion/base-red.log, .verificacion/base-verify.log y .verificacion/base-http.log. Informes JUnit: target/surefire-reports/. Estos artefactos están excluidos de Git.

## Pendientes

Cliente HTTP de identidad y seguridad, categorías, temas, comentarios, búsqueda, scripts SQL, datos de demostración, colección Postman y conexión/verificación Oracle. No se implementa frontend.
