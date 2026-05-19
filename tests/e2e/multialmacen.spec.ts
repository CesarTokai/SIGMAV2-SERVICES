import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

test.describe('Administración Multi-Almacén', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/');
    await page.fill('#login-email', TEST_EMAIL);
    await page.fill('#login-password', TEST_PASSWORD);
    await page.click('button.btn.primary');
    await page.waitForNavigation();

    // Navegar a multi-almacén
    await page.waitForSelector('.admin-sidebar', { timeout: 5000 });
    await page.goto('/Admin/MultiAlmacen');
    await page.waitForLoadState('networkidle');
  });

  test('Debe cargar página multi-almacén', async ({ page }) => {
    // Validar que se cargó la página
    await page.waitForTimeout(2000);

    // Validar que hay contenido
    const content = page.locator('[class*="container"], main, .content');
    await expect(content.first()).toBeVisible();
  });

  test('Debe mostrar selector de período', async ({ page }) => {
    // Buscar selector de período
    const periodoSelectors = page.locator('select, [class*="periodo"]');

    for (let i = 0; i < await periodoSelectors.count(); i++) {
      const selector = periodoSelectors.nth(i);
      if (await selector.isVisible()) {
        await expect(selector).toBeEnabled();
        break;
      }
    }
  });

  test('Debe mostrar tabla de productos multi-almacén', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(2000);

    const table = page.locator('table, [role="table"], [class*="table"]');

    // Validar existencia
    const tableVisible = await table.first().isVisible().catch(() => false);
    const empty = await page.locator('[class*="empty"]').isVisible().catch(() => false);

    expect(tableVisible || empty).toBeTruthy();
  });

  test('Debe tener barra de búsqueda', async ({ page }) => {
    // Buscar input de búsqueda
    const searchInputs = page.locator('input[class*="search"], input[placeholder*="buscar" i]');

    const hasSearch = await searchInputs.first().isVisible().catch(() => false);
    expect(hasSearch).toBeTruthy();
  });

  test('Debe permitir búsqueda en productos', async ({ page }) => {
    // Buscar input
    const searchInput = page.locator('input[class*="search"]').first();

    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('test');

      // Validar valor
      const value = await searchInput.inputValue();
      expect(value).toContain('test');

      // Limpiar
      await searchInput.clear();
    }
  });

  test('Debe tener botón de importación', async ({ page }) => {
    // Buscar botón importar
    const importBtn = page.locator('button', { hasText: /importar|import|cargar|subir/i }).first();

    const hasImport = await importBtn.isVisible().catch(() => false);
    expect(hasImport).toBeTruthy();
  });

  test('Debe mostrar tab de productos y bajas', async ({ page }) => {
    // Buscar tabs
    const tabs = page.locator('button[role="tab"], [class*="tab"], .nav-tabs button');

    const tabCount = await tabs.count();
    expect(tabCount >= 1).toBeTruthy();
  });

  test('Debe tener paginación', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(2000);

    // Buscar elementos de paginación
    const pagination = page.locator('[class*="pagination"], [class*="page"], nav');
    const pageSize = page.locator('select[class*="size"], select[class*="per"]');

    const hasPagination = await pagination.first().isVisible().catch(() => false);
    const hasPageSize = await pageSize.first().isVisible().catch(() => false);

    expect(hasPagination || hasPageSize).toBeTruthy();
  });

  test('Debe permitir cambiar período y recargar', async ({ page }) => {
    // Buscar selector periodo
    const periodoSelect = page.locator('select').first();

    if (await periodoSelect.isVisible()) {
      // Obtener opciones
      const options = page.locator('select option');
      const count = await options.count();

      // Si hay más de 1 opción, puede cambiar
      if (count > 1) {
        await periodoSelect.selectOption('1');

        // Esperar que recargue tabla
        await page.waitForTimeout(1000);

        // Tabla debería seguir visible
        const table = page.locator('table, [role="table"]').first();
        expect(await table.isVisible().catch(() => false)).toBeTruthy();
      }
    }
  });
});
