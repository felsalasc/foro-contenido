package cl.duoc.forocontenido;

import cl.duoc.forocontenido.support.UsuariosStub;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TemasApiTest {
    static final UsuariosStub usuarios = crearStub();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    long categoria;
    long otraCategoria;

    static UsuariosStub crearStub() {
        try { return new UsuariosStub(); }
        catch (IOException ex) { throw new IllegalStateException(ex); }
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("usuarios.base-url", usuarios::baseUrl);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:temas_test;MODE=Oracle;DB_CLOSE_DELAY=-1");
    }
    @BeforeEach void reset() throws Exception {
        usuarios.reset();
        if (hayTablaTemas()) jdbc.update("DELETE FROM TEMAS");
        jdbc.update("DELETE FROM CATEGORIAS");
        identidad(7, "ADMINISTRADOR");
        categoria = crearCategoria("Programación");
        otraCategoria = crearCategoria("Bases de datos");
        usuarios.reset();
    }
    @AfterAll static void cerrar() { usuarios.close(); }
    boolean hayTablaTemas() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='TEMAS'", Integer.class) > 0;
    }
    void identidad(long id, String rol) {
        usuarios.respuesta(200, "{\"id\":" + id + ",\"nombre\":\"Ana\",\"correo\":\"ana@example.test\",\"rol\":\"" + rol + "\"}");
    }
    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer tema-prueba");
    }
    MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, String datos) {
        return auth(request).contentType("application/json").content(datos);
    }
    String datos(String titulo, String contenido, long categoriaId) throws Exception {
        return json.writeValueAsString(Map.of("titulo", titulo, "contenido", contenido, "categoriaId", categoriaId));
    }
    long crearCategoria(String nombre) throws Exception {
        return json.readTree(mvc.perform(body(post("/api/categorias"), json.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    long crear(String titulo, String contenido, long categoriaId) throws Exception {
        return json.readTree(mvc.perform(body(post("/api/temas"), datos(titulo, contenido, categoriaId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    long cantidad() { return hayTablaTemas() ? jdbc.queryForObject("SELECT COUNT(*) FROM TEMAS", Long.class) : 0; }

    @Test void usuarioPublicaTemaConSuIdentidad() throws Exception {
        mvc.perform(body(post("/api/temas"), datos("  Consulta Java  ", "Primera línea\nSegunda línea", categoria)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/temas/[0-9]+")))
                .andExpect(jsonPath("$.titulo").value("Consulta Java"))
                .andExpect(jsonPath("$.contenido").value("Primera línea\nSegunda línea"))
                .andExpect(jsonPath("$.categoriaId").value(categoria)).andExpect(jsonPath("$.autorId").value(7))
                .andExpect(jsonPath("$.fechaCreacion").isString()).andExpect(jsonPath("$.categoria").doesNotExist());
        assertThat(cantidad()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT AUTOR_ID FROM TEMAS", Long.class)).isEqualTo(7);
    }

    @Test void buscaPorTextoYCategoria() throws Exception {
        long esperado = crear("Java y Spring", "Ayuda", categoria);
        crear("Java SQL", "Otra categoría", otraCategoria);
        crear("Kotlin", "Sin coincidencia", categoria);
        mvc.perform(auth(get("/api/temas").param("texto", "JAVA").param("categoriaId", "" + categoria)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(esperado));
    }

    @Test void busquedaLiteralCombinada() throws Exception {
        long esperado = crear("Programación 100%_!", "Código en español", categoria);
        crear("Programación 100abc!", "Cualquier cosa", categoria);
        crear("Programación 100%_!", "Otra", otraCategoria);
        mvc.perform(auth(get("/api/temas").param("texto", "100%_!").param("categoriaId", "" + categoria)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(esperado));
        mvc.perform(auth(get("/api/temas").param("texto", "PROGRAMACIÓN"))).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(auth(get("/api/temas").param("texto", "programacion"))).andExpect(content().json("[]"));
    }

    @Test void buscaContenidoClobCompleto() throws Exception {
        long id = crear("Sin coincidencia en título", "x".repeat(9500) + " BÚSQUEDA_SQL% final", categoria);
        mvc.perform(auth(get("/api/temas").param("texto", "búsqueda_sql%")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(auth(get("/api/temas/{id}", id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").value("x".repeat(9500) + " BÚSQUEDA_SQL% final"));
    }

    @Test void ordenFechaDescYDesempateIdDesc() throws Exception {
        long uno = crear("Uno", "Contenido", categoria);
        long dos = crear("Dos", "Contenido", otraCategoria);
        long tres = crear("Tres", "Contenido", categoria);
        jdbc.update("UPDATE TEMAS SET FECHA_CREACION=?", OffsetDateTime.parse("2026-01-01T12:00:00Z"));
        mvc.perform(auth(get("/api/temas"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(tres)).andExpect(jsonPath("$[1].id").value(dos))
                .andExpect(jsonPath("$[2].id").value(uno));
        jdbc.update("UPDATE TEMAS SET FECHA_CREACION=? WHERE ID=?", OffsetDateTime.parse("2026-01-02T12:00:00Z"), uno);
        mvc.perform(auth(get("/api/temas").param("texto", "contenido"))).andExpect(jsonPath("$[0].id").value(uno));
    }

    @Test void sinResultadosYFiltroCategoria() throws Exception {
        long id = crear("Tema", "Contenido", categoria);
        mvc.perform(auth(get("/api/temas").param("categoriaId", "" + categoria)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(auth(get("/api/temas").param("categoriaId", "" + otraCategoria))).andExpect(content().json("[]"));
        mvc.perform(auth(get("/api/temas").param("texto", "ausente"))).andExpect(content().json("[]"));
        mvc.perform(auth(get("/api/temas").param("texto", "  "))).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void categoriaInexistenteComoFiltroEsVacia() throws Exception {
        crear("Tema", "Contenido", categoria);
        mvc.perform(auth(get("/api/temas").param("categoriaId", "999999")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(auth(get("/api/temas").param("categoriaId", "999999").param("texto", "Tema")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void publicacionRechazaAutorDelBody() throws Exception {
        String request = datos("Tema", "Contenido", categoria).replace("}", ",\"autorId\":99}");
        mvc.perform(body(post("/api/temas"), request)).andExpect(status().isBadRequest());
        assertThat(cantidad()).isZero();
    }

    @Test void otroAutorNoEditaNiBorra() throws Exception {
        long id = crear("Original", "Contenido", categoria);
        identidad(8, "USUARIO");
        mvc.perform(body(put("/api/temas/{id}", id), datos("Ajeno", "Cambio", otraCategoria))).andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/temas/{id}", id))).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/temas/{id}", id))).andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Original"));
        assertThat(cantidad()).isEqualTo(1);
    }

    @Test void adminNoModeraTemaAjeno() throws Exception {
        long id = crear("Original", "Contenido", categoria);
        identidad(8, "ADMINISTRADOR");
        mvc.perform(body(put("/api/temas/{id}", id), datos("Ajeno", "Cambio", categoria))).andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/temas/{id}", id))).andExpect(status().isForbidden());
        assertThat(cantidad()).isEqualTo(1);
    }

    @Test void fechaCreacionNoCambiaEntrePublicacionConsultaYEdicion() throws Exception {
        var creado = json.readTree(mvc.perform(body(post("/api/temas"), datos("Original", "Texto", categoria)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id = creado.get("id").asLong();
        String fecha = creado.get("fechaCreacion").asText();
        var consultado = json.readTree(mvc.perform(auth(get("/api/temas/{id}", id))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(consultado.get("fechaCreacion").asText()).isEqualTo(fecha);
        mvc.perform(body(put("/api/temas/{id}", id), datos("Actualizado", "Otro texto", categoria)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fechaCreacion").value(fecha));
    }

    @Test void temaSePuedeMoverDeCategoria() throws Exception {
        long id = crear("Original", "Contenido", categoria);
        var anterior = jdbc.queryForMap("SELECT AUTOR_ID, FECHA_CREACION FROM TEMAS WHERE ID=?", id);
        mvc.perform(body(put("/api/temas/{id}", id), datos("Actualizado", "Nuevo contenido", otraCategoria)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoriaId").value(otraCategoria)).andExpect(jsonPath("$.autorId").value(7));
        assertThat(jdbc.queryForMap("SELECT AUTOR_ID, FECHA_CREACION FROM TEMAS WHERE ID=?", id)).isEqualTo(anterior);
        identidad(7, "ADMINISTRADOR");
        mvc.perform(auth(delete("/api/categorias/{id}", categoria))).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/api/categorias/{id}", otraCategoria))).andExpect(status().isConflict());
    }

    @Test void autorBorraYLiberaCategoria() throws Exception {
        long id = crear("Original", "Contenido", categoria);
        identidad(7, "ADMINISTRADOR"); // Cambiar de rol no cambia la propiedad del tema.
        mvc.perform(auth(delete("/api/temas/{id}", id))).andExpect(status().isNoContent());
        assertThat(cantidad()).isZero();
        mvc.perform(auth(delete("/api/categorias/{id}", categoria))).andExpect(status().isNoContent());
    }

    @Test void categoriaEnUsoEs409() throws Exception {
        crear("Original", "Contenido", categoria);
        identidad(7, "ADMINISTRADOR");
        mvc.perform(auth(delete("/api/categorias/{id}", categoria))).andExpect(status().isConflict());
        assertThat(cantidad()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CATEGORIAS WHERE ID=?", Long.class, categoria)).isEqualTo(1);
    }

    @Test void categoriaInexistenteEs404AlPublicar() throws Exception {
        mvc.perform(body(post("/api/temas"), datos("Tema", "Contenido", 999999))).andExpect(status().isNotFound());
        assertThat(cantidad()).isZero();
    }

    @Test void categoriaInexistenteAlEditarNoModificaTema() throws Exception {
        long id = crear("Original", "Contenido", categoria);
        mvc.perform(body(put("/api/temas/{id}", id), datos("Cambio", "Cambio", 999999))).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/temas/{id}", id))).andExpect(jsonPath("$.titulo").value("Original"));
    }

    @Test void temaInexistenteEs404() throws Exception {
        mvc.perform(auth(get("/api/temas/999999"))).andExpect(status().isNotFound());
        mvc.perform(body(put("/api/temas/999999"), datos("Tema", "Contenido", categoria))).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/temas/999999"))).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"titulo\":\"\",\"contenido\":\"Texto\",\"categoriaId\":%d}",
            "{\"titulo\":\"  \",\"contenido\":\"Texto\",\"categoriaId\":%d}",
            "{\"titulo\":\"Tema\",\"contenido\":\"  \",\"categoriaId\":%d}",
            "{\"titulo\":\"Tema\",\"contenido\":null,\"categoriaId\":%d}",
            "{\"titulo\":123,\"contenido\":\"Texto\",\"categoriaId\":%d}",
            "{\"titulo\":\"Tema\",\"contenido\":false,\"categoriaId\":%d}",
            "{\"titulo\":\"Tema\",\"contenido\":\"Texto\",\"categoriaId\":\"1\"}",
            "{\"titulo\":\"Tema\",\"contenido\":\"Texto\",\"categoriaId\":1.5}",
            "{\"titulo\":\"Tema\",\"contenido\":\"Texto\",\"categoriaId\":0}",
            "{\"titulo\":\"Tema\",\"contenido\":\"Texto\",\"categoriaId\":%d,\"fechaCreacion\":\"2020\"}", "[]", "{"})
    void entradaInvalidaEs400(String request) throws Exception {
        mvc.perform(body(post("/api/temas"), request.replace("%d", "" + categoria))).andExpect(status().isBadRequest());
        assertThat(cantidad()).isZero();
    }

    @Test void limitesDeLongitud() throws Exception {
        crear("T".repeat(200), "Á".repeat(10000), categoria);
        mvc.perform(body(post("/api/temas"), datos("T".repeat(201), "Texto", categoria))).andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/temas"), datos("Tema", "x".repeat(10001), categoria))).andExpect(status().isBadRequest());
        assertThat(cantidad()).isEqualTo(1);
    }

    @Test void edicionRechazaCambioDeAutorYFecha() throws Exception {
        long id = crear("Original", "Contenido", categoria);
        for (String extra : List.of("\"autorId\":99", "\"fechaCreacion\":\"2020-01-01T00:00:00Z\"", "\"id\":99")) {
            mvc.perform(body(put("/api/temas/{id}", id), datos("Cambio", "Cambio", categoria).replace("}", "," + extra + "}")))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(auth(get("/api/temas/{id}", id))).andExpect(jsonPath("$.titulo").value("Original"));
    }

    @ParameterizedTest @ValueSource(strings = {"abc", "0", "-1", "999999999999999999999999"})
    void filtroEIdInvalidoEs400(String valor) throws Exception {
        mvc.perform(auth(get("/api/temas/" + valor))).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/temas").param("categoriaId", valor))).andExpect(status().isBadRequest());
    }

    @Test void sinIdentidadNoPublica() throws Exception {
        mvc.perform(post("/api/temas").contentType("application/json").content(datos("Tema", "Texto", categoria)))
                .andExpect(status().isUnauthorized());
        usuarios.respuesta(503, "privado");
        mvc.perform(body(post("/api/temas"), datos("Tema", "Texto", categoria))).andExpect(status().isServiceUnavailable());
        assertThat(cantidad()).isZero();
    }

    @Test void borradoPendienteImpidePublicarEnCategoriaEliminada() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var conexion = jdbc.getDataSource().getConnection()) {
            conexion.setAutoCommit(false);
            try {
                try (var sentencia = conexion.prepareStatement("DELETE FROM CATEGORIAS WHERE ID=?")) {
                    sentencia.setLong(1, categoria);
                    sentencia.executeUpdate();
                }
                var publicacion = executor.submit(() -> mvc.perform(body(post("/api/temas"), datos("Carrera", "Texto", categoria)))
                        .andReturn().getResponse().getStatus());
                Integer antesDelCommit = null;
                try { antesDelCommit = publicacion.get(500, TimeUnit.MILLISECONDS); }
                catch (TimeoutException esperado) { /* La operación debe esperar la resolución del borrado. */ }
                conexion.commit();
                int resultado = antesDelCommit == null ? publicacion.get(10, TimeUnit.SECONDS) : antesDelCommit;
                assertThat(resultado).isIn(404, 409);
                assertThat(cantidad()).isZero();
            } finally { conexion.rollback(); }
        } finally { executor.shutdownNow(); }
    }

    @Test void carreraPublicacionYBorradoNoDejaHuerfanos() throws Exception {
        identidad(7, "ADMINISTRADOR");
        var executor = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(1);
        try {
            var publicacion = executor.submit(() -> { inicio.await(); return mvc.perform(body(post("/api/temas"), datos("Carrera", "Texto", categoria))).andReturn().getResponse().getStatus(); });
            var borrado = executor.submit(() -> { inicio.await(); return mvc.perform(auth(delete("/api/categorias/{id}", categoria))).andReturn().getResponse().getStatus(); });
            inicio.countDown();
            int post = publicacion.get(15, TimeUnit.SECONDS);
            int delete = borrado.get(15, TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TEMAS t LEFT JOIN CATEGORIAS c ON c.ID=t.CATEGORIA_ID WHERE c.ID IS NULL", Long.class))
                    .as("Sin huérfanos; claves externas: %s", jdbc.queryForList("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME='TEMAS' AND CONSTRAINT_TYPE='FOREIGN KEY'"))
                    .isZero();
            assertThat(List.of(post, delete)).isIn(List.of(201, 409), List.of(404, 204), List.of(409, 204));
            if (post == 201) assertThat(cantidad()).isEqualTo(1);
            else assertThat(cantidad()).isZero();
        } finally { executor.shutdownNow(); }
    }
}
