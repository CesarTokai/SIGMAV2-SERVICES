import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

test.describe('Gestión de Usuarios', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/');
    await page.fill('#login-email', TEST_EMAIL);
    await page.fill('#login-password', TEST_PASSWORD);
    await page.click('button.btn.primary');
    await page.waitForNavigation();

    // Navegar a usuarios
    await page.waitForSelector('.admin-sidebar', { timeout: 5000 });
    await page.goto('/Admin/user-management');
    await page.waitForLoadState('networkidle');
  });

  test('Debe cargar página de gestión de usuarios', async ({ page }) => {
    // Esperar título
    const title = page.locator('h1.page-title');
    await expect(title).toContainText('Usuarios', { ignoreCase: true, timeout: 5000 });
  });

  test('Debe mostrar tarjetas de estadísticas', async ({ page }) => {
    // Buscar tarjetas de estadísticas
    const statCards = page.locator('[class*="stat-card"], [class*="stat"]');

    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(4);
  });

  test('Debe mostrar búsqueda de usuarios', async ({ page }) => {
    // Buscar input de búsqueda
    const searchInput = page.locator('input[placeholder*="buscar" i]');

    const visible = await searchInput.isVisible();
    expect(visible).toBeTruthy();
  });

  test('Debe permitir buscar usuarios', async ({ page }) => {
    const searchInput = page.locator('input[placeholder*="buscar" i]');

    if (await searchInput.isVisible()) {
      await searchInput.fill('test');

      // Validar que se escribió
      const value = await searchInput.inputValue();
      expect(value).toContain('test');

      // Limpiar
      await searchInput.clear();
    }
  });

  test('Debe mostrar filtro por rol', async ({ page }) => {
    // Buscar select de rol
    const roleSelect = page.locator('select[class*="filter"]').first();

    const visible = await roleSelect.isVisible();
    expect(visible).toBeTruthy();
  });

  test('Debe permitir filtrar por rol', async ({ page }) => {
    const roleSelect = page.locator('select[class*="filter"]').first();

    if (await roleSelect.isVisible()) {
      // Validar que tiene opciones
      const options = page.locator('select option');
      const count = await options.count();
      expect(count).toBeGreaterThan(1);

      // Cambiar a ADMINISTRADOR
      await roleSelect.selectOption('ADMINISTRADOR');

      // Validar cambio
      const value = await roleSelect.inputValue();
      expect(value).toContain('ADMINISTRADOR');
    }
  });

  test('Debe mostrar tabla de usuarios', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(1500);

    const table = page.locator('table, [role="table"]');
    const tableVisible = await table.first().isVisible().catch(() => false);

    const empty = await page.locator('.no-data, [class*="empty"]').isVisible().catch(() => false);

    expect(tableVisible || empty).toBeTruthy();
  });

  test('Debe mostrar columnas de usuario', async ({ page }) => {
    // Buscar encabezados de tabla
    const headers = page.locator('th');

    const count = await headers.count();
    expect(count).toBeGreaterThanOrEqual(5);

    // Validar que existen columnas clave
    const headerTexts = await headers.allTextContents();
    const hasEmail = headerTexts.some(text => text.toLowerCase().includes('email'));
    const hasRole = headerTexts.some(text => text.toLowerCase().includes('rol'));

    expect(hasEmail || hasRole).toBeTruthy();
  });

  test('Debe tener botón para crear nuevo usuario', async ({ page }) => {
    // Buscar botón nuevo usuario
    const newBtn = page.locator('button', { hasText: /nuevo|crear|agregar/i }).first();

    const visible = await newBtn.isVisible();
    expect(visible).toBeTruthy();
  });

  test('Debe mostrar modal al crear usuario', async ({ page }) => {
    // Click botón nuevo
    const newBtn = page.locator('button', { hasText: /nuevo usuario|crear/i }).first();

    if (await newBtn.isVisible()) {
      await newBtn.click();

      // Esperar modal
      const modal = page.locator('[class*="modal"], dialog').first();
      const modalVisible = await modal.isVisible({ timeout: 3000 }).catch(() => false);

      expect(modalVisible).toBeTruthy();
    }
  });

  test('Debe mostrar acciones en tabla de usuarios', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(2000);

    // Buscar botones de acción
    const actionBtns = page.locator('button[title*="editar" i], button[title*="eliminar" i], [class*="btn-action"]');

    const hasActions = await actionBtns.first().isVisible().catch(() => false);
    expect(hasActions).toBeTruthy();
  });

  test('Debe tener paginación', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(1500);

    // Buscar paginación
    const pagination = page.locator('[class*="pagination"], [class*="page"]');
    const pageSize = page.locator('select');

    const hasPagination = await pagination.first().isVisible().catch(() => false);
    const hasPageSize = await pageSize.nth(1).isVisible().catch(() => false);

    expect(hasPagination || hasPageSize).toBeTruthy();
  });

  test('Debe mostrar información de usuario en tabla', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(2000);

    // Buscar filas
    const rows = page.locator('tbody tr, [role="row"]');
    const count = await rows.count();

    // Si hay usuarios, validar estructura
    if (count > 0 && count < 100) {
      const firstRow = rows.first();
      await expect(firstRow).toBeVisible();

      // Validar que tiene contenido
      const cells = firstRow.locator('td');
      const cellCount = await cells.count();
      expect(cellCount).toBeGreaterThan(0);
    }
  });

  test('Debe mostrar estado de usuarios', async ({ page }) => {
    // Esperar tabla
    await page.waitForTimeout(2000);

    // Buscar badges de estado
    const statusBadges = page.locator('[class*="status"], [class*="badge"]');

    const hasStatus = await statusBadges.first().isVisible().catch(() => false);
    expect(hasStatus).toBeTruthy();
  });

  test('Debe permitir cambiar filtro múltiples veces', async ({ page }) => {
    const roleSelect = page.locator('select[class*="filter"]').first();

    if (await roleSelect.isVisible()) {
      // Cambiar a ALMACENISTA
      await roleSelect.selectOption('ALMACENISTA');
      await page.waitForTimeout(800);

      let value = await roleSelect.inputValue();
      expect(value).toContain('ALMACENISTA');

      // Cambiar a AUXILIAR
      await roleSelect.selectOption('AUXILIAR');
      await page.waitForTimeout(800);

      value = await roleSelect.inputValue();
      expect(value).toContain('AUXILIAR');

      // Volver a todos
      await roleSelect.selectOption('');
    }
  });
});
