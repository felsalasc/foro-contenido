package cl.duoc.forocontenido;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datos_demo_test;MODE=Oracle;DB_CLOSE_DELAY=-1")
@ActiveProfiles("local")
class DatosDemostracionTest {
    @Autowired JdbcTemplate jdbc;

    @Test void cargaCincoCategoriasTresTemasYTresComentariosConAutoresParametrizados() throws Exception {
        var categorias = new ResourceDatabasePopulator(new FileSystemResource("database/02_datos_demostracion.sql"));
        categorias.setSqlScriptEncoding("UTF-8");
        categorias.execute(jdbc.getDataSource());
        // Identificadores de la contraparte simulada, deliberadamente distintos
        // de 1/2/3. La integración HTTP usa cuentas reales y sus IDs devueltos.
        String script = Files.readString(Path.of("database/02b_temas_comentarios_demostracion.sql"), StandardCharsets.UTF_8)
                .replaceAll("(?m)^(WHENEVER|ACCEPT)[^\\r\\n]*", "")
                .replace("&AUTOR_ADMIN", "41").replace("&AUTOR_USUARIO_1", "73").replace("&AUTOR_USUARIO_2", "109");
        var contenido = new ResourceDatabasePopulator(new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)));
        contenido.setSqlScriptEncoding("UTF-8");
        contenido.execute(jdbc.getDataSource());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CATEGORIAS", Long.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TEMAS", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM COMENTARIOS", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT DISTINCT AUTOR_ID FROM TEMAS", Long.class)).containsExactlyInAnyOrder(41L, 73L, 109L);
        assertThat(jdbc.queryForList("SELECT DISTINCT AUTOR_ID FROM COMENTARIOS", Long.class)).containsExactlyInAnyOrder(41L, 73L, 109L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM COMENTARIOS c JOIN TEMAS t ON t.ID=c.TEMA_ID", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT NOMBRE FROM CATEGORIAS", String.class)).contains("Programación", "Consultas académicas");
    }
}
