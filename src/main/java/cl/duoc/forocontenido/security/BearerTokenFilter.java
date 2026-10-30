package cl.duoc.forocontenido.security;

import cl.duoc.forocontenido.exception.IdentidadNoDisponibleException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class BearerTokenFilter extends OncePerRequestFilter {
    private static final Pattern BEARER_VALUE = Pattern.compile("[A-Za-z0-9\\-._~+/]+=*");
    private final UsuariosClient usuarios;
    private final ApiSecurityErrorWriter errors;

    public BearerTokenFilter(UsuariosClient usuarios, ApiSecurityErrorWriter errors) {
        this.usuarios = usuarios;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        String header = headers.get(0);
        String token = header.length() > 7 ? header.substring(7).strip() : "";
        if (headers.size() != 1 || !header.regionMatches(true, 0, "Bearer ", 0, 7)
                || !BEARER_VALUE.matcher(token).matches()) {
            errors.escribir(response, HttpStatus.UNAUTHORIZED);
            return;
        }
        try {
            Identidad identidad = usuarios.obtener(token);
            var authentication = new UsernamePasswordAuthenticationToken(identidad, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + identidad.rol())));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            errors.escribir(response, HttpStatus.UNAUTHORIZED);
            return;
        } catch (IdentidadNoDisponibleException ex) {
            SecurityContextHolder.clearContext();
            errors.escribir(response, HttpStatus.SERVICE_UNAVAILABLE);
            return;
        }
        chain.doFilter(request, response);
    }
}
