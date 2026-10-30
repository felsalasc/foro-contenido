package cl.duoc.forocontenido.dto;

import cl.duoc.forocontenido.model.Tema;
import java.time.OffsetDateTime;

public record TemaResponse(Long id, String titulo, String contenido, Long categoriaId,
                           Long autorId, OffsetDateTime fechaCreacion) {
    public static TemaResponse de(Tema tema) {
        return new TemaResponse(tema.getId(), tema.getTitulo(), tema.getContenido(), tema.getCategoria().getId(),
                tema.getAutorId(), tema.getFechaCreacion());
    }
}
