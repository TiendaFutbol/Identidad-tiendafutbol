package cl.tienda.ms.identidad.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PerfilRequest(
    @NotBlank @Size(max = 20) String run,
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 100) String apellido,
    @NotBlank @Email @Size(max = 150) String correo,
    @Past LocalDate fechaNacimiento,
    @Positive Long comunaId,
    @Size(max = 200) String direccion
) {}
