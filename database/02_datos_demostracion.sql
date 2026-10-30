-- Ejecutar una sola vez sobre CATEGORIAS vacía, después de 01_estructura.sql.
-- IDs generados por la base; sin credenciales ni dependencias de usuarios.
INSERT INTO CATEGORIAS (NOMBRE, CLAVE_NOMBRE, DESCRIPCION)
VALUES ('Programación', 'PROGRAMACIÓN', 'Lenguajes, algoritmos y desarrollo de software');
INSERT INTO CATEGORIAS (NOMBRE, CLAVE_NOMBRE, DESCRIPCION)
VALUES ('Bases de datos', 'BASES DE DATOS', 'Modelado, SQL y persistencia');
INSERT INTO CATEGORIAS (NOMBRE, CLAVE_NOMBRE, DESCRIPCION)
VALUES ('Tecnología', 'TECNOLOGÍA', 'Herramientas y novedades tecnológicas');
INSERT INTO CATEGORIAS (NOMBRE, CLAVE_NOMBRE, DESCRIPCION)
VALUES ('Videojuegos', 'VIDEOJUEGOS', 'Desarrollo y conversación sobre videojuegos');
INSERT INTO CATEGORIAS (NOMBRE, CLAVE_NOMBRE, DESCRIPCION)
VALUES ('Consultas académicas', 'CONSULTAS ACADÉMICAS', 'Dudas sobre actividades y aprendizaje');
COMMIT;
