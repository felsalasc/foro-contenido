package cl.duoc.forocontenido;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class AplicacionTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void arrancaConConexionLocalPropia() {
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
    }
}
