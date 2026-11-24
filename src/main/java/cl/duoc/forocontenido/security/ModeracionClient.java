package cl.duoc.forocontenido.security;

import cl.duoc.forocontenido.config.ModeracionProperties;
import cl.duoc.forocontenido.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.util.*;

@Component
@EnableConfigurationProperties(ModeracionProperties.class)
public class ModeracionClient {
    private static final int LOTE=200;
    private final RestClient client;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final TokenPeticion token;
    public ModeracionClient(RestClient.Builder builder,ModeracionProperties properties,ObjectMapper mapper,Validator validator,TokenPeticion token) {
        this.mapper=mapper; this.validator=validator; this.token=token;
        var factory=new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection,String method) throws IOException {
                super.prepareConnection(connection,method); connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(properties.connectTimeout()); factory.setReadTimeout(properties.readTimeout());
        client=builder.baseUrl(properties.baseUrl().toString()).requestFactory(factory).build();
    }
    public Bloqueados bloqueados(List<Long> temaIds,List<Long> comentarioIds) {
        var temas=temaIds.stream().distinct().toList();
        var comentarios=comentarioIds.stream().distinct().toList();
        var baneadosTemas=new HashSet<Long>(); var baneadosComentarios=new HashSet<Long>();
        for(int offset=0;offset<Math.max(temas.size(),comentarios.size());offset+=LOTE) {
            var temasLote=temas.subList(Math.min(offset,temas.size()),Math.min(offset+LOTE,temas.size()));
            var comentariosLote=comentarios.subList(Math.min(offset,comentarios.size()),Math.min(offset+LOTE,comentarios.size()));
            var resultado=consultar(temasLote,comentariosLote);
            baneadosTemas.addAll(resultado.temaIds()); baneadosComentarios.addAll(resultado.comentarioIds());
        }
        return new Bloqueados(Set.copyOf(baneadosTemas),Set.copyOf(baneadosComentarios));
    }
    public void exigirVisible(Long temaId,Long comentarioId) {
        var resultado=bloqueados(List.of(temaId),comentarioId==null?List.of():List.of(comentarioId));
        if(resultado.temaIds().contains(temaId) || (comentarioId!=null && resultado.comentarioIds().contains(comentarioId)))
            throw new RecursoNoEncontradoException("Recurso no encontrado");
    }
    private Respuesta consultar(List<Long> temas,List<Long> comentarios) {
        try {
            return client.post().uri("/api/moderacion/visibilidad").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .header("Authorization","Bearer "+token.obtener()).body(Map.of("temaIds",temas,"comentarioIds",comentarios))
                    .exchange((req,res)->{
                        MediaType media=res.getHeaders().getContentType();
                        if(res.getStatusCode().value()!=200 || media==null || !MediaType.APPLICATION_JSON.isCompatibleWith(media))
                            throw new ModeracionNoDisponibleException();
                        byte[] bytes=res.getBody().readNBytes(65537);
                        if(bytes.length>65536) throw new ModeracionNoDisponibleException();
                        Respuesta respuesta=mapper.readValue(bytes,Respuesta.class);
                        if(respuesta==null || !validator.validate(respuesta).isEmpty()
                                || !temas.containsAll(respuesta.temaIds()) || !comentarios.containsAll(respuesta.comentarioIds()))
                            throw new ModeracionNoDisponibleException();
                        return respuesta;
                    });
        } catch(RestClientException | IllegalArgumentException ex) { throw new ModeracionNoDisponibleException(); }
    }
    public record Respuesta(@NotNull @Size(max=200) List<@NotNull @Positive Long> temaIds,
                            @NotNull @Size(max=200) List<@NotNull @Positive Long> comentarioIds) { }
    public record Bloqueados(Set<Long> temaIds,Set<Long> comentarioIds) { }
}
