package cl.duoc.forocontenido.controller;

import cl.duoc.forocontenido.repository.*;
import cl.duoc.forocontenido.exception.RecursoNoEncontradoException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interno/moderacion")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Transactional(readOnly=true)
public class ReferenciaModeracionController {
    private final TemaRepository temas;
    private final ComentarioRepository comentarios;
    public ReferenciaModeracionController(TemaRepository temas,ComentarioRepository comentarios) { this.temas=temas; this.comentarios=comentarios; }
    @GetMapping("/temas/{id}")
    public Referencia tema(@PathVariable Long id) {
        validar(id);
        var tema=temas.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Tema no encontrado"));
        return new Referencia(tema.getId(),null);
    }
    @GetMapping("/comentarios/{id}")
    public Referencia comentario(@PathVariable Long id) {
        validar(id);
        var comentario=comentarios.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Comentario no encontrado"));
        return new Referencia(comentario.getId(),comentario.getTema().getId());
    }
    private void validar(Long id) { if(id==null || id<=0) throw new IllegalArgumentException("ID inválido"); }
    public record Referencia(Long id,Long temaId) { }
}
