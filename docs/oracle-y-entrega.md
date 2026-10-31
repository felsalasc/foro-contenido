# Oracle y demostración de contenido

Preparación del 31 de octubre de 2026. Oracle real y Postman real están pendientes. El usuario confirmó BIBLIOTECA activa y que faltan los dos esquemas. La existencia del Wallet local no acredita conexión desde las aplicaciones. Esta preparación no modificó Oracle; el siguiente paso es crear los usuarios/esquemas dedicados.

## Esquema exclusivo

Si no existe, el administrador de la base puede preparar el esquema. Propuesta para Autonomous Database con tablespace DATA; adaptar si la instancia real usa otro. Introducir la contraseña privadamente, sin guardarla con el código:

```sql
CREATE USER FORO_CONTENIDO IDENTIFIED BY "<CLAVE_PRIVADA>"
  DEFAULT TABLESPACE DATA QUOTA 20M ON DATA;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO FORO_CONTENIDO;
```

No ejecutar si ya existe. No conceder DBA ni acceso a USUARIOS. Conectar como FORO_CONTENIDO, comprobar SESSION_USER y ejecutar database/01_estructura.sql una sola vez sobre un esquema sin esas tablas. No ejecutar DDL como ADMIN ni alterar LIBROS. Si el esquema ya contiene versiones anteriores, revisar la estructura antes de migrar; estos scripts no son una migración destructiva.

Referencia: [creación de usuarios de Autonomous Database](https://docs.public.content.oci.oraclecloud.com/iaas/autonomous-database-shared/doc/manage-users-create.html). El SQL es una propuesta preparada y no ejecutada.

## Configuración y datos

Seguir README para ORACLE_URL, ORACLE_USERNAME y ORACLE_PASSWORD de contenido y perfil oracle. USUARIOS_BASE_URL apunta a la aplicación real de usuarios, normalmente http://localhost:8081. No definir JWT_SECRET_BASE64 en contenido. Cada servidor usa su propia terminal, puerto y credenciales de esquema.

Elegir un recorrido de carga:

- SQL: ejecutar 02_datos_demostracion.sql y luego 02b_temas_comentarios_demostracion.sql en SQLcl/SQL*Plus con UTF-8. Proporcionar los IDs reales de un administrador y dos usuarios activos de la API de usuarios. El segundo script presupone temas/comentarios vacíos.
- API: ejecutar la colección de postman/ con un administrador existente. Crea sus propias cuentas, cinco categorías, tres temas y tres comentarios; conserva esos mínimos tras los borrados temporales. Cada ejecución agrega datos únicos. No es necesario ejecutar antes la colección de usuarios.

Ambas rutas deben terminar con al menos cinco categorías, tres temas y tres comentarios. Las autorías se verifican contra los IDs reales de usuarios por HTTP, sin FK ni JOIN entre esquemas. No cargar identificadores inventados desde el test H2.

## Evidencia real pendiente

1. Ejecutar la colección en Postman sobre los dos perfiles Oracle; guardar resumen y respuestas. Probar además búsqueda al final de contenido de más de 9500 caracteres, mayúsculas, acentos, % y _; ninguna conversión debe truncar el CLOB.
2. Ejecutar database/03_verificacion.sql: cantidades, IDs, tipos, constraints y cero huérfanos. Confirmar CASCADE solo para comentarios; categoría con temas debe responder 409.
3. Reiniciar ambos servidores conservando esquema y configuración; iniciar sesión de nuevo y consultar los mismos registros. Guardar los conteos antes/después y ejemplos de datos persistidos.
4. Desactivar una cuenta temporal en usuarios y comprobar 401 con su token anterior en contenido. Detener usuarios y solicitar una escritura con token válido: 503 y conteo Oracle sin cambios. Reiniciar usuarios al terminar.
5. Ejecutar pruebas con sesiones concurrentes de publicar/borrar categoría y comentar/borrar tema. Las validaciones H2 no acreditan el bloqueo real Oracle.
6. Completar matriz y guion con los resultados observados. En la grabación usar registros extra para borrados y conservar mínimos.

Los repositorios remotos y la grabación requieren trabajo posterior; no se han publicado. El ZIP final se prepara cuando se cierre la verificación Oracle, excluyendo Wallet, variables privadas, certificados, cachés y compilados de diagnóstico.
