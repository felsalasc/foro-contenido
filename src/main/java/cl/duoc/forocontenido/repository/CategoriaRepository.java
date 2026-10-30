package cl.duoc.forocontenido.repository;

import cl.duoc.forocontenido.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Categoria c where c.id = :id")
    Optional<Categoria> buscarConBloqueo(@Param("id") Long id);

    List<Categoria> findAllByOrderByIdAsc();
    boolean existsByClaveNombreAndIdNot(String claveNombre, Long id);
    boolean existsByClaveNombre(String claveNombre);
}
