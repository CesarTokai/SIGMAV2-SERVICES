/**
 * Checks if the provided email string is in a valid email format.
 *
 * @param email - The email string to validate
 * @return true if the email is valid, false otherwise
 */
export function isValidEmail(email: string): boolean {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
}

/**
 * Checks if the provided phone string is a valid phone number with 10 digits.
 *
 * @param phone - The phone number string to validate
 * @return true if the phone number is valid, false otherwise
 */
export function isValidPhoneNumber(phone: string): boolean {
    const regex = /^\d{10}$/; // 10 dígitos (ejemplo)
    return regex.test(phone);
}

/**
 * Checks if the provided string is a valid name (letters, spaces, and certain special characters).
 *
 * @param name - The name string to validate
 * @return true if the name is valid, false otherwise
 */
export function isValidName(name: string): boolean {
    const regex = /^[A-Za-z\s'-]+$/;
    return regex.test(name);
}

/**
 * Checks if the provided string is a strong password.
 * Must be at least 8 characters long, contain at least one uppercase letter,
 * one lowercase letter, one number, and one special character.
 *
 * @param password - The password string to validate
 * @return true if the password is strong, false otherwise
 */
export function isStrongPassword(password: string): boolean {
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#\-_+=])[A-Za-z\d@$!%*?&.#\-_+=]{8,}$/;
    return regex.test(password);
}

/**
 * Checks if the provided string is a valid date in the format YYYY-MM-DD.
 *
 * @param date - The date string to validate
 * @return true if the date is valid, false otherwise
 */
export function isValidDate(date: string): boolean {
    const regex = /^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/;
    return regex.test(date);
}

/**
 * Checks if the provided string is a valid hexadecimal color code.
 *
 * @param hex - The hexadecimal color string to validate
 * @return true if the hex color code is valid, false otherwise
 */
export function isValidHexColor(hex: string): boolean {
    const regex = /^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$/;
    return regex.test(hex);
}
