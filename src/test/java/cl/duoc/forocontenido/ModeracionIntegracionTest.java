package cl.duoc.forocontenido;

import cl.duoc.forocontenido.support.UsuariosStub;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:moderacion_contenido;MODE=Oracle;DB_CLOSE_DELAY=-1","moderacion.read-timeout=200ms"})
@AutoConfigureMockMvc
class ModeracionIntegracionTest {
    static final UsuariosStub usuarios;
    static { try { usuarios=new UsuariosStub(); } catch(Exception ex) { throw new ExceptionInInitializerError(ex); } }
    @DynamicPropertySource static void props(DynamicPropertyRegistry p) {
        p.add("usuarios.base-url",usuarios::baseUrl); p.add("moderacion.base-url",usuarios::baseUrl);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    long categoria,tema,comentario;
    @BeforeEach void setup() throws Exception {
        usuarios.reset();
        jdbc.update("DELETE FROM COMENTARIOS"); jdbc.update("DELETE FROM TEMAS"); jdbc.update("DELETE FROM CATEGORIAS");
        identidad("ADMINISTRADOR");
        categoria=id(mvc.perform(body(post("/api/categorias"),Map.of("nombre","Pruebas"))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        identidad("USUARIO");
        tema=id(mvc.perform(body(post("/api/temas"),datosTema())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        comentario=id(mvc.perform(body(post("/api/temas/{id}/comentarios",tema),Map.of("contenido","Respuesta"))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    @AfterAll static void close() { usuarios.close(); }
    void identidad(String rol) { usuarios.respuesta(200,"{\"id\":7,\"nombre\":\"Ana\",\"correo\":\"ana@example.test\",\"rol\":\""+rol+"\"}"); }
    long id(String body) throws Exception { return json.readTree(body).get("id").longValue(); }
    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b) { return b.header("Authorization","Bearer real-validado"); }
    MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder b,Object datos) throws Exception { return auth(b).contentType("application/json").content(json.writeValueAsString(datos)); }
    Map<String,Object> datosTema() { return Map.of("titulo","Java","contenido","Consulta","categoriaId",categoria); }
    @Test void temaBaneadoSeOcultaEnListadoBusquedaDetalleYComentarios() throws Exception {
        usuarios.temasBaneados=Set.of(tema);
        mvc.perform(auth(get("/api/temas"))).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(auth(get("/api/temas").param("texto","Java").param("categoriaId",""+categoria))).andExpect(content().json("[]"));
        mvc.perform(auth(get("/api/temas/{id}",tema))).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/temas/{id}/comentarios",tema))).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/comentarios/{id}",comentario))).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TEMAS",Long.class)).isEqualTo(1);
        assertThat(usuarios.moderacionAuthorization).isEqualTo("Bearer real-validado");
        assertThat(usuarios.moderacionCookie).isNull();
    }
    @Test void temaBaneadoRechazaTodasLasEscriturasSinBorrarDatos() throws Exception {
        usuarios.temasBaneados=Set.of(tema);
        mvc.perform(body(put("/api/temas/{id}",tema),datosTema())).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/temas/{id}",tema))).andExpect(status().isNotFound());
        mvc.perform(body(post("/api/temas/{id}/comentarios",tema),Map.of("contenido","Intento"))).andExpect(status().isNotFound());
        mvc.perform(body(put("/api/comentarios/{id}",comentario),Map.of("contenido","Intento"))).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/comentarios/{id}",comentario))).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT CONTENIDO FROM COMENTARIOS WHERE ID=?",String.class,comentario)).isEqualTo("Respuesta");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TEMAS",Long.class)).isEqualTo(1);
    }
    @Test void comentarioBaneadoSeOcultaYNoPermiteModificarlo() throws Exception {
        usuarios.comentariosBaneados=Set.of(comentario);
        mvc.perform(auth(get("/api/temas/{id}",tema))).andExpect(status().isOk());
        mvc.perform(auth(get("/api/temas/{id}/comentarios",tema))).andExpect(content().json("[]"));
        mvc.perform(auth(get("/api/comentarios/{id}",comentario))).andExpect(status().isNotFound());
        mvc.perform(body(put("/api/comentarios/{id}",comentario),Map.of("contenido","Intento"))).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/comentarios/{id}",comentario))).andExpect(status().isNotFound());
        usuarios.comentariosBaneados=Set.of();
        mvc.perform(auth(get("/api/comentarios/{id}",comentario))).andExpect(status().isOk()).andExpect(jsonPath("$.contenido").value("Respuesta"));
    }
    @Test void restaurarTemaNoRestauraComentarioBaneadoPorSeparado() throws Exception {
        usuarios.temasBaneados=Set.of(tema); usuarios.comentariosBaneados=Set.of(comentario);
        mvc.perform(auth(get("/api/comentarios/{id}",comentario))).andExpect(status().isNotFound());
        usuarios.temasBaneados=Set.of();
        mvc.perform(auth(get("/api/temas/{id}",tema))).andExpect(status().isOk());
        mvc.perform(auth(get("/api/temas/{id}/comentarios",tema))).andExpect(content().json("[]"));
        mvc.perform(auth(get("/api/comentarios/{id}",comentario))).andExpect(status().isNotFound());
    }
    @Test void referenciasExigenAdminYFuncionanSinConsultarModeracion() throws Exception {
        mvc.perform(auth(get("/api/interno/moderacion/temas/{id}",tema))).andExpect(status().isForbidden());
        identidad("ADMINISTRADOR"); usuarios.moderacionStatus=503; usuarios.moderacionPeticiones.set(0);
        mvc.perform(auth(get("/api/interno/moderacion/temas/{id}",tema))).andExpect(status().isOk())
                .andExpect(content().json("{\"id\":"+tema+",\"temaId\":null}"));
        mvc.perform(auth(get("/api/interno/moderacion/comentarios/{id}",comentario))).andExpect(status().isOk())
                .andExpect(content().json("{\"id\":"+comentario+",\"temaId\":"+tema+"}"));
        mvc.perform(auth(get("/api/interno/moderacion/temas/999999"))).andExpect(status().isNotFound());
        assertThat(usuarios.moderacionPeticiones.get()).isZero();
    }
    @Test void fallaCerradoCuandoModeracionNoRespondeConContratoValido() throws Exception {
        for(int code:List.of(500,401,403,302)) {
            usuarios.moderacionStatus=code;
            mvc.perform(auth(get("/api/temas/{id}",tema))).andExpect(status().isServiceUnavailable());
        }
        usuarios.moderacionStatus=200;
        for(String respuesta:List.of("{}","{\"temaIds\":null,\"comentarioIds\":[]}","{\"temaIds\":[999999],\"comentarioIds\":[]}",
                "{\"temaIds\":[\"1\"],\"comentarioIds\":[]}","{\"temaIds\":[],\"comentarioIds\":[],\"extra\":1}","[]")) {
            usuarios.moderacionBody=respuesta;
            mvc.perform(auth(get("/api/temas/{id}",tema))).andExpect(status().isServiceUnavailable());
        }
        usuarios.moderacionBody=null; usuarios.moderacionTipo="text/html";
        mvc.perform(auth(get("/api/temas/{id}",tema))).andExpect(status().isServiceUnavailable());
        usuarios.moderacionTipo="application/json"; usuarios.moderacionDemora=1000;
        mvc.perform(auth(get("/api/temas/{id}",tema))).andExpect(status().isServiceUnavailable());
    }
    @Test void caidaModeracionNoModificaDatosYNoAfectaCategorias() throws Exception {
        usuarios.moderacionStatus=503;
        mvc.perform(body(put("/api/comentarios/{id}",comentario),Map.of("contenido","Cambio"))).andExpect(status().isServiceUnavailable());
        mvc.perform(auth(delete("/api/temas/{id}",tema))).andExpect(status().isServiceUnavailable());
        mvc.perform(auth(get("/api/categorias"))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT CONTENIDO FROM COMENTARIOS WHERE ID=?",String.class,comentario)).isEqualTo("Respuesta");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TEMAS",Long.class)).isEqualTo(1);
    }
    @Test void consultasAgrupanIdsEnLotesAcotados() throws Exception {
        for(int i=0;i<204;i++) jdbc.update("INSERT INTO TEMAS(TITULO,CONTENIDO,CATEGORIA_ID,AUTOR_ID,FECHA_CREACION) SELECT TITULO,CONTENIDO,CATEGORIA_ID,AUTOR_ID,FECHA_CREACION FROM TEMAS WHERE ID=?",tema);
        usuarios.moderacionPeticiones.set(0);
        mvc.perform(auth(get("/api/temas"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(205));
        assertThat(usuarios.moderacionPeticiones.get()).isEqualTo(2);
        assertThat(usuarios.maxLote).isEqualTo(200);
    }
    @Test void borrarTemaVisibleNoEliminaComentarioBaneadoPorCascada() throws Exception {
        usuarios.comentariosBaneados=Set.of(comentario);
        mvc.perform(auth(delete("/api/temas/{id}",tema))).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TEMAS",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT CONTENIDO FROM COMENTARIOS WHERE ID=?",String.class,comentario)).isEqualTo("Respuesta");
        usuarios.comentariosBaneados=Set.of();
        mvc.perform(auth(delete("/api/temas/{id}",tema))).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM COMENTARIOS",Long.class)).isZero();
    }
    @Test void otroAutorObtiene404ParaTodasLasEscriturasSobreRecursosOcultos() throws Exception {
        usuarios.respuesta(200,"{\"id\":99,\"nombre\":\"Otro\",\"correo\":\"otro@example.test\",\"rol\":\"USUARIO\"}");
        usuarios.temasBaneados=Set.of(tema);
        mvc.perform(body(put("/api/temas/{id}",tema),datosTema())).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/temas/{id}",tema))).andExpect(status().isNotFound());
        mvc.perform(body(put("/api/comentarios/{id}",comentario),Map.of("contenido","Intento"))).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/comentarios/{id}",comentario))).andExpect(status().isNotFound());
        usuarios.temasBaneados=Set.of(); usuarios.comentariosBaneados=Set.of(comentario);
        mvc.perform(auth(delete("/api/comentarios/{id}",comentario))).andExpect(status().isNotFound());
        usuarios.comentariosBaneados=Set.of();
        mvc.perform(auth(delete("/api/comentarios/{id}",comentario))).andExpect(status().isForbidden());
    }
}
