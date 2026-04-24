package tokai.com.mx.SIGMAV2.modules.users.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setStatus(true);
        user.setAttempts(0);
        user.setVerified(false);
    }

    @Test
    void incrementAttempts_increments_counter() {
        user.incrementAttempts();
        assertThat(user.getAttempts()).isEqualTo(1);
    }

    @Test
    void incrementAttempts_blocks_account_after_three_failures() {
        user.incrementAttempts();
        user.incrementAttempts();
        user.incrementAttempts();

        assertThat(user.isStatus()).isFalse();
        assertThat(user.getLastBlockedAt()).isNotNull();
    }

    @Test
    void incrementAttempts_does_not_block_before_third_failure() {
        user.incrementAttempts();
        user.incrementAttempts();

        assertThat(user.isStatus()).isTrue();
    }

    @Test
    void isBlocked_returns_true_when_blocked_within_five_minutes() {
        user.setStatus(false);
        user.setLastTryAt(LocalDateTime.now().minusMinutes(2));

        assertThat(user.isBlocked()).isTrue();
    }

    @Test
    void isBlocked_returns_false_when_block_expired() {
        user.setStatus(false);
        user.setLastTryAt(LocalDateTime.now().minusMinutes(10));

        assertThat(user.isBlocked()).isFalse();
    }

    @Test
    void isBlocked_returns_false_when_account_active() {
        user.setStatus(true);
        user.setLastTryAt(LocalDateTime.now().minusMinutes(1));

        assertThat(user.isBlocked()).isFalse();
    }

    @Test
    void markAsVerified_sets_verified_and_updatedAt() {
        user.markAsVerified();

        assertThat(user.isVerified()).isTrue();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void resetAttempts_clears_counter_and_activates_account() {
        user.setAttempts(3);
        user.setStatus(false);

        user.resetAttempts();

        assertThat(user.getAttempts()).isZero();
        assertThat(user.isStatus()).isTrue();
    }

    @Test
    void getFullName_concatenates_all_name_parts() {
        user.setName("Juan");
        user.setFirstLastName("Garcia");
        user.setSecondLastName("Lopez");

        assertThat(user.getFullName()).isEqualTo("Juan Garcia Lopez");
    }

    @Test
    void getFullName_handles_missing_second_last_name() {
        user.setName("Ana");
        user.setFirstLastName("Martinez");

        assertThat(user.getFullName()).isEqualTo("Ana Martinez");
    }

    @Test
    void hasCompletePersonalInfo_true_when_name_and_first_last_name_present() {
        user.setName("Carlos");
        user.setFirstLastName("Perez");

        assertThat(user.hasCompletePersonalInfo()).isTrue();
    }

    @Test
    void hasCompletePersonalInfo_false_when_name_missing() {
        user.setFirstLastName("Perez");

        assertThat(user.hasCompletePersonalInfo()).isFalse();
    }
}
