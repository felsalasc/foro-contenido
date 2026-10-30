package cl.duoc.forocontenido;

import cl.duoc.forocontenido.support.UsuariosStub;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CategoriasApiTest {
    static final UsuariosStub usuarios = crearStub();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    static UsuariosStub crearStub() {
        try { return new UsuariosStub(); }
        catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("usuarios.base-url", usuarios::baseUrl);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:categorias_test;MODE=Oracle;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        usuarios.reset();
        admin();
        // Permite observar primero el 404 de una API todavía inexistente.
        if (jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='CATEGORIAS'", Integer.class) > 0)
            jdbc.update("DELETE FROM CATEGORIAS");
    }

    @AfterAll static void cerrar() { usuarios.close(); }
    void admin() { usuarios.respuesta(200, UsuariosStub.USUARIO.replace("USUARIO", "ADMINISTRADOR")); }
    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer categoria-prueba");
    }
    MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, String contenido) {
        return auth(request).contentType("application/json").content(contenido);
    }
    String datos(String nombre, String descripcion) throws Exception {
        return json.writeValueAsString(Map.of("nombre", nombre, "descripcion", descripcion));
    }
    long crear(String nombre) throws Exception {
        var resultado = mvc.perform(body(post("/api/categorias"), datos(nombre, "Descripción")))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }
    long cantidad() { return jdbc.queryForObject("SELECT COUNT(*) FROM CATEGORIAS", Long.class); }

    @Test void adminCreaCategoria201() throws Exception {
        mvc.perform(body(post("/api/categorias"), datos("  Programación  ", "Ayuda con Java")))
                .andExpect(status().isCreated()).andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/categorias/[0-9]+")))
                .andExpect(jsonPath("$.nombre").value("Programación"))
                .andExpect(jsonPath("$.descripcion").value("Ayuda con Java"))
                .andExpect(jsonPath("$.claveNombre").doesNotExist());
        assertThat(cantidad()).isEqualTo(1);
    }

    @Test void usuarioNoCreaCategoria403() throws Exception {
        usuarios.reset();
        mvc.perform(body(post("/api/categorias"), datos("Tecnología", ""))).andExpect(status().isForbidden());
        assertThat(cantidad()).isZero();
    }

    @Test void usuarioConsultaListadoYDetalle() throws Exception {
        long primero = crear("Tecnología");
        long segundo = crear("Programación");
        usuarios.reset();
        mvc.perform(auth(get("/api/categorias"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(primero))
                .andExpect(jsonPath("$[1].id").value(segundo));
        mvc.perform(auth(get("/api/categorias/{id}", primero))).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tecnología"));
    }

    @Test void listadoVacioEs200() throws Exception {
        mvc.perform(auth(get("/api/categorias"))).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void adminEditaYBorraCategoria() throws Exception {
        long id = crear("Original");
        mvc.perform(body(put("/api/categorias/{id}", id), datos("  Nueva  ", "Actualizada")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Nueva")).andExpect(jsonPath("$.descripcion").value("Actualizada"));
        mvc.perform(auth(get("/api/categorias/{id}", id))).andExpect(jsonPath("$.nombre").value("Nueva"));
        mvc.perform(auth(delete("/api/categorias/{id}", id))).andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(cantidad()).isZero();
        mvc.perform(auth(get("/api/categorias/{id}", id))).andExpect(status().isNotFound());
    }

    @Test void usuarioNoEditaNiBorra() throws Exception {
        long id = crear("Original");
        usuarios.reset();
        mvc.perform(body(put("/api/categorias/{id}", id), datos("Otra", ""))).andExpect(status().isForbidden());
        mvc.perform(auth(delete("/api/categorias/{id}", id))).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT NOMBRE FROM CATEGORIAS WHERE ID=?", String.class, id)).isEqualTo("Original");
    }

    @Test void duplicadoNormalizadoEs409() throws Exception {
        crear("Programación");
        mvc.perform(body(post("/api/categorias"), datos(" programación ", "Otra")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(cantidad()).isEqualTo(1);
        crear("Programacion"); // Los acentos forman parte del nombre.
        assertThat(cantidad()).isEqualTo(2);
    }

    @Test void editarNoDuplicaNiModificaAlFallar() throws Exception {
        long primero = crear("Bases de datos");
        long segundo = crear("Java");
        mvc.perform(body(put("/api/categorias/{id}", segundo), datos(" BASES DE DATOS ", "Otra"))).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT NOMBRE FROM CATEGORIAS WHERE ID=?", String.class, segundo)).isEqualTo("Java");
        mvc.perform(body(put("/api/categorias/{id}", primero), datos("bases de datos", ""))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nombre\":null}", "{\"nombre\":\"\"}", "{\"nombre\":\"   \"}",
            "{\"nombre\":123}", "{\"nombre\":true}", "{\"nombre\":\"Java\",\"descripcion\":123}",
            "{\"nombre\":\"Java\",\"id\":22}", "{\"nombre\":\"Java\",\"claveNombre\":\"OTRA\"}",
            "{\"nombre\":\"Java\"} {}", "{", "[]"})
    void entradaInvalidaEs400SinEscritura(String contenido) throws Exception {
        mvc.perform(body(post("/api/categorias"), contenido)).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        assertThat(cantidad()).isZero();
    }

    @Test void limitesDeLongitud() throws Exception {
        crear("Á".repeat(100));
        mvc.perform(body(post("/api/categorias"), datos("a".repeat(101), ""))).andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/categorias"), datos("Otra", "d".repeat(501)))).andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/categorias"), datos("Válida", "d".repeat(500)))).andExpect(status().isCreated());
        assertThat(cantidad()).isEqualTo(2);
    }

    @Test void descripcionOpcionalYNombreExpandido() throws Exception {
        mvc.perform(body(post("/api/categorias"), "{\"nombre\":\"" + "ß".repeat(100) + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.descripcion").isEmpty());
        mvc.perform(body(post("/api/categorias"), "{\"nombre\":\"Otra\",\"descripcion\":null}"))
                .andExpect(status().isCreated());
    }

    @Test void inexistenteEs404() throws Exception {
        mvc.perform(auth(get("/api/categorias/999999"))).andExpect(status().isNotFound());
        mvc.perform(body(put("/api/categorias/999999"), datos("Otra", ""))).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/categorias/999999"))).andExpect(status().isNotFound());
    }

    @ParameterizedTest @ValueSource(strings = {"abc", "0", "-1", "999999999999999999999999"})
    void idInvalidoEs400(String id) throws Exception {
        mvc.perform(auth(get("/api/categorias/" + id))).andExpect(status().isBadRequest());
    }

    @Test void sinIdentidadNoEscribe() throws Exception {
        mvc.perform(post("/api/categorias").contentType("application/json").content(datos("Java", ""))).andExpect(status().isUnauthorized());
        usuarios.respuesta(503, "privado");
        mvc.perform(body(post("/api/categorias"), datos("Java", ""))).andExpect(status().isServiceUnavailable());
        assertThat(cantidad()).isZero();
    }

    @Test void dosCreacionesConcurrentesSoloGuardanUna() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(1);
        try {
            Callable<Integer> tarea = () -> { inicio.await(); return mvc.perform(body(post("/api/categorias"), datos("Concurrente", "")))
                    .andReturn().getResponse().getStatus(); };
            var uno = executor.submit(tarea);
            var dos = executor.submit(tarea);
            inicio.countDown();
            assertThat(java.util.List.of(uno.get(15, TimeUnit.SECONDS), dos.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(cantidad()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }

    @Test void datosDemostracionContienenCincoCategorias() throws Exception {
        var carga = new ResourceDatabasePopulator(new FileSystemResource("database/02_datos_demostracion.sql"));
        carga.setSqlScriptEncoding("UTF-8");
        carga.execute(jdbc.getDataSource());
        assertThat(cantidad()).isEqualTo(5);
        mvc.perform(auth(get("/api/categorias"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        assertThat(jdbc.queryForList("SELECT NOMBRE FROM CATEGORIAS", String.class))
                .containsExactlyInAnyOrder("Programación", "Bases de datos", "Tecnología", "Videojuegos", "Consultas académicas");
    }
}
