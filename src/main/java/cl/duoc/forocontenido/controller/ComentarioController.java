package cl.duoc.forocontenido.controller;

import cl.duoc.forocontenido.dto.*;
import cl.duoc.forocontenido.security.Identidad;
import cl.duoc.forocontenido.service.ComentarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
public class ComentarioController {
    private final ComentarioService comentarios;
    public ComentarioController(ComentarioService comentarios) { this.comentarios = comentarios; }

    @GetMapping("/api/temas/{temaId}/comentarios")
    public List<ComentarioResponse> listar(@PathVariable Long temaId) { return comentarios.listar(temaId); }

    @PostMapping("/api/temas/{temaId}/comentarios")
    public ResponseEntity<ComentarioResponse> crear(@PathVariable Long temaId, @Valid @RequestBody ComentarioRequest request,
                                                   @AuthenticationPrincipal Identidad identidad) {
        var comentario = comentarios.crear(temaId, request, identidad.id());
        return ResponseEntity.created(URI.create("/api/comentarios/" + comentario.id())).body(comentario);
    }

    @GetMapping("/api/comentarios/{id}")
    public ComentarioResponse obtener(@PathVariable Long id) { return comentarios.obtener(id); }

    @PutMapping("/api/comentarios/{id}")
    public ComentarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ComentarioRequest request,
                                         @AuthenticationPrincipal Identidad identidad) {
        return comentarios.actualizar(id, request, identidad.id());
    }

    @DeleteMapping("/api/comentarios/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal Identidad identidad) {
        comentarios.eliminar(id, identidad.id());
        return ResponseEntity.noContent().build();
    }
}
