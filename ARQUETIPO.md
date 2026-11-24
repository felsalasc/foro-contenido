# Adopcion del arquetipo

Este proyecto se reconstruyo desde la base Maven cl.duoc:foro-microservicio-archetype:1.0.0 y se adapto con el dominio de foro-contenido, commit e982f3978002f5cfb5c019505ee34a179ab0ecc6. La version anterior ya existia: esta es una migracion actual, no una atribucion retrospectiva.

Se conservaron el POM base, Maven Wrapper y estructura de la plantilla. El ejemplo Recurso se sustituyo por las clases, configuracion, SQL y pruebas reales. Se agregaron las dependencias de dominio faltantes (JWT en Usuarios). Las APIs, tablas y puerto 8082 se conservan.

.arquetipo/origen.json registra el SHA256 del POM recien generado y de cada archivo importado. Para reproducir desde los seis clones descritos en foro-despliegue: instala el arquetipo con mvnw.cmd install y ejecuta scripts/Adoptar-Servicios.ps1 desde su repositorio, indicando -SourceRoot la carpeta que contiene SEMANA 2 y SEMANA 5, y -OutputRoot una carpeta NUEVA separada. Requiere Git y JDK 17; -JavaHome permite especificar el JDK. El comando no modifica las fuentes ni despliega servicios.

Antes de promover una nueva copia, ejecuta mvnw.cmd verify en cada servicio generado. Los manifiestos acreditan el proceso de generacion e importacion; las pruebas acreditan comportamiento, no una calificacion garantizada.
