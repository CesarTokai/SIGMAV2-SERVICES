import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

test.describe('Gestión de Períodos', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/');
    await page.fill('#login-email', TEST_EMAIL);
    await page.fill('#login-password', TEST_PASSWORD);
    await page.click('button.btn.primary');
    await page.waitForNavigation();

    // Navegar a períodos (debe estar por defecto en el dashboard)
    await page.waitForSelector('.admin-sidebar', { timeout: 5000 });
  });

  test('Debe mostrar página de períodos', async ({ page }) => {
    // Validar título específico (h1)
    const title = page.locator('h1.page-title');
    await expect(title).toBeVisible({ timeout: 3000 });

    // Validar botón agregar
    const addBtn = page.locator('button.btn-add');
    await expect(addBtn).toBeVisible();
  });

  test('Debe abrir modal para crear período', async ({ page }) => {
    // Click en botón "Agregar Período"
    const addBtn = page.locator('button.btn-add');
    await addBtn.click();

    // Esperar modal y validar contenido
    const modal = page.locator('.modal-content');
    await expect(modal).toBeVisible({ timeout: 3000 });

    // Validar campos
    const fechaInput = page.locator('#fecha');
    const comentariosInput = page.locator('#comentarios');

    await expect(fechaInput).toBeVisible();
    await expect(comentariosInput).toBeVisible();
  });

  test('Debe mostrar error si comentarios son muy cortos', async ({ page }) => {
    // Abrir modal
    const addBtn = page.locator('button.btn-add');
    await addBtn.click();

    const modal = page.locator('.modal-content');
    await expect(modal).toBeVisible();

    // Llenar con datos inválidos
    const today = new Date().toISOString().split('T')[0];
    await page.fill('#fecha', today);
    await page.fill('#comentarios', 'Corto');

    // Intentar guardar
    const saveBtn = page.locator('button.btn-save');
    await saveBtn.click();

    // Validar error
    await page.waitForTimeout(500);
    const error = page.locator('.error-message');
    await expect(error).toBeVisible();
  });

  test('Debe validar que fecha es requerida', async ({ page }) => {
    const addBtn = page.locator('button.btn-add');
    await addBtn.click();

    const modal = page.locator('.modal-content');
    await expect(modal).toBeVisible();

    // Solo llenar comentarios, dejar fecha vacía
    await page.fill('#comentarios', 'Comentario sin fecha pero con suficientes caracteres');

    const saveBtn = page.locator('button.btn-save');
    await saveBtn.click();

    // Validar error
    await page.waitForTimeout(500);
    const error = page.locator('.error-message');
    await expect(error).toBeVisible();
  });

  test('Debe mostrar búsqueda', async ({ page }) => {
    // Validar que existe la barra de búsqueda
    const searchInput = page.locator('.search-input');
    await expect(searchInput).toBeVisible();

    // Escribir en búsqueda
    await searchInput.fill('test');

    // Validar que el campo tiene el valor
    const value = await searchInput.inputValue();
    expect(value).toBe('test');
  });

  test('Debe mostrar tabla de períodos', async ({ page }) => {
    // Validar tabla presente
    const table = page.locator('.data-table, .empty-state');
    await expect(table).toBeVisible();

    // Validar que tiene encabezados o mensaje vacío
    const headers = page.locator('th');
    const isEmpty = page.locator('.empty-state');

    const headerCount = await headers.count();
    const isEmptyVisible = await isEmpty.isVisible().catch(() => false);

    expect(headerCount > 0 || isEmptyVisible).toBeTruthy();
  });
});
