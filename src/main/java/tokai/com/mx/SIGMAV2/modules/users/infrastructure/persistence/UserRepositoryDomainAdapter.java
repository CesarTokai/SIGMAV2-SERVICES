package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.mapper.UserDomainMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador que implementa el puerto de salida UserRepository
 * Traduce entre el modelo de dominio y la infraestructura JPA
 */
@Repository
public class UserRepositoryDomainAdapter implements UserRepository {
    
    private final JpaUserRepository jpaRepository;
    private final UserDomainMapper mapper;

    public UserRepositoryDomainAdapter(JpaUserRepository jpaRepository, UserDomainMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        var entity = mapper.toEntity(user);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toDomain);
    }
    
    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public void incrementAttempts(String email) {
        jpaRepository.incrementAttemptsByEmail(email);
    }

    @Override
    public Optional<User> verifyByEmailAndCode(String email, String code) {
        return jpaRepository.findByEmailAndVerificationCode(email, code)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByEmail(String email) {
        jpaRepository.deleteByEmail(email);
    }

    // ============ MÉTODOS ADMINISTRATIVOS ============

    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<User> findByCriteria(String email, Role role, Boolean verified, Boolean status, Pageable pageable) {
        // Convertir Role del dominio a ERole de infraestructura
        tokai.com.mx.SIGMAV2.modules.users.model.ERole eRole = role != null ? mapper.mapRoleToEntity(role) : null;
        return jpaRepository.findByCriteria(email, eRole, verified, status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public long countByVerified(boolean verified) {
        return jpaRepository.countByIsVerified(verified);
    }

    @Override
    public long countByStatus(boolean status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public List<User> findUnverifiedUsersOlderThan(LocalDateTime cutoffDate) {
        return jpaRepository.findUnverifiedUsersOlderThan(cutoffDate)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByRole(Role role) {
        // Convertir Role del dominio a ERole de infraestructura
        tokai.com.mx.SIGMAV2.modules.users.model.ERole eRole = mapper.mapRoleToEntity(role);
        return jpaRepository.findByRole(eRole)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteUnverifiedUsersOlderThan(LocalDateTime cutoffDate) {
        return jpaRepository.deleteUnverifiedUsersOlderThan(cutoffDate);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByCreatedAtBetween(start, end)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
