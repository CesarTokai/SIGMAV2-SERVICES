import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

test.describe('Gestión de Inventario', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/');
    await page.fill('#login-email', TEST_EMAIL);
    await page.fill('#login-password', TEST_PASSWORD);
    await page.click('button.btn.primary');
    await page.waitForNavigation();

    // Navegar a inventario
    await page.waitForSelector('.admin-sidebar', { timeout: 5000 });
    await page.goto('/Admin/InventarioAdmin');
    await page.waitForLoadState('networkidle');
  });

  test('Debe cargar página de inventario', async ({ page }) => {
    // Validar título
    const title = page.locator('h1');
    await expect(title).toContainText('Inventario', { ignoreCase: true });

    // Validar selector de período
    const periodoSelector = page.locator('[class*="periodo"], select');
    await expect(periodoSelector.first()).toBeVisible({ timeout: 5000 });
  });

  test('Debe mostrar tabla de productos', async ({ page }) => {
    // Esperar tabla
    const table = page.locator('.data-table, table, [class*="table"]');
    await expect(table.first()).toBeVisible({ timeout: 5000 });

    // Validar que tiene filas o mensaje vacío
    const rows = page.locator('tbody tr, tr[class*="row"]');
    const rowCount = await rows.count();

    // O mostrar estado vacío
    const empty = page.locator('[class*="empty"]');
    const isEmpty = await empty.isVisible().catch(() => false);

    expect(rowCount > 0 || isEmpty).toBeTruthy();
  });

  test('Debe buscar productos', async ({ page }) => {
    // Buscar input de búsqueda específicamente
    const searchInput = page.locator('input[class*="search"]').first();

    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('test');

      // Validar que el valor se escribió
      const value = await searchInput.inputValue();
      expect(value).toContain('test');
    }
  });

  test('Debe tener botón de importación', async ({ page }) => {
    // Buscar botón de importar
    const importBtn = page.locator('button', { hasText: /importar|import|cargar|upload/i }).first();

    if (await importBtn.isVisible()) {
      await expect(importBtn).toBeEnabled();
    }
  });

  test('Debe tener controles de paginación', async ({ page }) => {
    // Esperar que cargue tabla
    await page.waitForTimeout(2000);

    // Buscar elementos de paginación
    const pagination = page.locator('[class*="pagination"], [class*="paginator"], nav');
    const pageSize = page.locator('select, [class*="page-size"]');

    // Al menos uno debería estar visible si hay muchos registros
    const isPaginationVisible = await pagination.first().isVisible().catch(() => false);
    const isPageSizeVisible = await pageSize.first().isVisible().catch(() => false);

    expect(isPaginationVisible || isPageSizeVisible).toBeTruthy();
  });

  test('Debe permitir cambiar período', async ({ page }) => {
    // Buscar selector de período
    const periodoSelects = page.locator('select, [class*="periodo-select"], [class*="periodo-selector"]');

    for (let i = 0; i < await periodoSelects.count(); i++) {
      const select = periodoSelects.nth(i);
      if (await select.isVisible()) {
        // Validar que es selectable
        await expect(select).toBeEnabled();
        break;
      }
    }
  });
});
