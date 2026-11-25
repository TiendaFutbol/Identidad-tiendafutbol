package cl.tienda.ms.identidad.dto;
public record AuthResponse(String token, String refreshToken, String tokenType,
    long expiresIn, UsuarioResponse usuario) {}
