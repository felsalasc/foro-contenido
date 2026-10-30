package cl.duoc.forocontenido.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiSecurityErrorWriter {
    private final ObjectMapper mapper;

    public ApiSecurityErrorWriter(ObjectMapper mapper) { this.mapper = mapper; }

    public void escribir(HttpServletResponse response, HttpStatus status) throws IOException {
        String detail = switch (status) {
            case UNAUTHORIZED -> "Autenticación requerida o token inválido";
            case FORBIDDEN -> "Permisos insuficientes";
            default -> "No se pudo verificar la identidad; intente nuevamente más tarde";
        };
        ProblemDetail error = ProblemDetail.forStatusAndDetail(status, detail);
        error.setTitle(status.getReasonPhrase());
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        if (status == HttpStatus.UNAUTHORIZED) response.setHeader("WWW-Authenticate", "Bearer");
        mapper.writeValue(response.getOutputStream(), error);
    }
}
