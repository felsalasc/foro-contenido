package cl.duoc.forocontenido.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix="moderacion")
public record ModeracionProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout) {
    public ModeracionProperties { new UsuariosProperties(baseUrl,connectTimeout,readTimeout); }
}
