package cl.duoc.forocontenido.security;

import jakarta.validation.constraints.*;

public record Identidad(
        @NotNull @Positive Long id,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Pattern(regexp = "USUARIO|ADMINISTRADOR") String rol) {
}
