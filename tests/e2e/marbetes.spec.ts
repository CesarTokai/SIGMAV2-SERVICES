import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

test.describe('Gestión de Marbetes', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/');
    await page.fill('#login-email', TEST_EMAIL);
    await page.fill('#login-password', TEST_PASSWORD);
    await page.click('button.btn.primary');
    await page.waitForNavigation();

    // Navegar a marbetes
    await page.waitForSelector('.admin-sidebar', { timeout: 5000 });
    await page.goto('/Admin/MarbetesAdmin');
    await page.waitForLoadState('networkidle');
  });

  test('Debe cargar página de marbetes', async ({ page }) => {
    // Esperar título específico
    const title = page.locator('h1.title').first();
    await expect(title).toContainText('Marbetes', { ignoreCase: true, timeout: 5000 });
  });

  test('Debe mostrar botones de submódulos', async ({ page }) => {
    // Buscar botones de submódulos
    const buttons = page.locator('button[class*="submodule"]');

    const count = await buttons.count();
    expect(count).toBeGreaterThanOrEqual(3);
  });

  test('Debe navegar a submódulo Consulta y Captura', async ({ page }) => {
    // Click en botón "Consulta y Captura"
    const consultaBtn = page.locator('button', { hasText: /consulta|captura/i }).first();

    if (await consultaBtn.isVisible()) {
      await consultaBtn.click();

      // Esperar que cargue
      await page.waitForTimeout(1000);

      // Validar que el botón está activo
      const active = await consultaBtn.evaluate(el =>
        el.classList.contains('active')
      );
      expect(active).toBeTruthy();
    }
  });

  test('Debe navegar a submódulo Impresión', async ({ page }) => {
    // Click en botón "Impresión"
    const impresionBtn = page.locator('button', { hasText: /impresión|impresion/i }).first();

    if (await impresionBtn.isVisible()) {
      await impresionBtn.click();

      // Esperar que cargue
      await page.waitForTimeout(1000);

      // Validar que el botón está activo
      const active = await impresionBtn.evaluate(el =>
        el.classList.contains('active')
      );
      expect(active).toBeTruthy();
    }
  });

  test('Debe navegar a submódulo Conteo', async ({ page }) => {
    // Click en botón "Conteo"
    const conteoBtn = page.locator('button', { hasText: /conteo|contar/i }).first();

    if (await conteoBtn.isVisible()) {
      await conteoBtn.click();

      // Esperar que cargue
      await page.waitForTimeout(1000);

      // Validar que el botón está activo
      const active = await conteoBtn.evaluate(el =>
        el.classList.contains('active')
      );
      expect(active).toBeTruthy();
    }
  });

  test('Debe navegar a submódulo Reimpresión', async ({ page }) => {
    // Click en botón "Reimpresión" o "Gestión"
    const reimprBtn = page.locator('button', { hasText: /reimpres|gestion|gestión/i }).first();

    if (await reimprBtn.isVisible()) {
      await reimprBtn.click();

      // Esperar que cargue
      await page.waitForTimeout(1000);

      // Validar que el botón está activo
      const active = await reimprBtn.evaluate(el =>
        el.classList.contains('active')
      );
      expect(active).toBeTruthy();
    }
  });

  test('Debe navegar a submódulo Listado Completo', async ({ page }) => {
    // Click en botón "Listado Completo"
    const listadoBtn = page.locator('button', { hasText: /listado/i }).first();

    if (await listadoBtn.isVisible()) {
      await listadoBtn.click();

      // Esperar que cargue
      await page.waitForTimeout(1000);

      // Validar que el botón está activo
      const active = await listadoBtn.evaluate(el =>
        el.classList.contains('active')
      );
      expect(active).toBeTruthy();
    }
  });

  test('Debe cargar contenido al cambiar submódulos', async ({ page }) => {
    // Navegar entre submódulos y validar contenido
    const buttons = page.locator('button[class*="submodule"]');

    for (let i = 0; i < await buttons.count(); i++) {
      const button = buttons.nth(i);

      if (await button.isVisible()) {
        await button.click();

        // Esperar carga
        await page.waitForTimeout(800);

        // Validar que hay contenido (no solo botones)
        const content = page.locator('.submodule-content, [class*="content"]');
        const hasContent = await content.first().isVisible().catch(() => false);

        expect(hasContent).toBeTruthy();
      }
    }
  });

  test('Debe mantener estado de submódulo en URL', async ({ page }) => {
    // Click en submódulo
    const impresionBtn = page.locator('button', { hasText: /impresión|impresion/i }).first();

    if (await impresionBtn.isVisible()) {
      await impresionBtn.click();

      // Esperar que cargue
      await page.waitForTimeout(1000);

      // Validar URL contiene submodulo
      const url = page.url();
      expect(url).toContain('submodulo');
    }
  });
});
