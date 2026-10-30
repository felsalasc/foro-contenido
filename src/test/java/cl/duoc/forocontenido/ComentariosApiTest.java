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
class ComentariosApiTest {
    static final UsuariosStub usuarios = crearStub();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    long tema;
    long otroTema;

    static UsuariosStub crearStub() {
        try { return new UsuariosStub(); }
        catch (IOException ex) { throw new IllegalStateException(ex); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("usuarios.base-url", usuarios::baseUrl);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:comentarios_test;MODE=Oracle;DB_CLOSE_DELAY=-1");
    }
    @BeforeEach void reset() throws Exception {
        usuarios.reset();
        if (hayComentarios()) jdbc.update("DELETE FROM COMENTARIOS");
        jdbc.update("DELETE FROM TEMAS");
        jdbc.update("DELETE FROM CATEGORIAS");
        identidad(7, "ADMINISTRADOR");
        var categoria = json.readTree(mvc.perform(body(post("/api/categorias"), "{\"nombre\":\"Programación\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        usuarios.reset();
        tema = crearTema(categoria, "Tema original");
        otroTema = crearTema(categoria, "Otro tema");
    }
    @AfterAll static void cerrar() { usuarios.close(); }
    boolean hayComentarios() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='COMENTARIOS'", Integer.class) > 0;
    }
    void identidad(long id, String rol) {
        usuarios.respuesta(200, "{\"id\":" + id + ",\"nombre\":\"Ana\",\"correo\":\"ana@example.test\",\"rol\":\"" + rol + "\"}");
    }
    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer comentario-prueba");
    }
    MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, String datos) {
        return auth(request).contentType("application/json").content(datos);
    }
    String datos(String contenido) throws Exception { return json.writeValueAsString(Map.of("contenido", contenido)); }
    long crearTema(long categoria, String titulo) throws Exception {
        return json.readTree(mvc.perform(body(post("/api/temas"), json.writeValueAsString(Map.of("titulo", titulo, "contenido", "Texto", "categoriaId", categoria))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    long crear(long temaId, String contenido) throws Exception {
        return json.readTree(mvc.perform(body(post("/api/temas/{id}/comentarios", temaId), datos(contenido)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    long cantidad() { return hayComentarios() ? jdbc.queryForObject("SELECT COUNT(*) FROM COMENTARIOS", Long.class) : 0; }

    @Test void comentaTemaExistenteConAutorAutenticado() throws Exception {
        identidad(8, "USUARIO");
        mvc.perform(body(post("/api/temas/{id}/comentarios", tema), datos("Primera línea\nSegunda línea")))
                .andExpect(status().isCreated()).andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/comentarios/[0-9]+")))
                .andExpect(jsonPath("$.contenido").value("Primera línea\nSegunda línea"))
                .andExpect(jsonPath("$.temaId").value(tema)).andExpect(jsonPath("$.autorId").value(8))
                .andExpect(jsonPath("$.fechaCreacion").isString()).andExpect(jsonPath("$.tema").doesNotExist());
        assertThat(cantidad()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT AUTOR_ID FROM COMENTARIOS", Long.class)).isEqualTo(8);
    }

    @Test void autorTemaNoEditaComentarioAjeno() throws Exception {
        identidad(8, "USUARIO");
        long id = crear(tema, "Comentario ajeno");
        identidad(7, "USUARIO");
        mvc.perform(body(put("/api/comentarios/{id}", id), datos("Cambio"))).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/comentarios/{id}", id))).andExpect(jsonPath("$.contenido").value("Comentario ajeno"));
    }

    @Test void borrarTemaEliminaSusComentarios() throws Exception {
        crear(tema, "Uno");
        identidad(8, "USUARIO");
        crear(tema, "De otro autor");
        long conservado = crear(otroTema, "De otro tema");
        identidad(7, "USUARIO");
        mvc.perform(auth(delete("/api/temas/{id}", tema))).andExpect(status().isNoContent());
        assertThat(cantidad()).isEqualTo(1);
        mvc.perform(auth(get("/api/comentarios/{id}", conservado))).andExpect(status().isOk());
        mvc.perform(auth(get("/api/temas/{id}/comentarios", tema))).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM COMENTARIOS WHERE TEMA_ID=?", Long.class, tema)).isZero();
    }

    @Test void borrarTemaAjenoNoBorraComentarios() throws Exception {
        long id = crear(tema, "Conservar");
        identidad(8, "ADMINISTRADOR");
        mvc.perform(auth(delete("/api/temas/{id}", tema))).andExpect(status().isForbidden());
        assertThat(cantidad()).isEqualTo(1);
        mvc.perform(auth(get("/api/comentarios/{id}", id))).andExpect(status().isOk());
    }

    @Test void borrarComentarioAjenoEs403() throws Exception {
        identidad(8, "USUARIO");
        long id = crear(tema, "Conservar");
        identidad(7, "USUARIO"); // El dueño del tema tampoco puede borrar directamente el comentario.
        mvc.perform(auth(delete("/api/comentarios/{id}", id))).andExpect(status().isForbidden());
        identidad(9, "ADMINISTRADOR");
        mvc.perform(auth(delete("/api/comentarios/{id}", id))).andExpect(status().isForbidden());
        mvc.perform(body(put("/api/comentarios/{id}", id), datos("Cambio"))).andExpect(status().isForbidden());
        assertThat(cantidad()).isEqualTo(1);
    }

    @Test void autorEditaYBorraSuComentario() throws Exception {
        identidad(8, "ADMINISTRADOR");
        var creado = json.readTree(mvc.perform(body(post("/api/temas/{id}/comentarios", tema), datos("Original")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id = creado.get("id").asLong();
        String fecha = creado.get("fechaCreacion").asText();
        identidad(8, "USUARIO");
        mvc.perform(auth(get("/api/comentarios/{id}", id))).andExpect(jsonPath("$.fechaCreacion").value(fecha));
        mvc.perform(body(put("/api/comentarios/{id}", id), datos("Actualizado")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").value("Actualizado"))
                .andExpect(jsonPath("$.temaId").value(tema)).andExpect(jsonPath("$.autorId").value(8))
                .andExpect(jsonPath("$.fechaCreacion").value(fecha));
        mvc.perform(auth(delete("/api/comentarios/{id}", id))).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(auth(get("/api/comentarios/{id}", id))).andExpect(status().isNotFound());
        assertThat(cantidad()).isZero();
        mvc.perform(auth(get("/api/temas/{id}", tema))).andExpect(status().isOk());
    }

    @Test void listadoSoloDelTemaOrdenadoPorFechaEIdAsc() throws Exception {
        long uno = crear(tema, "Uno");
        long dos = crear(tema, "Dos");
        crear(otroTema, "No incluido");
        jdbc.update("UPDATE COMENTARIOS SET FECHA_CREACION=?", OffsetDateTime.parse("2026-01-02T00:00:00Z"));
        mvc.perform(auth(get("/api/temas/{id}/comentarios", tema))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(uno)).andExpect(jsonPath("$[1].id").value(dos));
        jdbc.update("UPDATE COMENTARIOS SET FECHA_CREACION=? WHERE ID=?", OffsetDateTime.parse("2026-01-01T00:00:00Z"), dos);
        mvc.perform(auth(get("/api/temas/{id}/comentarios", tema))).andExpect(jsonPath("$[0].id").value(dos));
    }

    @Test void temaSinComentariosDevuelveListaVacia() throws Exception {
        mvc.perform(auth(get("/api/temas/{id}/comentarios", tema))).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void comentarioVacioEs400() throws Exception {
        mvc.perform(body(post("/api/temas/{id}/comentarios", tema), datos("  \n\t"))).andExpect(status().isBadRequest());
        assertThat(cantidad()).isZero();
    }

    @Test void comentarioLargoEs400() throws Exception {
        mvc.perform(body(post("/api/temas/{id}/comentarios", tema), datos("x".repeat(2001)))).andExpect(status().isBadRequest());
        crear(tema, "Á".repeat(2000));
        assertThat(cantidad()).isEqualTo(1);
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"contenido\":null}", "{\"contenido\":123}", "{\"contenido\":true}",
            "{\"contenido\":\"Texto\",\"autorId\":99}", "{\"contenido\":\"Texto\",\"temaId\":99}",
            "{\"contenido\":\"Texto\",\"fechaCreacion\":\"2020\"}", "{\"contenido\":\"Texto\",\"id\":99}", "[]", "{", "{\"contenido\":\"Texto\"} {}"})
    void entradaInvalidaEs400(String request) throws Exception {
        mvc.perform(body(post("/api/temas/{id}/comentarios", tema), request)).andExpect(status().isBadRequest());
        assertThat(cantidad()).isZero();
    }

    @Test void edicionNoAceptaTemaAutorNiFecha() throws Exception {
        long id = crear(tema, "Original");
        for (String extra : List.of("\"temaId\":" + otroTema, "\"autorId\":99", "\"fechaCreacion\":\"2020\"", "\"id\":99"))
            mvc.perform(body(put("/api/comentarios/{id}", id), "{\"contenido\":\"Cambio\"," + extra + "}"))
                    .andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/comentarios/{id}", id))).andExpect(jsonPath("$.contenido").value("Original"))
                .andExpect(jsonPath("$.temaId").value(tema));
    }

    @Test void edicionInvalidaNoModificaComentario() throws Exception {
        long id = crear(tema, "Original");
        mvc.perform(body(put("/api/comentarios/{id}", id), datos(" "))).andExpect(status().isBadRequest());
        mvc.perform(body(put("/api/comentarios/{id}", id), datos("x".repeat(2001)))).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/comentarios/{id}", id))).andExpect(jsonPath("$.contenido").value("Original"));
    }

    @Test void temaInexistenteAlComentarEs404() throws Exception {
        mvc.perform(body(post("/api/temas/999999/comentarios"), datos("Texto"))).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/temas/999999/comentarios"))).andExpect(status().isNotFound());
        assertThat(cantidad()).isZero();
    }

    @Test void comentarioInexistenteEs404() throws Exception {
        mvc.perform(auth(get("/api/comentarios/999999"))).andExpect(status().isNotFound());
        mvc.perform(body(put("/api/comentarios/999999"), datos("Texto"))).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/comentarios/999999"))).andExpect(status().isNotFound());
    }

    @ParameterizedTest @ValueSource(strings = {"abc", "0", "-1", "999999999999999999999999"})
    void idInvalidoEs400(String id) throws Exception {
        mvc.perform(auth(get("/api/comentarios/" + id))).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/temas/" + id + "/comentarios"))).andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/temas/" + id + "/comentarios"), datos("Texto"))).andExpect(status().isBadRequest());
    }

    @Test void identidadAusenteOCaidaNoEscribe() throws Exception {
        mvc.perform(post("/api/temas/{id}/comentarios", tema).contentType("application/json").content(datos("Texto")))
                .andExpect(status().isUnauthorized());
        usuarios.respuesta(503, "privado");
        mvc.perform(body(post("/api/temas/{id}/comentarios", tema), datos("Texto"))).andExpect(status().isServiceUnavailable());
        assertThat(cantidad()).isZero();
    }

    @Test void borradoFallidoConservaTemaYComentarios() throws Exception {
        long id = crear(tema, "Conservar");
        jdbc.execute("CREATE TABLE PRUEBA_REFERENCIA_TEMA (TEMA_ID BIGINT REFERENCES TEMAS(ID))");
        try {
            jdbc.update("INSERT INTO PRUEBA_REFERENCIA_TEMA VALUES (?)", tema);
            mvc.perform(auth(delete("/api/temas/{id}", tema))).andExpect(status().isConflict());
            assertThat(cantidad()).isEqualTo(1);
            mvc.perform(auth(get("/api/temas/{id}", tema))).andExpect(status().isOk());
            mvc.perform(auth(get("/api/comentarios/{id}", id))).andExpect(status().isOk());
        } finally { jdbc.execute("DROP TABLE PRUEBA_REFERENCIA_TEMA"); }
    }

    @Test void borradoPendienteDelTemaNoDejaComentariosHuerfanos() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var conexion = jdbc.getDataSource().getConnection()) {
            conexion.setAutoCommit(false);
            try {
                try (var sentencia = conexion.prepareStatement("DELETE FROM TEMAS WHERE ID=?")) {
                    sentencia.setLong(1, tema);
                    sentencia.executeUpdate();
                }
                var publicacion = executor.submit(() -> mvc.perform(body(post("/api/temas/{id}/comentarios", tema), datos("Carrera")))
                        .andReturn().getResponse().getStatus());
                Integer antes = null;
                try { antes = publicacion.get(500, TimeUnit.MILLISECONDS); }
                catch (TimeoutException esperado) { }
                conexion.commit();
                int resultado = antes == null ? publicacion.get(10, TimeUnit.SECONDS) : antes;
                assertThat(resultado).isIn(404, 409);
                assertThat(cantidad()).isZero();
            } finally { conexion.rollback(); }
        } finally { executor.shutdownNow(); }
    }
}
