package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.mapper.UserMapper;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador que implementa el puerto UserRepository
 * Convierte entre User (dominio) y BeanUser (infraestructura)
 */
@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        log.debug("Guardando usuario: {}", user.getEmail());
        BeanUser beanUser = userMapper.toBean(user);
        // Usar saveAndFlush para forzar que JPA envíe el UPDATE/INSERT inmediatamente
        BeanUser savedBeanUser = jpaUserRepository.saveAndFlush(beanUser);
        log.debug("Usuario guardado: id={}, email={}, verificationCode={}", savedBeanUser.getId(), savedBeanUser.getEmail(), savedBeanUser.getVerificationCode());
        return userMapper.toDomain(savedBeanUser);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.debug("Buscando usuario por email: {}", email);
        return jpaUserRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        log.debug("Buscando usuario por ID: {}", id);
        return jpaUserRepository.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        log.debug("Verificando existencia de usuario: {}", email);
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public void deleteByEmail(String email) {
        log.debug("Eliminando usuario por email: {}", email);
        jpaUserRepository.deleteByEmail(email);
    }

    @Override
    public Optional<User> verifyByEmailAndCode(String email, String code) {
        log.debug("Verificando usuario por email y código: {}", email);
        return jpaUserRepository.findByEmailAndVerificationCode(email, code)
                .map(userMapper::toDomain);
    }

    @Override
    public void incrementAttempts(String email) {
        log.debug("Incrementando intentos para usuario: {}", email);
        jpaUserRepository.incrementAttemptsByEmail(email);
    }

    // ============ MÉTODOS ADMINISTRATIVOS ============

    @Override
    public Page<User> findAll(Pageable pageable) {
        log.debug("Buscando todos los usuarios con paginación: page={}, size={} ",
                pageable.getPageNumber(), pageable.getPageSize());
        return jpaUserRepository.findAll(pageable)
                .map(userMapper::toDomain);
    }

    @Override
    public Page<User> findByCriteria(String email, Role role, Boolean verified, Boolean status, Pageable pageable) {
        log.debug("Buscando usuarios por criterios: email={}, role={}, verified={}, status={}", 
                email, role, verified, status);
        return jpaUserRepository.findByCriteria(email, role, verified, status, pageable)
                .map(userMapper::toDomain);
    }

    @Override
    public long countByVerified(boolean verified) {
        log.debug("Contando usuarios por verificación: {}", verified);
        return jpaUserRepository.countByIsVerified(verified);
    }

    @Override
    public long countByStatus(boolean status) {
        log.debug("Contando usuarios por status: {}", status);
        return jpaUserRepository.countByStatus(status);
    }

    @Override
    public List<User> findUnverifiedUsersOlderThan(LocalDateTime cutoffDate) {
        log.debug("Buscando usuarios no verificados anteriores a: {}", cutoffDate);
        return jpaUserRepository.findUnverifiedUsersOlderThan(cutoffDate)
                .stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByRole(Role role) {
        log.debug("Buscando usuarios por rol: {}", role);
        return jpaUserRepository.findByRole(role)
                .stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteUnverifiedUsersOlderThan(LocalDateTime cutoffDate) {
        log.debug("Eliminando usuarios no verificados anteriores a: {}", cutoffDate);
        return jpaUserRepository.deleteUnverifiedUsersOlderThan(cutoffDate);
    }

    @Override
    public long count() {
        log.debug("Contando total de usuarios");
        return jpaUserRepository.count();
    }

    @Override
    public List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        log.debug("Buscando usuarios creados entre {} y {}", start, end);
        return jpaUserRepository.findByCreatedAtBetween(start, end)
                .stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }
}
