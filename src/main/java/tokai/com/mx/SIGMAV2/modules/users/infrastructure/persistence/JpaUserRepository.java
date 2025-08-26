package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.transaction.annotation.Transactional;

import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {


    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.attempts = u.attempts + 1 WHERE u.username = :username")
    void incrementAttempts(String attempts);
    Optional<BeanUser> verifyByEmailAndCode(String email, String code);
    void deleteByEmail(String email);
    Optional<BeanUser> findByEmailAndCode(String email, String code);
}
