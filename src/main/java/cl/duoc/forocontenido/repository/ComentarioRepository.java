package cl.duoc.forocontenido.repository;

import cl.duoc.forocontenido.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByTema_IdOrderByFechaCreacionAscIdAsc(Long temaId);

    // Proyección: evita cargar una entidad que pudiera quedar obsoleta mientras
    // se espera el bloqueo del tema en una operación de escritura.
    @Query("select c.tema.id from Comentario c where c.id = :id")
    Optional<Long> buscarTemaId(@Param("id") Long id);
}
