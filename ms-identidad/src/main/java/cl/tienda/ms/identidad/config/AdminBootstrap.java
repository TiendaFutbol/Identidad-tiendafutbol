package cl.tienda.ms.identidad.config;
import cl.tienda.ms.identidad.model.Rol;
import cl.tienda.ms.identidad.repository.UsuarioRepository;
import cl.tienda.ms.identidad.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UsuarioRepository repository;
    private final UsuarioService usuarios;
    private final String correo;
    private final String clave;
    public AdminBootstrap(UsuarioRepository repository, UsuarioService usuarios,
            @Value("${app.bootstrap.admin-email}") String correo,
            @Value("${app.bootstrap.admin-password}") String clave) {
        this.repository = repository; this.usuarios = usuarios; this.correo = correo; this.clave = clave;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (correo.isBlank() && clave.isBlank()) return;
        if (correo.isBlank() || clave.isBlank()) throw new IllegalStateException("Configura ambas variables BOOTSTRAP_ADMIN_*.");
        var admin = repository.findByCorreoIgnoreCase(correo)
            .orElseThrow(() -> new IllegalStateException("Ejecuta primero el INSERT de administrador del informe."));
        if (admin.getRol() != Rol.admin) throw new IllegalStateException("La cuenta inicial no tiene rol admin.");
        // No se modifica una cuenta que ya tiene una clave real.
        if (admin.getClave().startsWith("REEMPLAZAR_POR_HASH_BCRYPT_ADMIN")) {
            admin.setClave(usuarios.codificarClave(clave));
            repository.saveAndFlush(admin);
        }
    }
}
