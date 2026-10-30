package cl.duoc.forocontenido.dto;

import jakarta.validation.constraints.*;

public record TemaRequest(@NotBlank @Size(max = 200) String titulo,
                          @NotBlank @Size(max = 10000) String contenido,
                          @NotNull @Positive Long categoriaId) {
    public TemaRequest { titulo = titulo == null ? null : titulo.strip(); }
}
