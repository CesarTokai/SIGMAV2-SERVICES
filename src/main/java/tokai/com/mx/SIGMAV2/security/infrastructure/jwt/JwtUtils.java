package tokai.com.mx.SIGMAV2.security.infrastructure.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import tokai.com.mx.SIGMAV2.shared.exception.CustomException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
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
                .collect(Collectors.joining());

        String jwtToken = JWT.create()
                .withIssuer(userGenerator) // El dueño del token
                .withSubject(username) // a quien se le da el token
                .withClaim("authorities", role)  // autorizacion del token
                .withIssuedAt(new Date()) // fecha de creacion del token
                .withExpiresAt(new Date(System.currentTimeMillis() + 86400000)) // expiracion del token
                .withJWTId(UUID.randomUUID().toString()) // id del token
                .withNotBefore(new Date(System.currentTimeMillis())) // ES VALIDO DESDE AHORA
                .sign(algorithm); // firma del token

        return jwtToken;
    }

    public DecodedJWT validateToken(String token){
        try {
            // traer el algoritmo
            Algorithm algorithm = Algorithm.HMAC256(this.privateKey);

            // verificar el token
            return JWT.require(algorithm)
                    .withIssuer(userGenerator)
                    .build()
                    .verify(token);

        }catch (JWTVerificationException e){
            throw  new CustomException("invalid token");
        }
    }

    public String extractUsername(DecodedJWT decodedJWT){
        return decodedJWT.getSubject().toString();
    }

    public Map<String, Claim> extractClaims(DecodedJWT decodedJWT){
        return decodedJWT.getClaims();
    }
}