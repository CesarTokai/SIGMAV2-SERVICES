package tokai.com.mx.SIGMAV2.security.infrastructure.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import tokai.com.mx.SIGMAV2.shared.exception.CustomException;
import tokai.com.mx.SIGMAV2.security.infrastructure.exception.JwtException;
import tokai.com.mx.SIGMAV2.security.infrastructure.exception.TokenExpiredException;
import tokai.com.mx.SIGMAV2.security.infrastructure.exception.TokenInvalidException;
import tokai.com.mx.SIGMAV2.security.infrastructure.exception.TokenMalformedException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtUtils {
    @Value("${security.jwt.key.private}")
    private String privateKey;

    @Value("${security.jwt.user.generator}")
    private String userGenerator;

    public String createToken(Authentication authentication){
        Algorithm algorithm = Algorithm.HMAC256(this.privateKey);

        // Obtener el usuario
        String username = authentication.getPrincipal().toString();
        // Obtener el rol
        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth == null ? "" : (auth.startsWith("ROLE_") ? auth : "ROLE_" + auth))
                .collect(Collectors.joining(","));

        return JWT.create()
                .withIssuer(userGenerator) // El dueño del token
                .withSubject(username) // a quien se le da el token
                .withClaim("authorities", role)  // autorizacion del token
                .withIssuedAt(new Date()) // fecha de creacion del token
                .withExpiresAt(new Date(System.currentTimeMillis() + 86400000)) // expiracion del token
                .withJWTId(UUID.randomUUID().toString()) // id del token
                .withNotBefore(new Date(System.currentTimeMillis())) // ES VALIDO DESDE AHORA
                .sign(algorithm); // firma del token
    }

    /**
     * Valida el token JWT verificando firma, expiración y claims
     *
     * IMPORTANTE - Validación de Expiración:
     * - El token contiene un claim 'exp' (fecha de expiración) generado al crear el token
     * - Esta validación compara 'exp' con la fecha/hora actual del servidor
     * - Si exp < now() → lanza TokenExpiredException
     * - NO consulta base de datos - la validación es local y rápida
     *
     * Flujo de validación:
     * 1. Verifica firma HMAC256 (token no ha sido modificado)
     * 2. Verifica issuer (generado por este servidor)
     * 3. Verifica exp > now() (token no ha expirado) ← EXPIRACIÓN NATURAL
     * 4. Verifica nbf <= now() (token ya es válido)
     *
     * @param token Token JWT en formato string
     * @return DecodedJWT con los claims del token
     * @throws TokenExpiredException si exp < now() (expiración natural por tiempo)
     * @throws TokenInvalidException si la firma es inválida o claims incorrectos
     * @throws TokenMalformedException si el formato del token es inválido
     */
    public DecodedJWT validateToken(String token){
        try {
            // traer el algoritmo
            Algorithm algorithm = Algorithm.HMAC256(this.privateKey);

            // verificar el token
            return JWT.require(algorithm)
                    .withIssuer(userGenerator)
                    .build()
                    .verify(token);

        } catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
            log.warn("Token expirado: {}", e.getMessage());
            java.time.LocalDateTime expiredAt = e.getExpiredOn().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            throw new TokenExpiredException(e.getMessage(), expiredAt);
        } catch (SignatureVerificationException e) {
            log.warn("Firma del token inválida: {}", e.getMessage());
            throw new TokenInvalidException("Firma del token inválida", "El token ha sido modificado o no fue generado por este servidor");
        } catch (AlgorithmMismatchException e) {
            log.warn("Algoritmo del token no coincide: {}", e.getMessage());
            throw new TokenInvalidException("Algoritmo del token no coincide", "El algoritmo usado para firmar el token no es válido");
        } catch (InvalidClaimException e) {
            log.warn("Claim del token inválido: {}", e.getMessage());
            throw new TokenInvalidException("Claim del token inválido", "Los claims del token no son válidos");
        } catch (JWTDecodeException e) {
            log.warn("Token malformado: {}", e.getMessage());
            throw new TokenMalformedException("Token malformado", "El formato del token no es válido");
        } catch (JWTVerificationException e) {
            log.warn("Error de verificación JWT: {}", e.getMessage());
            throw new TokenInvalidException("Error de verificación JWT", "No se pudo verificar la autenticidad del token");
        } catch (Exception e) {
            log.error("Error inesperado validando token: {}", e.getMessage());
            throw new TokenInvalidException("Error inesperado validando token", "Ha ocurrido un error interno al validar el token");
        }
    }

    /**
     * Valida token y retorna boolean (para casos donde no necesitas el DecodedJWT)
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (JwtException e) {
            log.error("JwtException en isTokenValid: {}", e.getMessage());
            return false;
        } catch (CustomException e) {
            log.error("CustomException en isTokenValid: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Exception inesperada en isTokenValid: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(DecodedJWT decodedJWT){
        return decodedJWT.getSubject();
    }

    public Map<String, Claim> extractClaims(DecodedJWT decodedJWT){
        return decodedJWT.getClaims();
    }

    /**
     * Extrae username directamente del token (método helper)
     */
    public String getUsernameFromToken(String token) {
        try {
            DecodedJWT decodedJWT = validateToken(token);
            return extractUsername(decodedJWT);
        } catch (JwtException | CustomException e) {
            log.warn("No se pudo extraer username del token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrae rol directamente del token (método helper)
     */
    public String getRoleFromToken(String token) {
        try {
            DecodedJWT decodedJWT = validateToken(token);
            Claim authoritiesClaim = decodedJWT.getClaim("authorities");
            return authoritiesClaim != null ? authoritiesClaim.asString() : null;
        } catch (JwtException | CustomException e) {
            log.warn("No se pudo extraer rol del token: {}", e.getMessage());
            return null;
        }
    }
}