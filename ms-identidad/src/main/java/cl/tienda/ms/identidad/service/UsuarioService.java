package cl.tienda.ms.identidad.service;
import cl.tienda.ms.identidad.dto.*;
import cl.tienda.ms.identidad.model.*;
import cl.tienda.ms.identidad.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository; this.encoder = encoder;
    }
    public List<UsuarioResponse> listar() {
        return repository.findAll(Sort.by("id")).stream().map(UsuarioResponse::from).toList();
    }
    public UsuarioResponse obtener(Long id) { return UsuarioResponse.from(buscar(id)); }
    public Usuario buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
    @Transactional
    public UsuarioResponse registrar(RegistroRequest r) {
        return crear(r.run(), r.nombre(), r.apellido(), r.correo(), r.fechaNacimiento(), r.comunaId(), r.direccion(), r.clave(), Rol.cliente);
    }
    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest r) {
        return crear(r.run(), r.nombre(), r.apellido(), r.correo(), r.fechaNacimiento(), r.comunaId(), r.direccion(), r.clave(), r.rol());
    }
    private UsuarioResponse crear(String run, String nombre, String apellido, String correo, LocalDate nacimiento,
            Long comunaId, String direccion, String clave, Rol rol) {
        String email = normalizarCorreo(correo);
        if (repository.existsByCorreoIgnoreCase(email)) throw correoDuplicado();
        Usuario u = new Usuario();
        aplicarPerfil(u, run, nombre, apellido, email, nacimiento, comunaId, direccion);
        u.setRol(rol);
        u.setClave(codificarClave(clave));
        return UsuarioResponse.from(repository.saveAndFlush(u));
    }
    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest r, Long administradorId) {
        Usuario u = buscar(id);
        // Impide que un administrador se quite a sí mismo los permisos accidentalmente.
        if (id.equals(administradorId) && r.rol() != Rol.admin) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes cambiar tu propio rol de administrador");
        }
        actualizarPerfil(u, r.run(), r.nombre(), r.apellido(), r.correo(), r.fechaNacimiento(), r.comunaId(), r.direccion());
        u.setRol(r.rol());
        if (r.clave() != null) u.setClave(codificarClave(r.clave()));
        return UsuarioResponse.from(repository.saveAndFlush(u));
    }
    @Transactional
    public UsuarioResponse actualizarMiPerfil(Long id, PerfilRequest r) {
        Usuario u = buscar(id);
        actualizarPerfil(u, r.run(), r.nombre(), r.apellido(), r.correo(), r.fechaNacimiento(), r.comunaId(), r.direccion());
        return UsuarioResponse.from(repository.saveAndFlush(u));
    }
    @Transactional
    public void cambiarClave(Long id, CambiarClaveRequest r) {
        Usuario u = buscar(id);
        if (!claveValida(r.claveActual()) || !encoder.matches(r.claveActual(), u.getClave())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave actual no es correcta");
        }
        u.setClave(codificarClave(r.claveNueva()));
        repository.saveAndFlush(u);
    }
    @Transactional
    public void eliminar(Long id, Long administradorId) {
        if (id.equals(administradorId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta de administrador");
        repository.delete(buscar(id));
        repository.flush();
    }
    private void actualizarPerfil(Usuario u, String run, String nombre, String apellido, String correo,
            LocalDate nacimiento, Long comunaId, String direccion) {
        String email = normalizarCorreo(correo);
        if (repository.existsByCorreoIgnoreCaseAndIdNot(email, u.getId())) throw correoDuplicado();
        aplicarPerfil(u, run, nombre, apellido, email, nacimiento, comunaId, direccion);
    }
    private void aplicarPerfil(Usuario u, String run, String nombre, String apellido, String correo,
            LocalDate nacimiento, Long comunaId, String direccion) {
        u.setRun(run.trim()); u.setNombre(nombre.trim()); u.setApellido(apellido.trim()); u.setCorreo(correo);
        u.setFechaNacimiento(nacimiento); u.setComunaId(comunaId); u.setDireccion(direccion == null ? null : direccion.trim());
    }
    public static String normalizarCorreo(String correo) { return correo.trim().toLowerCase(Locale.ROOT); }
    public static boolean claveValida(String clave) {
        return clave != null && !clave.isBlank() && clave.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
    public String codificarClave(String clave) {
        if (!claveValida(clave) || clave.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave debe tener al menos 8 caracteres y como máximo 72 bytes UTF-8");
        }
        return encoder.encode(clave);
    }
    private ResponseStatusException correoDuplicado() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
    }
}
