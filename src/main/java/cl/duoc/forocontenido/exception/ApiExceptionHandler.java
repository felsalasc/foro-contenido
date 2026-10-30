package cl.duoc.forocontenido.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> noEncontrado(RecursoNoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ProblemDetail> conflicto(ConflictoException ex) {
        return error(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> integridad() {
        return error(HttpStatus.CONFLICT, "Conflicto", "La operación entra en conflicto con los datos existentes");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException ex) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisar los campos enviados");
        problema.setTitle("Datos inválidos");
        var campos = new LinkedHashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(campo -> campos.putIfAbsent(campo.getField(), campo.getDefaultMessage()));
        problema.setProperty("errors", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> peticionInvalida() {
        return error(HttpStatus.BAD_REQUEST, "Petición inválida", "El JSON, sus campos o el identificador no son válidos");
    }

    private ResponseEntity<ProblemDetail> error(HttpStatus status, String titulo, String detalle) {
        var problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        return ResponseEntity.status(status).body(problema);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> permisos() {
        var error = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Permisos insuficientes");
        error.setTitle("Acceso denegado");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
