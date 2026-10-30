package cl.duoc.forocontenido.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "usuarios")
public record UsuariosProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout) {
    public UsuariosProperties {
        if (baseUrl == null || baseUrl.getHost() == null
                || !("http".equalsIgnoreCase(baseUrl.getScheme()) || "https".equalsIgnoreCase(baseUrl.getScheme()))
                || baseUrl.getRawUserInfo() != null || baseUrl.getRawQuery() != null || baseUrl.getRawFragment() != null) {
            throw new IllegalArgumentException("USUARIOS_BASE_URL debe ser HTTP o HTTPS sin credenciales, query ni fragmento");
        }
        if (connectTimeout == null || readTimeout == null
                || connectTimeout.toMillis() < 1 || readTimeout.toMillis() < 1
                || connectTimeout.toMillis() > Integer.MAX_VALUE || readTimeout.toMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Los timeouts de usuarios deben estar entre 1 y 2147483647 milisegundos");
        }
    }
}
