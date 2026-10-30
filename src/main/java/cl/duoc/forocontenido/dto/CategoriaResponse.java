package cl.duoc.forocontenido.dto;

import cl.duoc.forocontenido.model.Categoria;

public record CategoriaResponse(Long id, String nombre, String descripcion) {
    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre(), categoria.getDescripcion());
    }
}
