# Colección de contenido

Importar foro-contenido.postman_collection.json y local.postman_environment.json. Seleccionar este entorno y definir usuariosBaseUrl, contenidoBaseUrl, adminCorreo y adminPassword privados. El administrador debe existir en usuarios. Los valores secretos están vacíos en los archivos; contenido no necesita la clave de firma JWT.

Ejecutar completa y en orden con **Collection Runner**, una iteración. La colección inicia sesión en usuarios por HTTP, registra sus propias cuentas de prueba y obtiene tokens vigentes. No requiere ejecutar la colección de usuarios antes. Los tokens, IDs y contraseñas de prueba usan pm.variables y duran solo esa ejecución; Send aislado no conserva esas variables entre ejecuciones. No guardar credenciales en exportaciones ni compartir el entorno privado. Evitar mostrar cabeceras y secretos en la consola o video.

Las 54 solicitudes cubren categorías, temas, búsqueda por texto/categoría, caracteres literales, comentarios, autoría, cascada, roles y rechazo de cuenta desactivada. Crean cinco categorías, tres temas y tres comentarios permanentes de demostración; los borrados usan categorías/temas/comentarios adicionales. Solo modifican sus registros nuevos. Cada repetición agrega datos con nombres/correos únicos; ejecutar sobre un entorno destinado a la actividad.

La búsqueda utiliza 100%_ como literal y combina categoría. El dueño del tema y el administrador reciben 403 ante comentarios ajenos. La eliminación del tema propio elimina sus comentarios. La baja de una cuenta temporal se hace mediante usuarios y su token previo deja de acceder a contenido (401). El administrador existente y las dos cuentas activas de demostración se conservan.

54 flujos HTTP se verificaron contra ambos JAR locales sobre H2 mediante un reproductor diagnóstico; JSON y scripts compilan. **El runtime y las aserciones Postman están pendientes**, así como su ejecución con Oracle. Newman no se instaló porque su descarga fue rechazada.

## Comprobaciones manuales con Oracle

1. Iniciar cada aplicación con su esquema dedicado y ejecutar la colección sobre esas instancias. Guardar el resumen del Runner, sin secretos.
2. Ejecutar database/03_verificacion.sql en contenido y el script propio del esquema de usuarios. Comprobar mínimos, tipos, relaciones y cero huérfanos. Probar por API coincidencias al final de un contenido de más de 9500 caracteres, mayúsculas, acentos, % y _.
3. Reiniciar ambas aplicaciones con los mismos esquemas; volver a iniciar sesión y consultar los mismos IDs. Los datos deben persistir. H2 local no sirve para esta comprobación.
4. Conservar temporalmente un Bearer válido en una petición privada; detener solo usuarios e intentar publicar un comentario desde Postman. Debe responder 503. Comparar el conteo SQL antes/después para comprobar cero escrituras; volver a iniciar usuarios. Esta parada no forma parte de la colección automática.
5. Verificar también las carreras de categoría/publicación y tema/comentario en Oracle con sesiones de prueba; los resultados H2 existentes no acreditan esos bloqueos en Oracle.

Referencias: [variables locales de Postman](https://learning.postman.com/docs/use/send-requests/variables/variables) y [Newman](https://learning.postman.com/docs/collections/using-newman-cli/installing-running-newman), herramienta opcional.

El recorrido programa su parada ante estados inesperados, aserciones fallidas o respuestas JSON/estructura inválidas. El login de administrador construye su cuerpo con JSON.stringify para conservar comillas y barras de la contraseña. Esas guardas tienen ocho comprobaciones Node focalizadas compartidas entre las dos colecciones; la verificación en el sandbox real sigue pendiente.
