# Foro Contenido Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. El usuario pide avanzar paso a paso; ejecutar únicamente el bloque correspondiente al paso actual. No publicar automáticamente.

**Goal:** Construir foro-contenido como repositorio independiente con categorías, temas, comentarios y búsquedas persistidos en Oracle.

**Architecture:** Aplicación Spring Boot independiente en cl.duoc.forocontenido. Identidad por HTTP a foro-usuarios y tablas propias en otro esquema Oracle. Las pruebas simulan el servicio HTTP de usuarios para poder compilar y probar sin el otro repositorio.

**Tech Stack:** Java 17, Spring Boot 3.5.16 inicial, Maven Wrapper 3.9.14, Web, Data JPA, Validation, Security, Oracle JDBC, H2 de desarrollo, JUnit, MockMvc y un servidor HTTP local de prueba del JDK.

**Spec:** docs/diseno.md y docs/contrato-identidad.md, incluidos en este repositorio. Rutas interpretadas desde la raíz del repositorio.

## Global Constraints

- Raíz nueva: C:/Users/fasc8/OneDrive/Documentos/DUOC/DESARROLLO DE SOFTWARE III/SEMANA 2/foro-contenido. Rutas siguientes relativas a esta raíz.
- Git propio, sin proyecto padre, sin módulo ni dependencia de compilación a foro-usuarios o BIBLIOTECA.
- Puerto predeterminado 8082. USUARIOS_BASE_URL configura identidad; Oracle por entorno. No compartir clave JWT ni tablas con usuarios.
- Al menos cinco categorías, tres temas y tres comentarios; mantener cantidades al grabar demostración.
- ADMINISTRADOR gestiona categorías; cada autor gestiona su contenido; sin baneos ni moderación ajena.
- Búsqueda combinada por texto y categoriaId; sin frontend, pagos ni infraestructura adicional.
- No publicar repositorios ni entregar al AVA automáticamente. No marcar Oracle completo con pruebas H2.

## Review Focus

1. autorId enviado por un cliente nunca permite suplantar autor: prueba publicacionRechazaAutorDelBody.
2. Servicio de identidad caído, lento o con JSON inválido no autoriza peticiones: pruebas identidadCaidaEs503SinEscritura y identidadMalformadaEs503.
3. El autor de un tema no adquiere permiso de editar comentarios ajenos: prueba autorTemaNoEditaComentarioAjeno.
4. Categoría con temas no se borra; eliminar tema borra comentarios atómicamente: pruebas categoriaEnUsoEs409 y borrarTemaEliminaSusComentarios.
5. Texto con %, _ y caracteres acentuados produce búsqueda definida sin ampliar resultados por comodines: prueba busquedaLiteralCombinada. Los acentos se conservan; no se exige ignorarlos.

## Task 1: Base autónoma

**Files:** Crear pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties, .gitignore, README.md; src/main/java/cl/duoc/forocontenido/ForoContenidoApplication.java; src/main/resources/application.properties, application-local.properties, application-oracle.properties; src/test/java/cl/duoc/forocontenido/AplicacionTest.java; docs/diseno.md y docs/contrato-identidad.md.

**Interfaces:** Aplicación foro-contenido con H2 propio en perfil local y Oracle validate desde ORACLE_URL, ORACLE_USERNAME, ORACLE_PASSWORD. Sin funciones de negocio todavía. Git propio en rama de desarrollo.

- [x] Preparar pom y Wrapper independientes con dependencias básicas; Security se agrega con su configuración en Task 2.
- [x] Escribir prueba @SpringBootTest y @ActiveProfiles("local") que compruebe arranque y SELECT 1; ejecutar .\mvnw.cmd test antes de la clase principal, observar fallo por configuración ausente.
- [x] Crear clase y perfiles; copiar documentación pertinente. No incluir rutas a archivos del otro repositorio en pom, recursos ni scripts de ejecución.
- [x] Ejecutar .\mvnw.cmd clean verify en esta raíz. Esperado: BUILD SUCCESS y JAR propio sin foro-usuarios activo ni disponible como carpeta.
- [x] Registrar verificación del arranque local y commit local, sin push.

## Task 2: Identidad HTTP y errores

**Files:** Crear security/Identidad.java, security/UsuariosClient.java, security/BearerTokenFilter.java, security/SecurityConfig.java, exception/ApiExceptionHandler.java; agregar Security al pom; tests IdentidadHttpTest.java y support/UsuariosStub.java en src/test/java/cl/duoc/forocontenido/.

