package cl.tienda.ms.identidad.controller;
import cl.tienda.ms.identidad.dto.*;
import cl.tienda.ms.identidad.service.AuthService;
import cl.tienda.ms.identidad.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y renovación JWT")
public class AuthController {
    private final AuthService auth;
    private final UsuarioService usuarios;
    public AuthController(AuthService auth, UsuarioService usuarios) { this.auth = auth; this.usuarios = usuarios; }

    @PostMapping("/registro")
    @Operation(summary = "Registrar cliente", description = "El registro público siempre asigna el rol cliente.")
    @ApiResponse(responseCode = "201", description = "Cliente registrado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "Correo duplicado")
    public ResponseEntity<UsuarioResponse> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarios.registrar(request));
    }
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida la clave con BCrypt y emite un access token y un refresh token.")
    @ApiResponse(responseCode = "200", description = "Autenticación correcta")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token", description = "Valida el refresh token y consulta el rol vigente en Oracle.")
    @ApiResponse(responseCode = "200", description = "Token renovado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido o vencido")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) { return auth.refresh(request); }
}
