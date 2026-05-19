import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

// Umbrales de rendimiento (en ms)
const THRESHOLDS = {
  pageLoad: 3000,        // Carga página
  modalOpen: 800,        // Abrir modal
  search: 1500,          // Búsqueda
  filter: 1000,          // Filtrar
  tableRender: 2000,     // Tabla render
  navigation: 1500,      // Navegación
  login: 2000,           // Login
};

test.describe('Performance & Carga', () => {
  test.beforeEach(async ({ page }) => {
    // Login una sola vez
    await page.goto('/');
    await page.fill('#login-email', TEST_EMAIL);
    await page.fill('#login-password', TEST_PASSWORD);

    const startLogin = Date.now();
    await page.click('button.btn.primary');
    await page.waitForNavigation();
    const loginTime = Date.now() - startLogin;

    console.log(`⏱️  Login time: ${loginTime}ms`);
    expect(loginTime).toBeLessThan(THRESHOLDS.login);
  });

  test('Dashboard debe cargar en menos de 3s', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/Admin');
    await page.waitForLoadState('networkidle');

    const loadTime = Date.now() - startTime;

    console.log(`⏱️  Dashboard load: ${loadTime}ms`);
    expect(loadTime).toBeLessThan(THRESHOLDS.pageLoad);
  });

  test('Períodos debe cargar en menos de 3s', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/Admin/PeriodosAdmin');
    await page.waitForLoadState('networkidle');

    const loadTime = Date.now() - startTime;

    console.log(`⏱️  Períodos load: ${loadTime}ms`);
    expect(loadTime).toBeLessThan(THRESHOLDS.pageLoad);
  });

  test('Inventario debe cargar en menos de 3s', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/Admin/InventarioAdmin');
    await page.waitForLoadState('networkidle');

    const loadTime = Date.now() - startTime;

    console.log(`⏱️  Inventario load: ${loadTime}ms`);
    expect(loadTime).toBeLessThan(THRESHOLDS.pageLoad);
  });

  test('Almacenes debe cargar en menos de 3s', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/Admin/Almacen');
    await page.waitForLoadState('networkidle');

    const loadTime = Date.now() - startTime;

    console.log(`⏱️  Almacenes load: ${loadTime}ms`);
    expect(loadTime).toBeLessThan(THRESHOLDS.pageLoad);
  });

  test('MultiAlmacén debe cargar en menos de 3s', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/Admin/MultiAlmacen');
    await page.waitForLoadState('networkidle');

    const loadTime = Date.now() - startTime;

    console.log(`⏱️  MultiAlmacén load: ${loadTime}ms`);
    expect(loadTime).toBeLessThan(THRESHOLDS.pageLoad);
  });

  test('Usuarios debe cargar en menos de 3s', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/Admin/user-management');
    await page.waitForLoadState('networkidle');

    const loadTime = Date.now() - startTime;

    console.log(`⏱️  Usuarios load: ${loadTime}ms`);
    expect(loadTime).toBeLessThan(THRESHOLDS.pageLoad);
  });

  test('Reportes debe cargar en menos de 3s', async ({ page }) => {
    const startTime = Date.now();

    await page.goto('/Admin/ListadoMarbetes');
    await page.waitForLoadState('networkidle');

    const loadTime = Date.now() - startTime;

    console.log(`⏱️  Reportes load: ${loadTime}ms`);
    expect(loadTime).toBeLessThan(THRESHOLDS.pageLoad);
  });

  test('Modal debe abrir en menos de 800ms', async ({ page }) => {
    await page.goto('/Admin/user-management');
    await page.waitForLoadState('networkidle');

    const newBtn = page.locator('button', { hasText: /nuevo|crear/i }).first();

    const startTime = Date.now();
    await newBtn.click();

    const modal = page.locator('[class*="modal"]').first();
    await expect(modal).toBeVisible({ timeout: THRESHOLDS.modalOpen });

    const openTime = Date.now() - startTime;

    console.log(`⏱️  Modal open: ${openTime}ms`);
    expect(openTime).toBeLessThan(THRESHOLDS.modalOpen);
  });

  test('Búsqueda debe responder en menos de 1.5s', async ({ page }) => {
    await page.goto('/Admin/user-management');
    await page.waitForLoadState('networkidle');

    const searchInput = page.locator('input[placeholder*="buscar" i]');

    const startTime = Date.now();
    await searchInput.fill('test');

    // Esperar que se aplique filtro
    await page.waitForTimeout(500);

    const searchTime = Date.now() - startTime;

    console.log(`⏱️  Search response: ${searchTime}ms`);
    expect(searchTime).toBeLessThan(THRESHOLDS.search);
  });

  test('Filtro debe responder en menos de 1s', async ({ page }) => {
    await page.goto('/Admin/user-management');
    await page.waitForLoadState('networkidle');

    const roleSelect = page.locator('select[class*="filter"]').first();

    const startTime = Date.now();
    await roleSelect.selectOption('ADMINISTRADOR');

    await page.waitForTimeout(300);

    const filterTime = Date.now() - startTime;

    console.log(`⏱️  Filter response: ${filterTime}ms`);
    expect(filterTime).toBeLessThan(THRESHOLDS.filter);
  });

  test('Navegación entre tabs debe ser rápida (< 1.5s)', async ({ page }) => {
    await page.goto('/Admin/MarbetesAdmin');
    await page.waitForLoadState('networkidle');

    const buttons = page.locator('button[class*="submodule"]');

    for (let i = 0; i < Math.min(3, await buttons.count()); i++) {
      const button = buttons.nth(i);

      const startTime = Date.now();
      await button.click();
      await page.waitForTimeout(400);

      const navTime = Date.now() - startTime;

      console.log(`⏱️  Navigation ${i + 1}: ${navTime}ms`);
      expect(navTime).toBeLessThan(THRESHOLDS.navigation);
    }
  });

  test('Tabla con datos debe renderizar en menos de 2s', async ({ page }) => {
    await page.goto('/Admin/ListadoMarbetes');

    const startTime = Date.now();
    await page.waitForLoadState('networkidle');

    const table = page.locator('table, [role="table"]').first();
    await expect(table).toBeVisible({ timeout: THRESHOLDS.tableRender });

    const renderTime = Date.now() - startTime;

    console.log(`⏱️  Table render: ${renderTime}ms`);
    expect(renderTime).toBeLessThan(THRESHOLDS.tableRender);
  });

  test('Renderizado consistente en navegación rápida', async ({ page }) => {
    // Navegar múltiples veces y validar que todo renderiza
    const pages = [
      '/Admin',
      '/Admin/user-management',
      '/Admin/InventarioAdmin',
      '/Admin/ListadoMarbetes'
    ];

    for (const url of pages) {
      await page.goto(url);
      await page.waitForLoadState('networkidle');

      // Validar que página cargó contenido
      const content = page.locator('[class*="container"], main, .content');
      await expect(content.first()).toBeVisible({ timeout: 1000 });

      console.log(`✅ ${url} rendered successfully`);
    }
  });

  test('Navegación rápida (5 páginas en menos de 15s)', async ({ page }) => {
    const pages = [
      '/Admin',
      '/Admin/user-management',
      '/Admin/InventarioAdmin',
      '/Admin/Almacen',
      '/Admin/ListadoMarbetes'
    ];

    const startTime = Date.now();

    for (const url of pages) {
      await page.goto(url);
      await page.waitForLoadState('networkidle');
    }

    const totalTime = Date.now() - startTime;
    const avgTime = totalTime / pages.length;

    console.log(`⏱️  Total time for 5 pages: ${totalTime}ms`);
    console.log(`⏱️  Average per page: ${avgTime.toFixed(0)}ms`);

    expect(totalTime).toBeLessThan(15000);
  });

  test('Paginación debe ser rápida', async ({ page }) => {
    await page.goto('/Admin/user-management');
    await page.waitForLoadState('networkidle');

    // Buscar botones de paginación
    const pageButtons = page.locator('button').filter({ hasText: /\d+|next|siguiente/i });

    if (await pageButtons.count() > 0) {
      const startTime = Date.now();

      // Click primera página siguiente
      const nextBtn = pageButtons.first();
      if (await nextBtn.isVisible()) {
        await nextBtn.click();
        await page.waitForTimeout(300);
      }

      const paginationTime = Date.now() - startTime;

      console.log(`⏱️  Pagination: ${paginationTime}ms`);
      expect(paginationTime).toBeLessThan(THRESHOLDS.filter);
    }
  });

  test('Core Web Vitals aproximados', async ({ page }) => {
    // LCP (Largest Contentful Paint) - Debería estar < 2.5s
    // FID (First Input Delay) - Debería estar < 100ms
    // CLS (Cumulative Layout Shift) - Debería estar < 0.1

    const startTime = Date.now();

    await page.goto('/Admin');
    await page.waitForLoadState('networkidle');

    const navigationTime = Date.now() - startTime;

    console.log(`⏱️  Navigation timing: ${navigationTime}ms`);

    // LCP aproximado (cuando networkidle)
    expect(navigationTime).toBeLessThan(2500);

    // Validar que página es responsiva (click responde rápido)
    const element = page.locator('button').first();

    const clickStart = Date.now();
    await element.click().catch(() => {});
    const clickTime = Date.now() - clickStart;

    console.log(`⏱️  Interactivity: ${clickTime}ms`);
    expect(clickTime).toBeLessThan(100);
  });
});
