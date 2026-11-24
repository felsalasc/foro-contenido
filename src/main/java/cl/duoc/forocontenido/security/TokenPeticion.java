package cl.duoc.forocontenido.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.stereotype.Component;

@Component
public class TokenPeticion {
    private static final String ATRIBUTO=TokenPeticion.class.getName();
    public static void guardar(HttpServletRequest request,String token) { request.setAttribute(ATRIBUTO,token); }
    public String obtener() {
        if(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                && attributes.getRequest().getAttribute(ATRIBUTO) instanceof String token) return token;
        throw new IllegalStateException("No hay identidad validada en esta petición");
    }
}
