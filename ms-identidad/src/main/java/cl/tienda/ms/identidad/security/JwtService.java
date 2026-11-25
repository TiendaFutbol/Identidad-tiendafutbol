package cl.tienda.ms.identidad.security;
import cl.tienda.ms.identidad.model.Usuario;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import java.security.MessageDigest;

@Service
public class JwtService {
    private static final String ISSUER = "ms-identidad";
    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long expiration;
    private final long refreshExpiration;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.refresh-secret}") String refreshSecret,
                      @Value("${jwt.expiration}") long expiration,
                      @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        byte[] access = HexFormat.of().parseHex(secret);
        byte[] refresh = HexFormat.of().parseHex(refreshSecret);
        if (access.length != 32 || refresh.length != 32 || MessageDigest.isEqual(access, refresh)) {
            throw new IllegalArgumentException("Las claves JWT deben ser hexadecimales de 32 bytes y distintas.");
        }
        if (expiration < 1000 || refreshExpiration <= expiration) {
            throw new IllegalArgumentException("Duraciones JWT inválidas.");
        }
        this.accessKey = Keys.hmacShaKeyFor(access);
        this.refreshKey = Keys.hmacShaKeyFor(refresh);
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }
    public String generarToken(Usuario usuario) {
        return base(usuario, "access", expiration)
            .claim("correo", usuario.getCorreo())
            .claim("rol", usuario.getRol().name())
            .claim("usuarioId", usuario.getId())
            .signWith(accessKey, SignatureAlgorithm.HS256).compact();
    }
    public String generarRefreshToken(Usuario usuario) {
        return base(usuario, "refresh", refreshExpiration)
            .signWith(refreshKey, SignatureAlgorithm.HS256).compact();
    }
    private JwtBuilder base(Usuario usuario, String tipo, long duracion) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder().setIssuer(ISSUER).setSubject(usuario.getId().toString())
            .setId(UUID.randomUUID().toString()).claim("tipo", tipo)
            .setIssuedAt(new Date(ahora)).setExpiration(new Date(ahora + duracion));
    }
    private Claims parse(String token, SecretKey key, String tipo) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).requireIssuer(ISSUER)
            .require("tipo", tipo).build().parseClaimsJws(token).getBody();
        if (claims.getExpiration() == null || claims.getSubject() == null) {
            throw new MalformedJwtException("Faltan claims obligatorios");
        }
        return claims;
    }
    public Claims validarToken(String token) { return parse(token, accessKey, "access"); }
    public Long validarRefreshToken(String token) {
        return Long.valueOf(parse(token, refreshKey, "refresh").getSubject());
    }
    public long getExpiresIn() { return expiration / 1000; }
}
