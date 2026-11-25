package cl.tienda.ms.identidad.security;
import cl.tienda.ms.identidad.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.Locale;

// Se instancia únicamente en SecurityConfig para evitar doble registro como filtro Servlet.
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UsuarioRepository repository;
    public JwtAuthenticationFilter(JwtService jwt, UsuarioRepository repository) {
        this.jwt = jwt; this.repository = repository;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equals(request.getMethod()) ||
            ("POST".equals(request.getMethod()) && List.of("/api/auth/login", "/api/auth/registro", "/api/auth/refresh").contains(path)) ||
            path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/") || path.equals("/v3/api-docs") || path.startsWith("/v3/api-docs/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ") &&
                SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwt.validarToken(authorization.substring(7));
                Long id = Long.valueOf(claims.getSubject());
                repository.findById(id).filter(u -> u.getCorreo().equals(claims.get("correo", String.class)) &&
                    u.getRol().name().equals(claims.get("rol", String.class))).ifPresent(u -> {
                        var auth = new UsernamePasswordAuthenticationToken(u.getId(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name().toUpperCase(Locale.ROOT))));
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
