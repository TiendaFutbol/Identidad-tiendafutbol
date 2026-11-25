package cl.tienda.ms.identidad.dto;
import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotBlank @Email @Size(max = 150) String correo,
    @NotBlank @Size(max = 72) String clave
) {}
