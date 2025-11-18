package tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model.BeanRequestRecoveryPassword;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model.BeanRequestStatus;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.ResponsePageRequestRecoveryDTO;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;

@Repository
public interface IRequestRecoveryPassword extends JpaRepository<BeanRequestRecoveryPassword, Long> {
    int countAllByUserAndStatus(BeanUser user, BeanRequestStatus status);

    @Query("SELECT new tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.ResponsePageRequestRecoveryDTO(" +
            "r.requestId, r.status, r.date, r.user.email, r.user.email, r.user.role) " +
            "FROM BeanRequestRecoveryPassword r " +
            "WHERE r.user.role = :role " +
            "AND r.status = :status " +
            "ORDER BY r.date ASC")
    Page<ResponsePageRequestRecoveryDTO> getRequestByRole(
            @Param("role") ERole role,
            @Param("status") BeanRequestStatus status,
            Pageable pageable);

    @Query("SELECT new tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.ResponsePageRequestRecoveryDTO(" +
            "r.requestId, r.status, r.date, r.user.email, r.user.email, r.user.role) " +
            "FROM BeanRequestRecoveryPassword r " +
            "WHERE r.user.role = :role " +
            "AND r.status IN :statuses " +
            "ORDER BY r.date ASC")
    Page<ResponsePageRequestRecoveryDTO> getRequestByRoleAndStatuses(
            @Param("role") ERole role,
            @Param("statuses") java.util.List<BeanRequestStatus> statuses,
            Pageable pageable);
}
