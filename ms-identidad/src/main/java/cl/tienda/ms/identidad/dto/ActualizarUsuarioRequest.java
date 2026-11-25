package cl.tienda.ms.identidad.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import cl.tienda.ms.identidad.model.Rol;

public record ActualizarUsuarioRequest(
    @NotBlank @Size(max = 20) String run,
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 100) String apellido,
    @NotBlank @Email @Size(max = 150) String correo,
    @Past LocalDate fechaNacimiento,
    @Positive Long comunaId,
    @Size(max = 200) String direccion,
    @Size(min = 8, max = 72) String clave,
    @NotNull Rol rol
) {}
