package cl.tienda.ms.identidad.dto;
import jakarta.validation.constraints.*;

public record CambiarClaveRequest(
    @NotBlank @Size(max = 72) String claveActual,
    @NotBlank @Size(min = 8, max = 72) String claveNueva
) {}
