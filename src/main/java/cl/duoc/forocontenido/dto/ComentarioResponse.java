package cl.duoc.forocontenido.dto;

import cl.duoc.forocontenido.model.Comentario;
import java.time.OffsetDateTime;

public record ComentarioResponse(Long id, String contenido, Long temaId, Long autorId, OffsetDateTime fechaCreacion) {
    public static ComentarioResponse de(Comentario comentario) {
        return new ComentarioResponse(comentario.getId(), comentario.getContenido(), comentario.getTema().getId(),
                comentario.getAutorId(), comentario.getFechaCreacion());
    }
}