**Interfaces:** Identidad(Long id, String nombre, String correo, String rol). UsuariosClient.obtener(String bearerToken): Identidad; GET /api/auth/me del contrato. Filtro crea principal local. UsuariosStub usa com.sun.net.httpserver.HttpServer en puerto libre, con respuestas y retardos configurables solo en pruebas.

- [x] Escribir casos contratoMeValido y tokenAusenteEs401, con HTTP stub real. Ejecutar .\mvnw.cmd '-Dtest=IdentidadHttpTest' test antes de implementar y observar el fallo por integración inexistente.
- [x] Implementar cliente con RestClient y timeouts 2/3 segundos configurables, filtro, ProblemDetail y seguridad sin sesiones. Requerir identidad en todos los endpoints de negocio.
- [x] Añadir primero los casos usuarioResponde401, identidadCaidaEs503SinEscritura, identidadMalformadaEs503, identidadTimeoutEs503 y rolDesconocidoEs503. Comprobar estado y ausencia de cambio en datos; no limitarse a verificar llamadas del mock. No cachear ni reintentar autenticación.
- [x] Ejecutar .\mvnw.cmd test; registrar resultado y commit local.

## Task 3: Categorías

**Files:** Crear bajo src/main/java/cl/duoc/forocontenido/: model/Categoria.java, repository/CategoriaRepository.java, dto/CategoriaRequest.java, dto/CategoriaResponse.java, service/CategoriaService.java, controller/CategoriaController.java; tests CategoriasApiTest.java; database/01_estructura.sql.

**Interfaces:** GET/POST /api/categorias; GET/PUT/DELETE /api/categorias/{id}. CategoriaRequest(String nombre, String descripcion). Respuesta id, nombre, descripcion. Consulta autenticada; escritura solo ADMINISTRADOR. Nombre único sin distinguir mayúsculas y espacios exteriores, longitud 1–100; descripción hasta 500.

- [x] Escribir adminCreaCategoria201 y usuarioNoCreaCategoria403; ejecutar clase de prueba antes del controlador.
- [x] Implementar CRUD, DTO, permisos y errores. Normalizar nombre para unicidad sin perder acentos, con restricción equivalente en Oracle. Duplicado → 409; no existe → 404; inválido → 400; borrado válido → 204.
- [x] Añadir pruebas de listado, edición y borrado, duplicados normalizados y campos desconocidos. Sembrar cinco categorías en database/02_datos_demostracion.sql y probar conteo al cargar ese conjunto de datos.
- [x] Ejecutar .\mvnw.cmd test y registrar resultado; commit local.

## Task 4: Temas y búsqueda

**Files:** Crear model/Tema.java, repository/TemaRepository.java, dto/TemaRequest.java, dto/TemaResponse.java, service/TemaService.java, controller/TemaController.java; tests TemasApiTest.java. Ampliar scripts SQL propios y reglas de categoría.

**Interfaces:** TemaRequest(String titulo, String contenido, Long categoriaId). Respuesta id, titulo, contenido, categoriaId, autorId, fechaCreacion. GET/POST /api/temas; GET/PUT/DELETE /api/temas/{id}. TemaService.buscar(String texto, Long categoriaId): List<TemaResponse>. Autor asignado desde Identidad; inmutable al editar.

- [x] Escribir usuarioPublicaTemaConSuIdentidad y buscaPorTextoYCategoria; ejecutar antes de implementar.
- [x] Implementar título 1–200 y contenido 1–10000, no blancos; categoría obligatoria existente. texto busca sin distinguir mayúsculas en título/contenido, escapando % y _ para búsqueda literal; categoriaId se combina con AND. Orden fechaCreacion DESC, id DESC. Sin resultados → arreglo vacío 200.
- [x] Añadir publicacionRechazaAutorDelBody (400), otroAutorNoEditaNiBorra (403), adminNoModeraTemaAjeno (403), categoriaInexistenteEs404AlPublicar, categoriaInexistenteComoFiltroEsVacia, busquedaLiteralCombinada, temaInexistenteEs404, temaSePuedeMoverDeCategoria y categoriaEnUsoEs409. Ejecutar cada caso antes del comportamiento correspondiente.
- [x] Crear relación y constraint de categoría; impedir borrado de categoría en uso con 409, incluyendo carrera entre creación y borrado. No exponer entidades JPA directamente; DTO sin referencias recursivas.
- [x] Ejecutar .\mvnw.cmd test; registrar resultado y commit local.

