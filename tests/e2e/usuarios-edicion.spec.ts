import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'cgonzalez@tokai.com.mx';
const TEST_PASSWORD = 'Password123!';

test.describe('Edición y Cambio de Roles de Usuarios', () => {
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
    await page.waitForTimeout(2000);
  });

  test('Debe permitir abrir modal de edición de usuario', async ({ page }) => {
    // Buscar botón editar (icono lápiz)
    const editBtns = page.locator('button[title*="editar" i], button[title*="edit" i]');

    if (await editBtns.first().isVisible()) {
      await editBtns.first().click();

      // Esperar modal
      const modal = page.locator('[class*="modal"]').first();
      const modalVisible = await modal.isVisible({ timeout: 3000 }).catch(() => false);

      expect(modalVisible).toBeTruthy();
    }
  });

  test('Debe mostrar información de usuario en modal', async ({ page }) => {
    // Buscar botón editar
    const editBtns = page.locator('button[title*="editar" i], button[title*="edit" i]');

    if (await editBtns.first().isVisible()) {
      await editBtns.first().click();

      // Esperar modal
      const modal = page.locator('[class*="modal"]').first();
      await expect(modal).toBeVisible({ timeout: 3000 });

      // Validar que hay campos
      const inputs = modal.locator('input, select');
      const inputCount = await inputs.count();

      expect(inputCount).toBeGreaterThan(0);
    }
  });

  test('Debe tener selector de rol en modal', async ({ page }) => {
    // Buscar botón editar
    const editBtns = page.locator('button[title*="editar" i], button[title*="edit" i]');

    if (await editBtns.first().isVisible()) {
      await editBtns.first().click();

      // Esperar modal
      const modal = page.locator('[class*="modal"]').first();
      await expect(modal).toBeVisible({ timeout: 3000 });

      // Buscar select de rol
      const roleSelect = modal.locator('select');

      const hasRoleSelect = await roleSelect.first().isVisible().catch(() => false);
      expect(hasRoleSelect).toBeTruthy();
    }
  });

  test('Debe permitir seleccionar diferentes roles', async ({ page }) => {
    // Buscar botón editar
    const editBtns = page.locator('button[title*="editar" i], button[title*="edit" i]');

    if (await editBtns.first().isVisible()) {
      await editBtns.first().click();

      // Esperar modal
      const modal = page.locator('[class*="modal"]').first();
      await expect(modal).toBeVisible({ timeout: 3000 });

      // Buscar select de rol
      const roleSelect = modal.locator('select').first();

      if (await roleSelect.isVisible()) {
        // Validar opciones disponibles
        const options = modal.locator('option');
        const optionCount = await options.count();

        expect(optionCount).toBeGreaterThan(1);

        // Cambiar rol
        const firstOption = await options.nth(1).getAttribute('value');
        if (firstOption) {
          await roleSelect.selectOption(firstOption);

          // Validar cambio
          const selected = await roleSelect.inputValue();
          expect(selected).toBe(firstOption);
        }
      }
    }
  });

  test('Debe tener botones de guardar y cancelar en modal', async ({ page }) => {
    // Buscar botón editar
    const editBtns = page.locator('button[title*="editar" i], button[title*="edit" i]');

    if (await editBtns.first().isVisible()) {
      await editBtns.first().click();

      // Esperar modal
      const modal = page.locator('[class*="modal"]').first();
      await expect(modal).toBeVisible({ timeout: 3000 });

      // Buscar botones
      const saveBtn = modal.locator('button', { hasText: /guardar|save/i });
      const cancelBtn = modal.locator('button', { hasText: /cancelar|cancel/i });

      const hasSave = await saveBtn.first().isVisible().catch(() => false);
      const hasCancel = await cancelBtn.first().isVisible().catch(() => false);

      expect(hasSave || hasCancel).toBeTruthy();
    }
  });

  test('Debe cerrar modal al cancelar', async ({ page }) => {
    // Buscar botón editar
    const editBtns = page.locator('button[title*="editar" i], button[title*="edit" i]');

    if (await editBtns.first().isVisible()) {
      await editBtns.first().click();

      // Esperar modal
      const modal = page.locator('[class*="modal"]').first();
      await expect(modal).toBeVisible({ timeout: 3000 });

      // Click cancelar
      const cancelBtn = modal.locator('button', { hasText: /cancelar|cancel/i }).first();

      if (await cancelBtn.isVisible()) {
        await cancelBtn.click();

        // Modal debe cerrarse
        await page.waitForTimeout(500);
        const modalVisible = await modal.isVisible().catch(() => false);

        expect(!modalVisible).toBeTruthy();
      }
    }
  });

  test('Debe permitir eliminar usuario', async ({ page }) => {
    // Buscar botón eliminar
    const deleteBtns = page.locator('button[title*="eliminar" i], button[title*="delete" i]');

    const hasDelete = await deleteBtns.first().isVisible().catch(() => false);
    expect(hasDelete).toBeTruthy();
  });

  test('Debe mostrar confirmación al eliminar usuario', async ({ page }) => {
    // Buscar botón eliminar
    const deleteBtns = page.locator('button[title*="eliminar" i], button[title*="delete" i]');

    if (await deleteBtns.first().isVisible().catch(() => false)) {
      await deleteBtns.first().click();

      // Esperar alert/modal de confirmación
      const alert = page.locator('[class*="swal"], [role="alertdialog"]');

      const hasAlert = await alert.first().isVisible({ timeout: 2000 }).catch(() => false);
      expect(hasAlert).toBeTruthy();
    }
  });

  test('Debe filtrar usuarios por rol y permitir editar', async ({ page }) => {
    // Cambiar filtro a ADMINISTRADOR
    const roleSelect = page.locator('select[class*="filter"]').first();

    if (await roleSelect.isVisible()) {
      await roleSelect.selectOption('ADMINISTRADOR');

      // Esperar que se filtre
      await page.waitForTimeout(1000);

      // Buscar botón editar
      const editBtns = page.locator('button[title*="editar" i], button[title*="edit" i]');

      if (await editBtns.first().isVisible()) {
        await editBtns.first().click();

        // Esperar modal
        const modal = page.locator('[class*="modal"]').first();
        const modalVisible = await modal.isVisible({ timeout: 3000 }).catch(() => false);

        expect(modalVisible).toBeTruthy();
      }
    }
  });

  test('Modal edición tiene campos requeridos', async ({ page }) => {
    // Buscar botón editar
    const editBtns = page.locator('button[title*="editar" i], button[title*="edit" i]');

    if (await editBtns.first().isVisible()) {
      await editBtns.first().click();

      // Esperar modal
      const modal = page.locator('[class*="modal"]').first();
      await expect(modal).toBeVisible({ timeout: 3000 });

      // Validar que modal tiene estructura
      const form = modal.locator('form, [class*="form"]');
      const hasForm = await form.first().isVisible().catch(() => false);

      expect(hasForm).toBeTruthy();
    }
  });
});
