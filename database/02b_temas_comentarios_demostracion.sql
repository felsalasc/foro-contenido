-- SQLcl o SQL*Plus, archivo UTF-8. Ejecutar una sola vez sobre TEMAS y
-- COMENTARIOS vacías, después de 01_estructura.sql y 02_datos_demostracion.sql.
-- Obtener los tres IDs de cuentas activas de foro-usuarios por GET /api/usuarios
-- como administrador o por GET /api/auth/me de cada cuenta. No se consultan sus
-- tablas y no se presuponen IDs 1/2/3. Estos parámetros son IDs, no contraseñas.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
ACCEPT AUTOR_ADMIN NUMBER PROMPT 'ID real de administrador activo: '
ACCEPT AUTOR_USUARIO_1 NUMBER PROMPT 'ID real del primer usuario activo: '
ACCEPT AUTOR_USUARIO_2 NUMBER PROMPT 'ID real del segundo usuario activo: '

INSERT INTO TEMAS (TITULO, CONTENIDO, CATEGORIA_ID, AUTOR_ID, FECHA_CREACION)
VALUES ('Consulta sobre interfaces Java', '¿Cómo organizar las interfaces de un servicio Spring?',
    (SELECT ID FROM CATEGORIAS WHERE CLAVE_NOMBRE = 'PROGRAMACIÓN'), &AUTOR_USUARIO_1, CURRENT_TIMESTAMP);
INSERT INTO TEMAS (TITULO, CONTENIDO, CATEGORIA_ID, AUTOR_ID, FECHA_CREACION)
VALUES ('Búsqueda literal 100%_SQL', 'Prueba de texto con acentos, porcentaje 100% y guion_bajo.',
    (SELECT ID FROM CATEGORIAS WHERE CLAVE_NOMBRE = 'BASES DE DATOS'), &AUTOR_USUARIO_2, CURRENT_TIMESTAMP);
INSERT INTO TEMAS (TITULO, CONTENIDO, CATEGORIA_ID, AUTOR_ID, FECHA_CREACION)
VALUES ('Organización de consultas académicas', 'Compartamos dudas y recursos de aprendizaje de la semana.',
    (SELECT ID FROM CATEGORIAS WHERE CLAVE_NOMBRE = 'CONSULTAS ACADÉMICAS'), &AUTOR_ADMIN, CURRENT_TIMESTAMP);

INSERT INTO COMENTARIOS (CONTENIDO, TEMA_ID, AUTOR_ID, FECHA_CREACION)
SELECT 'Separar el contrato de su implementación ayuda a organizar el servicio.', ID, &AUTOR_ADMIN, CURRENT_TIMESTAMP
FROM TEMAS WHERE TITULO = 'Consulta sobre interfaces Java';
INSERT INTO COMENTARIOS (CONTENIDO, TEMA_ID, AUTOR_ID, FECHA_CREACION)
SELECT 'También conviene definir DTO para las entradas y respuestas.', ID, &AUTOR_USUARIO_2, CURRENT_TIMESTAMP
FROM TEMAS WHERE TITULO = 'Consulta sobre interfaces Java';
INSERT INTO COMENTARIOS (CONTENIDO, TEMA_ID, AUTOR_ID, FECHA_CREACION)
SELECT 'La búsqueda debe tratar el porcentaje y el guion bajo como caracteres literales.', ID, &AUTOR_USUARIO_1, CURRENT_TIMESTAMP
FROM TEMAS WHERE TITULO = 'Búsqueda literal 100%_SQL';
COMMIT;
