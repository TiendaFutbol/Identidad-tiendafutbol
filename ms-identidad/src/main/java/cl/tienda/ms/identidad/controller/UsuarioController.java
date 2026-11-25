package cl.tienda.ms.identidad.controller;
import cl.tienda.ms.identidad.dto.*;
import cl.tienda.ms.identidad.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "CRUD administrativo y perfil personal")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "No autenticado")
@ApiResponse(responseCode = "403", description = "Permisos insuficientes")
public class UsuarioController {
    private final UsuarioService usuarios;
    public UsuarioController(UsuarioService usuarios) { this.usuarios = usuarios; }
    @GetMapping
    @Operation(summary = "Listar usuarios (admin)")
    @ApiResponse(responseCode = "200", description = "Listado de usuarios")
    public List<UsuarioResponse> listar() { return usuarios.listar(); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar usuario por ID (admin)")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    public UsuarioResponse obtener(@PathVariable Long id) { return usuarios.obtener(id); }

    @PostMapping
    @Operation(summary = "Crear usuario (admin)")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "Correo duplicado")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioResponse response = usuarios.crear(request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + response.id())).body(response);
    }
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario (admin)", description = "Actualización completa; la clave es opcional. Permite gestionar el rol.")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @ApiResponse(responseCode = "409", description = "Correo duplicado")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest request,
                                      @AuthenticationPrincipal Long administradorId) {
        return usuarios.actualizar(id, request, administradorId);
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario (admin)")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado")
    @ApiResponse(responseCode = "400", description = "No se permite eliminar la cuenta propia")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal Long administradorId) {
        usuarios.eliminar(id, administradorId); return ResponseEntity.noContent().build();
    }
    @GetMapping("/me")
    @Operation(summary = "Consultar mi perfil", description = "Obtiene el ID a partir del JWT, no de datos enviados por el cliente.")
    @ApiResponse(responseCode = "200", description = "Perfil encontrado")
    public UsuarioResponse miPerfil(@AuthenticationPrincipal Long id) { return usuarios.obtener(id); }

    @PutMapping("/me")
    @Operation(summary = "Actualizar mi perfil", description = "No permite cambiar el rol ni la contraseña. Si cambia el correo, renueva el access token.")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "Correo duplicado")
    public UsuarioResponse actualizarMiPerfil(@AuthenticationPrincipal Long id, @Valid @RequestBody PerfilRequest request) {
        return usuarios.actualizarMiPerfil(id, request);
    }
    @PatchMapping("/me/clave")
    @Operation(summary = "Cambiar mi clave", description = "Requiere comprobar la contraseña actual.")
    @ApiResponse(responseCode = "204", description = "Clave modificada")
    @ApiResponse(responseCode = "400", description = "Clave actual incorrecta o nueva clave inválida")
    public ResponseEntity<Void> cambiarClave(@AuthenticationPrincipal Long id, @Valid @RequestBody CambiarClaveRequest request) {
        usuarios.cambiarClave(id, request); return ResponseEntity.noContent().build();
    }
}
