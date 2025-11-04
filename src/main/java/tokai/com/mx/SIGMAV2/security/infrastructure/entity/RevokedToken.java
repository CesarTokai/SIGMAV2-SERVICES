package tokai.com.mx.SIGMAV2.security.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "revoked_tokens", indexes = {
    @Index(name = "idx_revoked_jti", columnList = "jti"),
    @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String jti; // JWT ID único

    @Column(nullable = false)
    private Instant revokedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(length = 100)
    private String reason; // LOGOUT, SECURITY_BREACH, ADMIN_REVOCATION, etc.

    @Column(length = 255)
    private String username; // Usuario asociado (opcional)
}

