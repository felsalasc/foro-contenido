package cl.duoc.forocontenido.security;

import cl.duoc.forocontenido.config.UsuariosProperties;
import cl.duoc.forocontenido.exception.IdentidadNoDisponibleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.HttpURLConnection;

@Component
public class UsuariosClient {
    private static final int MAX_IDENTITY_BYTES = 16_384;
    private final RestClient client;
    private final ObjectMapper mapper;
    private final Validator validator;

    public UsuariosClient(RestClient.Builder builder, ObjectMapper mapper, Validator validator, UsuariosProperties properties) {
        this.mapper = mapper;
        this.validator = validator;
        var factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        client = builder.baseUrl(properties.baseUrl().toString()).requestFactory(factory).build();
    }

    public Identidad obtener(String bearerToken) {
        try {
            return client.get().uri("/api/auth/me").accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + bearerToken)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 401) throw new BadCredentialsException("Token no autorizado");
                        if (status != 200) throw new IdentidadNoDisponibleException();
                        MediaType type = response.getHeaders().getContentType();
                        if (type == null || !(MediaType.APPLICATION_JSON.isCompatibleWith(type)
                                || ("application".equals(type.getType()) && type.getSubtype().endsWith("+json")))) {
                            throw new IdentidadNoDisponibleException();
                        }
                        byte[] body = response.getBody().readNBytes(MAX_IDENTITY_BYTES + 1);
                        if (body.length > MAX_IDENTITY_BYTES) throw new IdentidadNoDisponibleException();
                        Identidad identidad = mapper.readValue(body, Identidad.class);
                        if (identidad == null || !validator.validate(identidad).isEmpty()) {
                            throw new IdentidadNoDisponibleException();
                        }
                        return identidad;
                    });
        } catch (RestClientException | IllegalArgumentException ex) {
            throw new IdentidadNoDisponibleException();
        }
    }
}
