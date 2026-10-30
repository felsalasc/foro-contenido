package cl.duoc.forocontenido.service;

import cl.duoc.forocontenido.dto.*;
import cl.duoc.forocontenido.exception.RecursoNoEncontradoException;
import cl.duoc.forocontenido.model.Comentario;
import cl.duoc.forocontenido.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ComentarioService {
    private final ComentarioRepository comentarios;
    private final TemaRepository temas;

    public ComentarioService(ComentarioRepository comentarios, TemaRepository temas) {
        this.comentarios = comentarios;
        this.temas = temas;
    }

    public List<ComentarioResponse> listar(Long temaId) {
        validarId(temaId);
        if (!temas.existsById(temaId)) throw new RecursoNoEncontradoException("Tema no encontrado");
        return comentarios.findByTema_IdOrderByFechaCreacionAscIdAsc(temaId).stream().map(ComentarioResponse::de).toList();
    }
    public ComentarioResponse obtener(Long id) { return ComentarioResponse.de(buscar(id)); }

    @Transactional
    public ComentarioResponse crear(Long temaId, ComentarioRequest request, Long autorId) {
        validarId(temaId);
        var tema = temas.buscarConBloqueo(temaId).orElseThrow(() -> new RecursoNoEncontradoException("Tema no encontrado"));
        return ComentarioResponse.de(comentarios.saveAndFlush(new Comentario(request.contenido(), tema, autorId)));
    }

    @Transactional
    public ComentarioResponse actualizar(Long id, ComentarioRequest request, Long autorId) {
        var comentario = buscarParaEscritura(id);
        exigirAutor(comentario, autorId);
        comentario.actualizar(request.contenido());
        comentarios.flush();
        return ComentarioResponse.de(comentario);
    }

    @Transactional
    public void eliminar(Long id, Long autorId) {
        var comentario = buscarParaEscritura(id);
        exigirAutor(comentario, autorId);
        comentarios.delete(comentario);
        comentarios.flush();
    }

    private Comentario buscar(Long id) {
        validarId(id);
        return comentarios.findById(id).orElseThrow(this::noEncontrado);
    }
    private Comentario buscarParaEscritura(Long id) {
        validarId(id);
        var temaId = comentarios.buscarTemaId(id).orElseThrow(this::noEncontrado);
        // Todas las escrituras de comentarios y el borrado del tema toman la
        // misma fila antes de cargar/modificar el comentario.
        temas.buscarConBloqueo(temaId).orElseThrow(this::noEncontrado);
        return buscar(id);
    }
    private RecursoNoEncontradoException noEncontrado() { return new RecursoNoEncontradoException("Comentario no encontrado"); }
    private void exigirAutor(Comentario comentario, Long autorId) {
        if (!comentario.getAutorId().equals(autorId)) throw new AccessDeniedException("Solo el autor puede modificar el comentario");
    }
    private void validarId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("El ID debe ser positivo");
    }
}
