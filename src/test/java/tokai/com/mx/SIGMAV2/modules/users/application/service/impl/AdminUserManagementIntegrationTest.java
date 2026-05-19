package tokai.com.mx.SIGMAV2.modules.users.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.RegisterUserCommand;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.shared.exception.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminUserManagementIntegrationTest {

    @Autowired private UserService userService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Crear usuario de prueba a través del servicio
        testUser = userService.register(new RegisterUserCommand(
                "test" + System.nanoTime() + "@test.com",
                "TestPassword123!",
                "AUXILIAR",
                "Test User",
                "First",
                "Last",
                "1234567890",
                null,
                true,
                true
        ));

        // Crear usuario admin para contexto de seguridad
        adminUser = userService.register(new RegisterUserCommand(
                "admin" + System.nanoTime() + "@test.com",
                "AdminPassword123!",
                "ADMINISTRADOR",
                "Admin User",
                "Admin",
                "User",
                "0987654321",
                null,
                true,
                true
        ));

        // Mock security con usuario admin
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(adminUser.getEmail());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ───────────────────── LIST USERS ─────────────────────

    @Test
    void listUsers_success_with_valid_pagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = userService.findAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    void listUsers_success_empty_page() {
        Pageable pageable = PageRequest.of(100, 10);
        Page<User> result = userService.findAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    // ───────────────────── CREATE USER ─────────────────────

    @Test
    void createUser_success_with_valid_data() {
        RegisterUserCommand command = new RegisterUserCommand(
                "newuser" + System.nanoTime() + "@test.com",
                "ValidPassword123!",
                "AUXILIAR",
                "New User",
                "First",
                "Last",
                "1234567890",
                null,
                true,
                true
        );

        User result = userService.register(command);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(command.getEmail());
        assertThat(result.getRole()).isEqualTo(Role.AUXILIAR);
    }

    @Test
    void createUser_throws_when_email_already_exists() {
        RegisterUserCommand command = new RegisterUserCommand(
                testUser.getEmail(),
                "ValidPassword123!",
                "AUXILIAR",
                "Another User",
                "First",
                "Last",
                "1234567890",
                null,
                true,
                true
        );

        assertThatThrownBy(() -> userService.register(command))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void createUser_throws_when_password_weak() {
        RegisterUserCommand command = new RegisterUserCommand(
                "unique" + System.nanoTime() + "@test.com",
                "weak",
                "AUXILIAR",
                "User",
                "First",
                "Last",
                "1234567890",
                null,
                true,
                true
        );

        assertThatThrownBy(() -> userService.register(command))
                .isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void createUser_throws_when_invalid_email() {
        RegisterUserCommand command = new RegisterUserCommand(
                "notanemail",
                "ValidPassword123!",
                "AUXILIAR",
                "User",
                "First",
                "Last",
                "1234567890",
                null,
                true,
                true
        );

        assertThatThrownBy(() -> userService.register(command))
                .isInstanceOf(InvalidEmailFormatException.class);
    }

    @Test
    void createUser_success_with_all_valid_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            RegisterUserCommand command = new RegisterUserCommand(
                    "user" + System.nanoTime() + "_" + role + "@test.com",
                    "ValidPassword123!",
                    role,
                    "User for " + role,
                    "First",
                    "Last",
                    "1234567890",
                    null,
                    true,
                    true
            );

            User result = userService.register(command);

            assertThat(result).isNotNull();
            assertThat(result.getRole().name()).isEqualTo(role);
        }
    }

    // ───────────────────── TOGGLE USER STATUS ─────────────────────

    @Test
    void toggleUserStatus_success_deactivate() {
        assertThat(testUser.isStatus()).isTrue();

        User result = userService.toggleUserStatus(testUser.getId());

        assertThat(result).isNotNull();
        assertThat(result.isStatus()).isFalse();
    }

    @Test
    void toggleUserStatus_success_activate() {
        // Primero desactivar
        userService.toggleUserStatus(testUser.getId());

        // Luego activar
        User result = userService.toggleUserStatus(testUser.getId());

        assertThat(result).isNotNull();
        assertThat(result.isStatus()).isTrue();
    }

    @Test
    void toggleUserStatus_throws_when_user_not_found() {
        assertThatThrownBy(() -> userService.toggleUserStatus(999L))
                .isInstanceOf(RuntimeException.class);
    }

    // ───────────────────── CHANGE USER ROLE ─────────────────────

    @Test
    void changeUserRole_success() {
        User result = userService.updateUserRole(testUser.getId(), "ALMACENISTA");

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(Role.ALMACENISTA);
    }

    @Test
    void changeUserRole_throws_when_user_not_found() {
        assertThatThrownBy(() -> userService.updateUserRole(999L, "ALMACENISTA"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void changeUserRole_throws_when_invalid_role() {
        assertThatThrownBy(() -> userService.updateUserRole(testUser.getId(), "INVALID_ROLE"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void changeUserRole_success_with_all_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            User result = userService.updateUserRole(testUser.getId(), role);

            assertThat(result).isNotNull();
            assertThat(result.getRole().name()).isEqualTo(role);
        }
    }

    // ───────────────────── DELETE USER ─────────────────────

    @Test
    void deleteUser_success() {
        Long userId = testUser.getId();
        String email = testUser.getEmail();

        userService.deleteByEmail(email);

        assertThat(userService.findById(userId)).isEmpty();
    }

    @Test
    void deleteUser_throws_when_user_not_found() {
        assertThatThrownBy(() -> userService.deleteByEmail("nonexistent@test.com"))
                .isInstanceOf(RuntimeException.class);
    }

    // ───────────────────── RESET ATTEMPTS ─────────────────────

    @Test
    void resetUserAttempts_success() {
        // Los intentos se incrementan en el modelo
        User result = userService.resetUserAttempts(testUser.getId());

        assertThat(result).isNotNull();
        assertThat(result.getAttempts()).isZero();
    }

    @Test
    void resetUserAttempts_throws_when_user_not_found() {
        assertThatThrownBy(() -> userService.resetUserAttempts(999L))
                .isInstanceOf(RuntimeException.class);
    }

    // ───────────────────── FORCE VERIFY USER ─────────────────────

    @Test
    void forceVerifyUser_success() {
        // Crear un usuario no verificado
        User unverifiedUser = userService.register(new RegisterUserCommand(
                "unverified" + System.nanoTime() + "@test.com",
                "UnverifiedPassword123!",
                "AUXILIAR",
                "Unverified User",
                "First",
                "Last",
                "5555555555",
                null,
                true,
                false
        ));

        User result = userService.forceVerifyUser(unverifiedUser.getId());

        assertThat(result).isNotNull();
        assertThat(result.isVerified()).isTrue();
    }

    @Test
    void forceVerifyUser_throws_when_user_not_found() {
        assertThatThrownBy(() -> userService.forceVerifyUser(999L))
                .isInstanceOf(RuntimeException.class);
    }

}
