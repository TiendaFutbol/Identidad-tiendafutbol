# ms-identidad

Microservicio de identidad para la tienda online de la Evaluación 3 (DSY1104).
Java 17, Spring Boot 3.5.8, Maven, Oracle, JPA, Security, JJWT 0.11.5, Swagger.
Paquete propuesto: `cl.tienda.ms.identidad` (no se recibió un paquete existente).

## Ejecutar localmente

1. Tener JDK 17 o 21, Maven y Oracle en ejecución. Ejecutar el script de identidad del informe (usuario C##ms_identidad, tabla usuarios y datos iniciales).
2. Abrir `pom.xml` como proyecto Maven en el IDE. La configuración local usa los datos entregados: SID xe en localhost:1521, usuario C##ms_identidad, clave local 123, puerto HTTP 8081 y JWT de 24 horas.
3. `src/main/resources/application-local.yml` viene incluido para desarrollo local, pero está ignorado por Git. No subirlo al repositorio. No usar sus claves en producción. Si ya fue agregado a Git, quitarlo del índice y rotar las claves.
4. Las filas del SQL original contienen valores `REEMPLAZAR_POR_HASH_BCRYPT_ADMIN` y `REEMPLAZAR_POR_HASH_BCRYPT_CLIENTE`; no son hashes válidos. Para inicializar el administrador existente, ejecutar una vez en PowerShell:

```powershell
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "ElegirUnaClaveSegura123!"
mvn spring-boot:run
```

El bootstrap solo sustituye el marcador del administrador indicado, no crea usuarios ni sobrescribe hashes existentes. Después de detener el servidor, eliminar las variables `Remove-Item Env:BOOTSTRAP_ADMIN_EMAIL, Env:BOOTSTRAP_ADMIN_PASSWORD`. Los clientes de ejemplo con marcador deben recibir hashes reales por un administrador (PUT con clave) o registrar un nuevo cliente con otro correo. La clave de ejemplo anterior es solo demostrativa; elegir otra.

5. Abrir http://localhost:8081/swagger-ui.html. También está disponible http://localhost:8081/v3/api-docs.
6. Ejecutar `mvn clean package` para empaquetar. Las pruebas de integración HTTP ahora se ejecutan con Vitest (ver abajo).

Si la conexión SID `xe` no funciona, comprobar el listener y la versión de Oracle; no se cambió la URL solicitada. Si el usuario/schema fue creado en otra PDB, configurar `DB_URL` con su service name correcto.

## Endpoints

| Método | Ruta | Acceso | Respuesta esperada |
|---|---|---|---|
| POST | /api/auth/registro | Público | 201 |
| POST | /api/auth/login | Público | 200 |
| POST | /api/auth/refresh | Refresh token válido | 200 |
| GET | /api/usuarios | admin | 200 |
| GET | /api/usuarios/{id} | admin | 200 |
| POST | /api/usuarios | admin | 201 |
| PUT | /api/usuarios/{id} | admin | 200 |
| DELETE | /api/usuarios/{id} | admin | 204 |
| GET | /api/usuarios/me | admin/cliente | 200 |
| PUT | /api/usuarios/me | admin/cliente | 200 |
| PATCH | /api/usuarios/me/clave | admin/cliente | 204 |

### Registrar cliente

```json
{
  "run": "12345678-9",
  "nombre": "Ana",
  "apellido": "Pérez",
  "correo": "ana@example.com",
  "fechaNacimiento": "2000-03-12",
  "comunaId": 1,
  "direccion": "Calle de ejemplo 100",
  "clave": "UnaClaveSegura123!"
}
```

Registro público: el servidor asigna `cliente`, no acepta escalamiento de rol. El CRUD administrativo usa los mismos datos y el campo `rol` (`admin` o `cliente`). En PUT administrativo la clave puede omitirse para conservar la existente. En PUT de perfil se envían todos los campos de perfil, sin clave ni rol. Si cambia correo, renovar access token para continuar. El cambio de clave usa `claveActual` y `claveNueva`.

### Login

```json
{"correo":"ana@example.com","clave":"UnaClaveSegura123!"}
```

Devuelve `token`, `refreshToken`, `tokenType` (`Bearer`), `expiresIn` (segundos) y `usuario` (sin clave). En Swagger usar Authorize e introducir el valor de `token` sin prefijo. En Axios enviar `Authorization: Bearer <token>`.

### Renovación

POST `/api/auth/refresh` con `{"refreshToken":"..."}`. Devuelve la misma forma que login. El refresh token expira a los 7 días y no se extiende en cada renovación. Se firma con otra clave, por lo que no sirve como access token. No usar un access token como refresh token. La respuesta contiene el rol vigente.

## Contrato JWT para los otros microservicios

- Algoritmo HS256, clave `jwt.secret` de 64 caracteres hexadecimales, decodificada con `HexFormat.of().parseHex(secret)` (NO Base64 ni bytes UTF-8 del texto).
- Usar la misma clave de acceso en los otros microservicios, nunca compartir `jwt.refresh-secret`.
- `iss`: `ms-identidad`; `sub`: ID de usuario como texto; `usuarioId`: ID numérico; `correo`: correo; `rol`: `admin` o `cliente`; `tipo`: `access`; `iat`, `exp` y `jti`.
- Verificar firma, issuer, tipo y expiración antes de confiar en claims. Convertir el rol a `ROLE_ADMIN` o `ROLE_CLIENTE` para Spring Security. Rechazar tokens refresh. No recibir el rol desde el frontend como autoridad.
- ms-identidad consulta el usuario en su propia base en cada petición autenticada para comprobar que todavía exista y que el rol y correo coincidan. Los demás servicios que validen solo la firma no sabrán de cambios posteriores hasta expirar el token; definir una estrategia de revocación si el proyecto lo requiere.
- Para ms-ventas: un admin puede consultar usuarios por ID. Para un cliente autenticado que realiza una compra, ms-ventas puede reenviar su access token a `/api/usuarios/me` para consultar al comprador, sin dar acceso al listado de usuarios. No se agregó OpenFeign a identidad; corresponde a ventas.

## Seguridad y alcance

- No se agregaron tablas ni FK externas; `comunaId` es un ID lógico conforme al informe. No se verifica su existencia remotamente en esta versión.
- Contraseñas BCrypt, DTOs sin exposición de hash, validaciones, CORS configurable, control de roles en backend y errores 400/401/403/404/409.
- La renovación es stateless para respetar la tabla existente. No implementa revocación individual, rotación de refresh tokens ni cierre de sesión global: un refresh token robado puede usarse hasta expirar. Cambiar la contraseña no revoca tokens anteriores. Para un entorno real, agregar almacenamiento de sesiones/refresh tokens revocables, rotación, expiraciones de acceso más cortas, límites de intentos de login y HTTPS. El cierre de sesión del frontend debe borrar sus credenciales; la persistencia segura de sesión se implementará en el frontend, preferiblemente con cookies HttpOnly y protección CSRF si se adopta ese mecanismo. No guardar refresh tokens en localStorage en producción.
- La eliminación física respeta la rúbrica CRUD; ventas puede conservar IDs históricos. Definir una política de baja si se requiere conservar perfiles asociados a órdenes.
- En un entorno no local, establecer `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_REFRESH_SECRET`, `CORS_ALLOWED_ORIGINS`, y usar `DDL_AUTO=validate`. Generar ambas claves de forma independiente con un generador seguro. No poner secretos en Git. El perfil local usa ddl-auto update tal como se pidió.

## Evidencias recomendadas

Capturar Swagger con los endpoints y Authorize, registro y login correcto, clave incorrecta (401), cliente intentando listar usuarios (403), CRUD de administrador, renovación JWT y tabla Oracle con hash BCrypt. Guardar también la salida de `npm test` y las pruebas reales de integración frontend/backend. Los tests incluidos no fueron ejecutados en el entorno de generación (sin Maven ni acceso a dependencias u Oracle).

## Pruebas Vitest (Node 20+ recomendado)

Con identidad en ejecución y un cliente previamente registrado, abrir PowerShell en la carpeta que contiene el pom.xml:

```powershell
cd pruebas-vitest
npm install
$env:CLIENTE_EMAIL = "juan.prueba@gmail.com"
$env:CLIENTE_PASSWORD = "TU_CLAVE_ACTUAL"
npm test
```

Usar un cliente, no un administrador. Si no se configuran credenciales, las dos pruebas autenticadas se omiten. Las pruebas no modifican Oracle. Son pruebas HTTP de integración, no pruebas unitarias de Java ni pruebas de componentes React.
