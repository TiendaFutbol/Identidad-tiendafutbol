package cl.tienda.ms.identidad.dto;
import java.util.Map;
public record ErrorResponse(int status, String mensaje, Map<String, String> errores) {
    public ErrorResponse(int status, String mensaje) { this(status, mensaje, Map.of()); }
}
