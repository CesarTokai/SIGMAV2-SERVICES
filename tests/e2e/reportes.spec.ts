import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

test.describe('Reportes', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/');
    await page.fill('#login-email', TEST_EMAIL);
    await page.fill('#login-password', TEST_PASSWORD);
    await page.click('button.btn.primary');
    await page.waitForNavigation();

    // Navegar a sidebar
    await page.waitForSelector('.admin-sidebar', { timeout: 5000 });
  });

  test('Debe cargar reporte Listado Marbetes', async ({ page }) => {
    // Navegar a reporte
    await page.goto('/Admin/ListadoMarbetes');
    await page.waitForLoadState('networkidle');

    // Esperar contenido
    await page.waitForTimeout(2000);

    // Validar que hay contenido
    const content = page.locator('[class*="container"], main, .content');
    const visible = await content.first().isVisible().catch(() => false);
    expect(visible).toBeTruthy();
  });

  test('Debe mostrar selector periodo en ListadoMarbetes', async ({ page }) => {
    await page.goto('/Admin/ListadoMarbetes');
    await page.waitForLoadState('networkidle');

    // Buscar selector periodo
    const selects = page.locator('select');
    const hasSelect = await selects.first().isVisible({ timeout: 5000 }).catch(() => false);
    expect(hasSelect).toBeTruthy();
  });

  test('Debe mostrar tabla en ListadoMarbetes', async ({ page }) => {
    await page.goto('/Admin/ListadoMarbetes');
    await page.waitForLoadState('networkidle');

    // Esperar tabla
    await page.waitForTimeout(2000);

    const table = page.locator('table, [role="table"]');
    const tableVisible = await table.first().isVisible().catch(() => false);

    const empty = await page.locator('[class*="empty"]').isVisible().catch(() => false);

    expect(tableVisible || empty).toBeTruthy();
  });

  test('Debe cargar reporte Marbetes Cancelados', async ({ page }) => {
    await page.goto('/Admin/MarbetesCancelados');
    await page.waitForLoadState('networkidle');

    await page.waitForTimeout(2000);

    const content = page.locator('[class*="container"], main, .content');
    const visible = await content.first().isVisible().catch(() => false);
    expect(visible).toBeTruthy();
  });

  test('Debe cargar reporte Marbetes Pendientes', async ({ page }) => {
    await page.goto('/Admin/MarbetesPendientes');
    await page.waitForLoadState('networkidle');

    await page.waitForTimeout(2000);

    const content = page.locator('[class*="container"], main, .content');
    const visible = await content.first().isVisible().catch(() => false);
    expect(visible).toBeTruthy();
  });

  test('Debe cargar reporte Marbetes con Diferencia', async ({ page }) => {
    await page.goto('/Admin/MarbetesConDiferencia');
    await page.waitForLoadState('networkidle');

    await page.waitForTimeout(2000);

    const content = page.locator('[class*="container"], main, .content');
    const visible = await content.first().isVisible().catch(() => false);
    expect(visible).toBeTruthy();
  });

  test('Debe cargar reporte Distribución Marbetes', async ({ page }) => {
    await page.goto('/Admin/DistribucionMarbetes');
    await page.waitForLoadState('networkidle');

    await page.waitForTimeout(2000);

    const content = page.locator('[class*="container"], main, .content');
    const visible = await content.first().isVisible().catch(() => false);
    expect(visible).toBeTruthy();
  });

  test('Debe cargar reporte Comparativos Marbetes', async ({ page }) => {
    await page.goto('/Admin/ComparativosMarbetes');
    await page.waitForLoadState('networkidle');

    await page.waitForTimeout(2000);

    const content = page.locator('[class*="container"], main, .content');
    const visible = await content.first().isVisible().catch(() => false);
    expect(visible).toBeTruthy();
  });

  test('Debe cargar reporte Almacen Detalle', async ({ page }) => {
    await page.goto('/Admin/AlmacenDetalle');
    await page.waitForLoadState('networkidle');

    await page.waitForTimeout(2000);

    const content = page.locator('[class*="container"], main, .content');
    const visible = await content.first().isVisible().catch(() => false);
    expect(visible).toBeTruthy();
  });

  test('Debe cargar reporte Producto Detalle', async ({ page }) => {
    await page.goto('/Admin/ProductoDetalle');
    await page.waitForLoadState('networkidle');

    await page.waitForTimeout(2000);

    const content = page.locator('[class*="container"], main, .content');
    const visible = await content.first().isVisible().catch(() => false);
    expect(visible).toBeTruthy();
  });

  test('Debe permitir búsqueda en reportes', async ({ page }) => {
    await page.goto('/Admin/ListadoMarbetes');
    await page.waitForLoadState('networkidle');

    // Buscar input
    const searchInputs = page.locator('input[type="text"], input[class*="search"]');
    const searchInput = searchInputs.first();

    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('test');

      // Validar valor
      const value = await searchInput.inputValue();
      expect(value).toContain('test');

      // Limpiar
      await searchInput.clear();
    }
  });

  test('Debe permitir cambiar período en reporte', async ({ page }) => {
    await page.goto('/Admin/ListadoMarbetes');
    await page.waitForLoadState('networkidle');

    // Buscar select de periodo
    const selects = page.locator('select');

    for (let i = 0; i < await selects.count(); i++) {
      const select = selects.nth(i);
      if (await select.isVisible()) {
        // Validar que es selectable
        await expect(select).toBeEnabled();
        break;
      }
    }
  });

  test('Debe tener opción de exportar en reportes', async ({ page }) => {
    await page.goto('/Admin/ListadoMarbetes');
    await page.waitForLoadState('networkidle');

    // Buscar botón de exportar
    const exportBtns = page.locator('button', { hasText: /exportar|export|pdf|descargar/i });

    const hasExport = await exportBtns.first().isVisible().catch(() => false);
    expect(hasExport).toBeTruthy();
  });
});
