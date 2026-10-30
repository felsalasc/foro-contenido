package cl.duoc.forocontenido.service;

import cl.duoc.forocontenido.dto.*;
import cl.duoc.forocontenido.exception.RecursoNoEncontradoException;
import cl.duoc.forocontenido.model.*;
import cl.duoc.forocontenido.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class TemaService {
    private final TemaRepository temas;
    private final CategoriaRepository categorias;

    public TemaService(TemaRepository temas, CategoriaRepository categorias) {
        this.temas = temas;
        this.categorias = categorias;
    }
    public List<TemaResponse> buscar(String texto, Long categoriaId) {
        if (categoriaId != null) validarId(categoriaId);
        String termino = texto == null ? "" : texto.strip();
        List<Tema> resultados;
        if (termino.isEmpty()) {
            resultados = categoriaId == null ? temas.findAllByOrderByFechaCreacionDescIdDesc()
                    : temas.findByCategoria_IdOrderByFechaCreacionDescIdDesc(categoriaId);
        } else {
            String patron = "%" + termino.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            resultados = categoriaId == null ? temas.buscarTexto(patron) : temas.buscarTextoYCategoria(patron, categoriaId);
        }
        return resultados.stream().map(TemaResponse::de).toList();
    }
    public TemaResponse obtener(Long id) { return TemaResponse.de(buscarTema(id)); }

    @Transactional
    public TemaResponse crear(TemaRequest request, Long autorId) {
        var categoria = buscarCategoria(request.categoriaId());
        return TemaResponse.de(temas.saveAndFlush(new Tema(request.titulo(), request.contenido(), categoria, autorId)));
    }

    @Transactional
    public TemaResponse actualizar(Long id, TemaRequest request, Long autorId) {
        var tema = buscarTema(id);
        exigirAutor(tema, autorId);
        var categoria = buscarCategoria(request.categoriaId());
        tema.actualizar(request.titulo(), request.contenido(), categoria);
        temas.flush();
        return TemaResponse.de(tema);
    }

    @Transactional
    public void eliminar(Long id, Long autorId) {
        validarId(id);
        var tema = temas.buscarConBloqueo(id).orElseThrow(() -> new RecursoNoEncontradoException("Tema no encontrado"));
        exigirAutor(tema, autorId);
        // La FK elimina los comentarios en la misma sentencia/transacción.
        temas.delete(tema);
        temas.flush();
    }
    private Tema buscarTema(Long id) {
        validarId(id);
        return temas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Tema no encontrado"));
    }
    private Categoria buscarCategoria(Long id) {
        // Mantener la categoría hasta confirmar la creación o edición del tema.
        return categorias.buscarConBloqueo(id).orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));
    }
    private void exigirAutor(Tema tema, Long autorId) {
        if (!tema.getAutorId().equals(autorId)) throw new AccessDeniedException("Solo el autor puede modificar el tema");
    }
    private void validarId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("El ID debe ser positivo");
    }
}