## Task 5: Comentarios y reglas de eliminación

**Files:** Crear model/Comentario.java, repository/ComentarioRepository.java, dto/ComentarioRequest.java, dto/ComentarioResponse.java, service/ComentarioService.java, controller/ComentarioController.java; tests ComentariosApiTest.java. Ampliar TemaService y scripts SQL.

**Interfaces:** GET/POST /api/temas/{temaId}/comentarios; GET/PUT/DELETE /api/comentarios/{id}. ComentarioRequest(String contenido), respuesta id, contenido, temaId, autorId, fechaCreacion. Contenido 1–2000, no blanco. Al listar orden fechaCreacion ASC, id ASC. El tema es inmutable al editar un comentario.

- [x] Escribir comentaTemaExistenteConAutorAutenticado, autorTemaNoEditaComentarioAjeno y borrarTemaEliminaSusComentarios; ejecutar antes de implementar.
- [x] Implementar CRUD de comentarios, permisos por autor y 404 de relaciones ausentes; edición no acepta temaId ni autorId del body. Usuario o administrador solo modifican sus propios comentarios.
- [x] Implementar eliminación transaccional de tema y sus comentarios, con integridad referencial; añadir borrarTemaAjenoNoBorraComentarios, comentarioVacioEs400, comentarioLargoEs400, borrarComentarioAjenoEs403 y temaInexistenteAlComentarEs404.
- [x] Cargar al menos tres temas y tres comentarios con autores reales del conjunto de usuarios. Los scripts usan ids de autores como parámetros; no suponen que la secuencia de usuarios empiece en 1.
- [x] Ejecutar .\mvnw.cmd clean verify y registrar resultado; commit local.

## Task 6: Oracle, integración y entrega verificable

**Files:** Crear database/03_verificacion.sql, postman/foro-contenido.postman_collection.json, postman/local.postman_environment.json, docs/verificacion.md, docs/matriz-cumplimiento.md, docs/guion-video.md; completar README.md.

**Interfaces:** Código y scripts propios; colección inicia sesión por HTTP en usuarios, usa accessToken solo como variable de ejecución y prueba contenido. No necesita compilar usuarios para construir contenido. El video y la matriz final abarcan ambos repositorios.

- [x] Ejecutar .\mvnw.cmd clean verify sin usuarios activo: suite verde con servidor stub.
- [ ] Preparar esquema Oracle propio de contenido, ejecutar estructura y datos parametrizados; contar cinco categorías, al menos tres temas y tres comentarios. Verificar búsqueda real, incluyendo mayúsculas, acentos, % y _, sobre el tipo de columna elegido para contenido. Ajustar tipo y consultas si la prueba Oracle detecta incompatibilidades.
- [ ] Ejecutar ambas aplicaciones reales con sus esquemas. Probar Postman: login, me, categorías por administrador, publicar, buscar, comentar, editar, eliminar y restricciones 400/401/403/404/409. Desactivar autor en usuarios y comprobar rechazo del token anterior en contenido; detener usuarios y comprobar 503 sin escrituras. Reiniciar servicios y verificar persistencia Oracle.
- [ ] Revisar git diff --check y git status --short de cada repositorio. Confirmar que cada JAR se genera desde su raíz, que no hay dependencia de rutas del otro repositorio y que los README explican la dependencia HTTP en ejecución.
- [ ] Registrar en matriz Git 10, CRUD Oracle 20, consultas de dos servicios 30, JSON/Postman 25 y video 15; ningún criterio se marca completo sin evidencia. Guion Kaltura de 7–10 minutos: diseño/separación, usuarios/login/roles, foro/búsqueda/comentarios, Oracle/persistencia y Git. Preparar ZIP solo con entregables; publicación y AVA quedan para autorización expresa.

## Self-review

Task 1 cubre autonomía de compilación; Task 2 identidad; Tasks 3–5 categorías, publicaciones, búsqueda, comentarios y sus permisos; Task 6 Oracle, datos mínimos, Postman y evidencia de ambos servicios. Review Focus tiene casos explícitos en Tasks 2–5. Dependencia de contrato: solo HTTP, consistente con el plan de usuarios; no hay dependencia de Java, SQL ni JAR entre repositorios.
