package cl.duoc.forocontenido.service;

import cl.duoc.forocontenido.dto.*;
import cl.duoc.forocontenido.exception.*;
import cl.duoc.forocontenido.model.Categoria;
import cl.duoc.forocontenido.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoriaService {
    private final CategoriaRepository categorias;
    public CategoriaService(CategoriaRepository categorias) { this.categorias = categorias; }

    public List<CategoriaResponse> listar() {
        return categorias.findAllByOrderByIdAsc().stream().map(CategoriaResponse::de).toList();
    }
    public CategoriaResponse obtener(Long id) { return CategoriaResponse.de(buscar(id)); }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        if (categorias.existsByClaveNombre(Categoria.clave(request.nombre()))) throw duplicado();
        return CategoriaResponse.de(categorias.saveAndFlush(new Categoria(request.nombre(), request.descripcion())));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        var categoria = buscar(id);
        if (categorias.existsByClaveNombreAndIdNot(Categoria.clave(request.nombre()), id)) throw duplicado();
        categoria.actualizar(request.nombre(), request.descripcion());
        categorias.flush();
        return CategoriaResponse.de(categoria);
    }

    @Transactional
    public void eliminar(Long id) {
        categorias.delete(buscar(id));
        categorias.flush();
    }
    private Categoria buscar(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("El ID debe ser positivo");
        return categorias.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));
    }
    private ConflictoException duplicado() { return new ConflictoException("Ya existe una categoría con ese nombre"); }
}
