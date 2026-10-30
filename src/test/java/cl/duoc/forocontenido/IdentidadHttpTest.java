package cl.duoc.forocontenido;

import cl.duoc.forocontenido.support.UsuariosStub;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(IdentidadHttpTest.ProbeConfig.class)
class IdentidadHttpTest {
    static final UsuariosStub usuarios = crearStub();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private static UsuariosStub crearStub() {
        try { return new UsuariosStub(); }
        catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("usuarios.base-url", usuarios::baseUrl);
        registry.add("usuarios.connect-timeout", () -> "500ms");
        registry.add("usuarios.read-timeout", () -> "500ms");
    }

    @BeforeEach
    void reset() {
        usuarios.reset();
        jdbc.execute("CREATE TABLE IF NOT EXISTS PRUEBA_ESCRITURAS (ID BIGINT)");
        jdbc.update("DELETE FROM PRUEBA_ESCRITURAS");
    }

    @AfterAll
    static void cerrar() { usuarios.close(); }

    @Test
    void contratoMeValido() throws Exception {
        mvc.perform(get("/prueba/identidad").header("Authorization", "Bearer token-prueba").header("Cookie", "privada=ejemplo"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.correo").value("ana@example.test"))
                .andExpect(jsonPath("$.rol").value("USUARIO"))
                .andExpect(header().doesNotExist("Set-Cookie"));
        assertThat(usuarios.authorization).isEqualTo("Bearer token-prueba");
        assertThat(usuarios.cookie).isNull();
        assertThat(usuarios.peticiones.get()).isEqualTo(1);
    }

    @Test
    void tokenAusenteEs401SinConsultarUsuarios() throws Exception {
        mvc.perform(post("/prueba/escritura"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
        assertThat(usuarios.peticiones.get()).isZero();
        sinEscrituras();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Basic abc", "Bearer", "Bearer ", "Bearer token con espacios"})
    void cabeceraInvalidaEs401(String header) throws Exception {
        mvc.perform(post("/prueba/escritura").header("Authorization", header))
                .andExpect(status().isUnauthorized());
        assertThat(usuarios.peticiones.get()).isZero();
        sinEscrituras();
    }

    @Test
    void cabecerasDuplicadasEs401() throws Exception {
        mvc.perform(post("/prueba/escritura").header("Authorization", "Bearer a", "Bearer b"))
                .andExpect(status().isUnauthorized());
        assertThat(usuarios.peticiones.get()).isZero();
        sinEscrituras();
    }

    @Test
    void usuarioResponde401() throws Exception {
        usuarios.respuesta(401, "{\"detail\":\"informacion interna\"}");
        mvc.perform(post("/prueba/escritura").header("Authorization", "Bearer token-prueba"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("informacion interna"))));
        sinEscrituras();
    }

    @ParameterizedTest
    @ValueSource(ints = {204, 301, 302, 403, 404, 429, 500, 503})
    void respuestaInesperadaEs503SinReintentos(int status) throws Exception {
        usuarios.respuesta(status, "{}");
        esperar503();
        assertThat(usuarios.peticiones.get()).isEqualTo(1);
        assertThat(usuarios.redirecciones.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "{}",
            "{\"id\":7,\"nombre\":\"Ana\",\"correo\":\"ana@example.test\"}",
            "{\"id\":0,\"nombre\":\"Ana\",\"correo\":\"ana@example.test\",\"rol\":\"USUARIO\"}",
            "{\"id\":\"7\",\"nombre\":\"Ana\",\"correo\":\"ana@example.test\",\"rol\":\"USUARIO\"}",
            "{\"id\":7.5,\"nombre\":\"Ana\",\"correo\":\"ana@example.test\",\"rol\":\"USUARIO\"}",
            "{\"id\":7,\"nombre\":12,\"correo\":\"ana@example.test\",\"rol\":\"USUARIO\"}",
            "{\"id\":7,\"nombre\":\"\",\"correo\":\"ana@example.test\",\"rol\":\"USUARIO\"}",
            "{\"id\":7,\"nombre\":\"Ana\",\"correo\":\"sin-formato\",\"rol\":\"USUARIO\"}",
            "{\"id\":7,\"nombre\":\"Ana\",\"correo\":\"ana@example.test\",\"rol\":\"OTRO\"}",
            "{\"id\":7,\"nombre\":\"Ana\",\"correo\":\"ana@example.test\",\"rol\":null}"})
    void identidadMalformadaEs503(String body) throws Exception {
        usuarios.respuesta(200, body);
        esperar503();
    }

    @Test
    void contenidoNoJsonEs503() throws Exception {
        usuarios.tipo("text/plain");
        esperar503();
    }

    @Test
    void identidadCaidaEs503SinEscritura() throws Exception {
        usuarios.desconectar();
        esperar503();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void identidadTimeoutEs503(boolean demoraCuerpo) throws Exception {
        usuarios.demora(demoraCuerpo);
        esperar503();
    }

    @Test
    void respuestaDemasiadoGrandeEs503() throws Exception {
        usuarios.respuesta(200, " ".repeat(17000) + UsuariosStub.USUARIO);
        esperar503();
    }

    @Test
    void permisosYRolSeConsultanEnCadaPeticion() throws Exception {
        mvc.perform(post("/prueba/admin").header("Authorization", "Bearer token-prueba"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        sinEscrituras();
        usuarios.respuesta(200, UsuariosStub.USUARIO.replace("USUARIO", "ADMINISTRADOR"));
        mvc.perform(post("/prueba/admin").header("Authorization", "Bearer token-prueba")).andExpect(status().isOk());
        assertThat(cantidad()).isEqualTo(1);
        usuarios.respuesta(401, "{}");
        mvc.perform(post("/prueba/escritura").header("Authorization", "Bearer token-prueba"))
                .andExpect(status().isUnauthorized());
        assertThat(cantidad()).isEqualTo(1);
        assertThat(usuarios.peticiones.get()).isEqualTo(3);
    }

    private void esperar503() throws Exception {
        mvc.perform(post("/prueba/escritura").header("Authorization", "Bearer token-prueba"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        sinEscrituras();
    }

    private long cantidad() { return jdbc.queryForObject("SELECT COUNT(*) FROM PRUEBA_ESCRITURAS", Long.class); }
    private void sinEscrituras() { assertThat(cantidad()).isZero(); }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfig {
        @Bean ProbeController probeController(JdbcTemplate jdbc) { return new ProbeController(jdbc); }
    }

    @RestController
    @RequestMapping("/prueba")
    static class ProbeController {
        private final JdbcTemplate jdbc;
        ProbeController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

        @GetMapping("/identidad")
        Object identidad(@AuthenticationPrincipal Object identidad) { return identidad; }

        @PostMapping("/escritura")
        Map<String, Boolean> escritura() {
            jdbc.update("INSERT INTO PRUEBA_ESCRITURAS (ID) VALUES (1)");
            return Map.of("guardado", true);
        }

        @PostMapping("/admin")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        public Map<String, Boolean> admin() { return escritura(); }
    }
}
