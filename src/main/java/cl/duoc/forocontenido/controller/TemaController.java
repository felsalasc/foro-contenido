package cl.duoc.forocontenido.controller;

import cl.duoc.forocontenido.dto.*;
import cl.duoc.forocontenido.security.Identidad;
import cl.duoc.forocontenido.service.TemaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/temas")
public class TemaController {
    private final TemaService temas;
    public TemaController(TemaService temas) { this.temas = temas; }

    @GetMapping
    public List<TemaResponse> buscar(@RequestParam(required = false) String texto,
                                     @RequestParam(required = false) Long categoriaId) {
        return temas.buscar(texto, categoriaId);
    }
    @GetMapping("/{id}")
    public TemaResponse obtener(@PathVariable Long id) { return temas.obtener(id); }

    @PostMapping
    public ResponseEntity<TemaResponse> crear(@Valid @RequestBody TemaRequest request,
                                             @AuthenticationPrincipal Identidad identidad) {
        var tema = temas.crear(request, identidad.id());
        return ResponseEntity.created(URI.create("/api/temas/" + tema.id())).body(tema);
    }
    @PutMapping("/{id}")
    public TemaResponse actualizar(@PathVariable Long id, @Valid @RequestBody TemaRequest request,
                                   @AuthenticationPrincipal Identidad identidad) {
        return temas.actualizar(id, request, identidad.id());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal Identidad identidad) {
        temas.eliminar(id, identidad.id());
        return ResponseEntity.noContent().build();
    }
}
