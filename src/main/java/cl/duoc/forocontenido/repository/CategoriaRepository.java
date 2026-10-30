package cl.duoc.forocontenido.repository;

import cl.duoc.forocontenido.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findAllByOrderByIdAsc();
    boolean existsByClaveNombreAndIdNot(String claveNombre, Long id);
    boolean existsByClaveNombre(String claveNombre);
}
