import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

test.describe('Dashboard Admin', () => {
  test.beforeEach(async ({ page }) => {
    // Login antes de cada test
    await page.goto('/');
    await page.fill('#login-email', TEST_EMAIL);
    await page.fill('#login-password', TEST_PASSWORD);
    await page.click('button.btn.primary');
    await page.waitForNavigation();
  });

  test('Debe mostrar dashboard después de login exitoso', async ({ page }) => {
    // Validar URL cambió
    expect(page.url()).not.toContain('/login');

    // Validar token se guardó
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();

    // Validar sidebar presente (puede tardar en cargar)
    const sidebar = page.locator('.admin-sidebar');
    await expect(sidebar).toBeVisible({ timeout: 5000 });
  });

  test('Debe mostrar información del usuario en sidebar', async ({ page }) => {
    // Validar nombre de usuario visible
    const userInfo = page.locator('.user-details');
    await expect(userInfo).toBeVisible();

    // Validar rol mostrado
    const userRole = page.locator('text=Administrador');
    await expect(userRole).toBeVisible();
  });

  test('Debe tener botón de cerrar sesión', async ({ page }) => {
    // Click en botón logout
    const logoutBtn = page.locator('button.btn-logout');
    await expect(logoutBtn).toBeVisible();

    // Click y esperar cambios
    await logoutBtn.click();

    // Esperar a que el token se limpie
    await page.waitForTimeout(1500);

    // Validar token removido del localStorage
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeFalsy();
  });

  test('Debe tener menú de navegación visible', async ({ page }) => {
    const navList = page.locator('.nav-list');
    await expect(navList).toBeVisible();

    // Validar que hay elementos de menú
    const navItems = page.locator('.nav-list li');
    const count = await navItems.count();
    expect(count).toBeGreaterThan(0);
  });

  test('Debe permanecer autenticado al refrescar', async ({ page }) => {
    // Refrescar página
    await page.reload();
    await page.waitForLoadState('networkidle');

    // Debe seguir autenticado
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();

    // No debe ir a login
    expect(page.url()).not.toContain('/login');
  });

});
