package cl.duoc.forocontenido.repository;

import cl.duoc.forocontenido.model.Tema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface TemaRepository extends JpaRepository<Tema, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tema t where t.id = :id")
    Optional<Tema> buscarConBloqueo(@Param("id") Long id);

    List<Tema> findAllByOrderByFechaCreacionDescIdDesc();
    List<Tema> findByCategoria_IdOrderByFechaCreacionDescIdDesc(Long categoriaId);
    boolean existsByCategoria_Id(Long categoriaId);

    // SQL directo: LOWER/LIKE operan sobre el CLOB completo, sin truncarlo.
    @Query(value = """
            SELECT t.* FROM TEMAS t
            WHERE (LOWER(t.TITULO) LIKE :patron ESCAPE '!' OR LOWER(t.CONTENIDO) LIKE :patron ESCAPE '!')
            ORDER BY t.FECHA_CREACION DESC, t.ID DESC
            """, nativeQuery = true)
    List<Tema> buscarTexto(@Param("patron") String patron);

    @Query(value = """
            SELECT t.* FROM TEMAS t
            WHERE t.CATEGORIA_ID = :categoriaId
              AND (LOWER(t.TITULO) LIKE :patron ESCAPE '!' OR LOWER(t.CONTENIDO) LIKE :patron ESCAPE '!')
            ORDER BY t.FECHA_CREACION DESC, t.ID DESC
            """, nativeQuery = true)
    List<Tema> buscarTextoYCategoria(@Param("patron") String patron, @Param("categoriaId") Long categoriaId);
}
