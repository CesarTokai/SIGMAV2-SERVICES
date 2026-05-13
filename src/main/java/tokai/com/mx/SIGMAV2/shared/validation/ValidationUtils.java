package tokai.com.mx.SIGMAV2.shared.validation;

import tokai.com.mx.SIGMAV2.shared.exception.InvalidEmailFormatException;
import tokai.com.mx.SIGMAV2.shared.exception.InvalidRoleException;
import tokai.com.mx.SIGMAV2.shared.exception.WeakPasswordException;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;

import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#\\-_+=])[A-Za-z\\d@$!%*?&.#\\-_+=]{8,}$"
    );

    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidEmailFormatException("El correo electrónico es obligatorio");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailFormatException("El formato del correo electrónico no es válido");
        }
    }

    public static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new WeakPasswordException("La contraseña es obligatoria");
        }

        if (password.length() < 8) {
            throw new WeakPasswordException("La contraseña debe tener al menos 8 caracteres");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new WeakPasswordException(
                "La contraseña debe contener al menos: una mayúscula, una minúscula, un número y un carácter especial (@$!%*?&.#-_+=)"
            );
        }
    }

    public static void validateRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new InvalidRoleException("El rol es obligatorio");
        }

        try {
            ERole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("El rol '" + role + "' no es válido. Roles permitidos: USER, ADMIN, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO");
        }
    }

    public static void validateStringNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' es obligatorio");
        }
    }

    public static void validateVerificationCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new InvalidEmailFormatException("El código de verificación es obligatorio");
        }

        if (!code.matches("\\d{6}")) {
            throw new InvalidEmailFormatException("El código de verificación debe tener exactamente 6 dígitos");
        }
    }
}
