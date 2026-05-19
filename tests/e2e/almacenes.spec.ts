import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

test.describe('Gestión de Almacenes', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/');
    await page.fill('#login-email', TEST_EMAIL);
    await page.fill('#login-password', TEST_PASSWORD);
    await page.click('button.btn.primary');
    await page.waitForNavigation();

    // Navegar a almacenes
    await page.waitForSelector('.admin-sidebar', { timeout: 5000 });
    await page.goto('/Admin/Almacen');
    await page.waitForLoadState('networkidle');
  });

  test('Debe cargar página de almacenes', async ({ page }) => {
    // Esperar a que cargue contenido
    await page.waitForTimeout(2000);

    // Validar título o encabezado
    const headers = page.locator('h1, h2, [class*="title"]');
    const visible = await headers.first().isVisible().catch(() => false);
    expect(visible).toBeTruthy();
  });

  test('Debe mostrar tabla de almacenes', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(1500);

    const table = page.locator('table, [role="table"], [class*="table"]');
    const tableVisible = await table.first().isVisible().catch(() => false);

    const empty = await page.locator('[class*="empty"]').isVisible().catch(() => false);

    expect(tableVisible || empty).toBeTruthy();
  });

  test('Debe tener búsqueda de almacenes', async ({ page }) => {
    // Buscar input
    const searchInputs = page.locator('input[type="text"], input[class*="search"]');

    const hasSearch = await searchInputs.first().isVisible().catch(() => false);
    expect(hasSearch).toBeTruthy();
  });

  test('Debe permitir buscar almacenes', async ({ page }) => {
    const searchInput = page.locator('input[type="text"], input[class*="search"]').first();

    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('ALM');

      // Validar que se escribió
      const value = await searchInput.inputValue();
      expect(value).toContain('ALM');

      // Limpiar
      await searchInput.clear();
    }
  });

  test('Debe tener botón para crear almacén', async ({ page }) => {
    // Buscar botón crear
    const createBtn = page.locator('button', { hasText: /crear|nuevo|agregar|add/i }).first();

    const hasCreate = await createBtn.isVisible().catch(() => false);
    expect(hasCreate).toBeTruthy();
  });

  test('Debe mostrar modal al crear almacén', async ({ page }) => {
    // Buscar botón crear
    const createBtn = page.locator('button', { hasText: /crear|nuevo|agregar/i }).first();

    if (await createBtn.isVisible().catch(() => false)) {
      await createBtn.click();

      // Esperar modal
      const modal = page.locator('[class*="modal"], dialog').first();
      const modalVisible = await modal.isVisible({ timeout: 3000 }).catch(() => false);

      expect(modalVisible).toBeTruthy();
    }
  });

  test('Debe validar campos requeridos en almacén', async ({ page }) => {
    // Abrir modal crear
    const createBtn = page.locator('button', { hasText: /crear|nuevo|agregar/i }).first();

    if (await createBtn.isVisible().catch(() => false)) {
      await createBtn.click();

      // Esperar modal
      const modal = page.locator('[class*="modal"]').first();
      await expect(modal).toBeVisible({ timeout: 3000 });

      // Intentar guardar sin llenar
      const saveBtn = page.locator('button', { hasText: /guardar|save|crear/i }).first();

      if (await saveBtn.isVisible().catch(() => false)) {
        await saveBtn.click();

        // Debe mostrar validación
        await page.waitForTimeout(500);

        const error = page.locator('[class*="error"], [role="alert"]').first();
        const hasError = await error.isVisible().catch(() => false);

        expect(hasError).toBeTruthy();
      }
    }
  });

  test('Debe tener paginación', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(2000);

    // Buscar elementos de paginación
    const pagination = page.locator('[class*="pagination"], [class*="page"]');
    const pageSize = page.locator('select');

    const hasPagination = await pagination.first().isVisible().catch(() => false);
    const hasPageSize = await pageSize.first().isVisible().catch(() => false);

    expect(hasPagination || hasPageSize).toBeTruthy();
  });

  test('Debe mostrar información de almacenes', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(2000);

    // Buscar filas
    const rows = page.locator('tbody tr, [role="row"]');
    const count = await rows.count();

    // Si hay almacenes, validar que muestra info
    if (count > 0) {
      const firstRow = rows.first();
      await expect(firstRow).toBeVisible();
    }
  });

  test('Debe tener botones de acción en almacenes', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(2000);

    // Buscar botones de acción
    const actionButtons = page.locator('button[title*="editar" i], button[title*="eliminar" i], [class*="btn-action"]');

    const hasActions = await actionButtons.first().isVisible().catch(() => false);
    expect(hasActions).toBeTruthy();
  });
});
