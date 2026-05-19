import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

test.describe('Login', () => {
  test('Debe ingresar con credenciales válidas', async ({ page }) => {
    // Navega a login
    await page.goto('/');

    // Llenar formulario
    await page.fill('#login-email', TEST_EMAIL);
    await page.fill('#login-password', TEST_PASSWORD);

    // Click en botón Ingresar
    await page.click('button.btn.primary');

    // Esperar redirección (puede ir a cualquier dashboard según rol)
    await page.waitForNavigation();

    // Validar que NO estamos en login
    expect(page.url()).not.toContain('/login');

    // Validar que token se guardó en localStorage
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();
  });

  test('Debe mostrar error con email vacío', async ({ page }) => {
    await page.goto('/');

    await page.fill('#login-password', TEST_PASSWORD);
    await page.click('button.btn.primary');

    // Esperar mensaje de error
    const error = page.locator('.error');
    await expect(error).toContainText('obligatorios');
  });

  test('Debe mostrar error con contraseña vacía', async ({ page }) => {
    await page.goto('/');

    await page.fill('#login-email', TEST_EMAIL);
    await page.click('button.btn.primary');

    const error = page.locator('.error');
    await expect(error).toContainText('obligatorios');
  });

  test('Debe mostrar error con credenciales inválidas', async ({ page }) => {
    await page.goto('/');

    await page.fill('#login-email', 'wrong@example.com');
    await page.fill('#login-password', 'WrongPassword123!');
    await page.click('button.btn.primary');

    // Esperar error del servidor
    const error = page.locator('.error');
    await expect(error).toBeVisible({ timeout: 3000 });
  });
});
