package cl.tienda.ms.identidad.dto;
import cl.tienda.ms.identidad.model.Rol;
import cl.tienda.ms.identidad.model.Usuario;
import java.time.LocalDate;
public record UsuarioResponse(Long id, String run, String nombre, String apellido,
    String correo, LocalDate fechaNacimiento, Rol rol, Long comunaId, String direccion) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getRun(), u.getNombre(), u.getApellido(),
            u.getCorreo(), u.getFechaNacimiento(), u.getRol(), u.getComunaId(), u.getDireccion());
    }
}
