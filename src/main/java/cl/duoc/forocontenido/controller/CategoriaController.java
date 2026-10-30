package cl.duoc.forocontenido.controller;

import cl.duoc.forocontenido.dto.*;
import cl.duoc.forocontenido.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
    private final CategoriaService categorias;
    public CategoriaController(CategoriaService categorias) { this.categorias = categorias; }

    @GetMapping
    public List<CategoriaResponse> listar() { return categorias.listar(); }

    @GetMapping("/{id}")
    public CategoriaResponse obtener(@PathVariable Long id) { return categorias.obtener(id); }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        var categoria = categorias.crear(request);
        return ResponseEntity.created(URI.create("/api/categorias/" + categoria.id())).body(categoria);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return categorias.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categorias.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
