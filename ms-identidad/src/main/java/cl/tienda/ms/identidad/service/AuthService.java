package cl.tienda.ms.identidad.service;
import cl.tienda.ms.identidad.dto.*;
import cl.tienda.ms.identidad.model.Usuario;
import cl.tienda.ms.identidad.repository.UsuarioRepository;
import cl.tienda.ms.identidad.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String dummyHash;
    public AuthService(UsuarioRepository repository, PasswordEncoder encoder, JwtService jwt) {
        this.repository = repository; this.encoder = encoder; this.jwt = jwt;
        this.dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }
    public AuthResponse login(LoginRequest r) {
        if (!UsuarioService.claveValida(r.clave())) throw credencialesInvalidas();
        Usuario u = repository.findByCorreoIgnoreCase(UsuarioService.normalizarCorreo(r.correo())).orElse(null);
        String hash = u == null || u.getClave().startsWith("REEMPLAZAR_") ? dummyHash : u.getClave();
        boolean coincide = encoder.matches(r.clave(), hash);
        if (u == null || !coincide || u.getClave().startsWith("REEMPLAZAR_")) throw credencialesInvalidas();
        return respuesta(u, jwt.generarRefreshToken(u));
    }
    public AuthResponse refresh(RefreshRequest r) {
        Long id;
        try { id = jwt.validarRefreshToken(r.refreshToken()); }
        catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido o vencido");
        }
        Usuario u = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
        // No se extiende la vida del refresh token en cada renovación.
        return respuesta(u, r.refreshToken());
    }
    private AuthResponse respuesta(Usuario u, String refresh) {
        return new AuthResponse(jwt.generarToken(u), refresh, "Bearer", jwt.getExpiresIn(), UsuarioResponse.from(u));
    }
    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o clave incorrectos");
    }
}
