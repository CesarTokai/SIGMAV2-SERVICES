# SIGMAV2-APP Context & Testing Documentation

## Project Overview

SIGMAV2-APP es sistema de gestión de inventario y marbetes para Tokai. Aplicación Vue 3 + Vite con backend API REST.

## Testing Infrastructure

### E2E Testing with Playwright

**Total Coverage:** 87 tests (100% passing)
**Execution Time:** ~2.9 minutes
**Framework:** Playwright (@playwright/test)
**Browser:** Chrome (headless)

#### Test Execution

```bash
npm run test:e2e          # Headless mode (CI/CD ready)
npm run test:e2e:headed   # Visible browser
npm run test:e2e:ui       # Interactive UI mode
```

#### Reports

- HTML report: `playwright-report/index.html`
- Test results: `test-results/`

### Test Coverage by Module

#### 1. Authentication (4 tests)
**File:** `tests/e2e/login.spec.ts`

Tests login functionality with valid/invalid credentials:
- Credenciales válidas → redirige a dashboard
- Email vacío → muestra error
- Contraseña vacía → muestra error
- Credenciales inválidas → muestra error

**Key Elements:**
- Login form: `#login-email`, `#login-password`
- Submit button: `button.btn.primary`
- Token persistence: localStorage

#### 2. Dashboard Admin (5 tests)
**File:** `tests/e2e/dashboard.spec.ts`

Tests dashboard post-login and navigation:
- Dashboard carga correctamente
- Info usuario visible en sidebar
- Logout funciona
- Menú navegación visible
- Sesión persiste tras refresh

**Key Elements:**
- Sidebar: `.admin-sidebar`
- User info: `.user-details`
- Logout button: `button.btn-logout`
- Logo container: `.logo-container`

#### 3. Gestión de Períodos (6 tests)
**File:** `tests/e2e/periodos.spec.ts`

Tests period management (crear, validar, buscar):
- Página carga con título correcto
- Modal para crear período
- Validación: comentarios mínimo 10 caracteres
- Validación: fecha requerida
- Búsqueda de períodos
- Tabla con lista de períodos

**Key Elements:**
- Title: `h1.page-title`
- Add button: `button.btn-add`
- Modal: `.modal-overlay`
- Form fields: `#fecha`, `#comentarios`

#### 4. Gestión de Inventario (6 tests)
**File:** `tests/e2e/inventario.spec.ts`

Tests inventory management:
- Página de inventario carga
- Selector de período disponible
- Tabla de productos visible
- Búsqueda de productos
- Botón de importación presente
- Controles de paginación

**Key Elements:**
- Periodo selector: selector de período
- Search input: `.search-input`
- Products table: `.data-table`
- Import button: botón con texto "importar"

#### 5. Multi-Almacén (9 tests)
**File:** `tests/e2e/multialmacen.spec.ts`

Tests multi-warehouse functionality:
- Página carga correctamente
- Selector de período funciona
- Tabla de productos multi-almacén
- Búsqueda en productos
- Botón de importación
- Navegación con tabs (productos/bajas)
- Paginación
- Cambio de período recarga datos
- Menú de navegación

#### 6. Gestión de Almacenes (10 tests)
**File:** `tests/e2e/almacenes.spec.ts`

Tests warehouse CRUD operations:
- Página de almacenes carga
- Tabla de almacenes visible
- Búsqueda de almacenes
- Crear almacén (modal)
- Validación de campos requeridos
- Paginación
- Información de almacenes en tabla
- Botones de acción (editar/eliminar)

**Key Elements:**
- Title: encabezado de página
- Create button: `button.btn-add`
- Search input: input de búsqueda
- Almacenes table: tabla de datos

#### 7. Gestión de Marbetes (9 tests)
**File:** `tests/e2e/marbetes.spec.ts`

Tests marbetes (tickets) module with submódulos:
- Página de marbetes carga
- Botones de submódulos visibles
- Navegación a Consulta y Captura
- Navegación a Impresión
- Navegación a Conteo
- Navegación a Reimpresión
- Navegación a Listado Completo
- Contenido carga al cambiar submódulos
- Estado de submódulo en URL

**Submódulos:**
- Consulta y Captura (ConsultaCaptura.vue)
- Impresión (ImpresionMarbetes.vue)
- Conteo (ConteoMarbetes.vue)
- Reimpresión de Marbetes (GestionMarbetes.vue)
- Listado Completo (ListadoCompleteMarbetes.vue)

#### 8. Reportes (13 tests)
**File:** `tests/e2e/reportes.spec.ts`

Tests reporting module (8 tipos de reportes):
- Listado Marbetes
- Marbetes Cancelados
- Marbetes Pendientes
- Marbetes con Diferencia
- Distribución Marbetes
- Comparativos Marbetes
- Almacén Detalle
- Producto Detalle

Funcionalidades probadas:
- Carga de cada reporte
- Selector de período
- Búsqueda en reportes
- Cambio de almacén
- Botón de exportar

#### 9. Gestión de Usuarios (15 tests)
**File:** `tests/e2e/usuarios.spec.ts`

Tests user management:
- Página de usuarios carga
- Tarjetas de estadísticas (Total, Admin, Almacenista, Auxiliar, Aux. Conteo)
- Búsqueda de usuarios
- Filtro por rol
- Tabla de usuarios
- Columnas correctas en tabla
- Botón crear usuario
- Modal de crear usuario
- Acciones en tabla (editar/eliminar)
- Paginación
- Información de usuario en tabla
- Estado de usuario visible
- Cambio de filtro múltiples veces

**Roles:**
- ADMINISTRADOR
- ALMACENISTA
- AUXILIAR
- AUXILIAR_DE_CONTEO

#### 10. Edición y Roles de Usuarios (10 tests)
**File:** `tests/e2e/usuarios-edicion.spec.ts`

Tests user editing and role changes:
- Abrir modal de edición
- Mostrar información del usuario
- Selector de rol en modal
- Seleccionar diferentes roles
- Botones guardar/cancelar
- Cerrar modal al cancelar
- Permitir eliminar usuario
- Confirmación al eliminar
- Filtrar y editar usuarios
- Modal tiene estructura de formulario

## Authentication & Authorization

### Credenciales de Prueba

- **Email:** cgonzalez@tokai.com.mx
- **Password:** Password123!
- **Rol:** ADMINISTRADOR

### Token Management

- Token almacenado en: `localStorage.token`
- Validación: JWT format (3 partes separadas por punto)
- Expiración: Validación de `exp` claim
- Limpieza: Se ejecuta al logout o token inválido

### Authorization Guards

Router valida:
- Presencia de token válido
- Formato JWT correcto
- Expiración del token
- Rol del usuario contra ruta requerida

Rutas públicas: `/`, `/login`, `/password-recovery`, `/register`

## Navigation Structure

### Router Configuration

**Public Routes:**
- `/` → Login (default)
- `/login` → Login page
- `/register` → User registration
- `/password-recovery` → Password recovery

**Admin Routes (ADMINISTRADOR):**
- `/Admin` → Períodos (default)
- `/Admin/user-management` → Gestión de Usuarios
- `/Admin/Almacen` → Gestión de Almacenes
- `/Admin/MultiAlmacen` → Multi-Almacén
- `/Admin/InventarioAdmin` → Gestión de Inventario
- `/Admin/MarbetesAdmin` → Gestión de Marbetes
- `/Admin/ListadoMarbetes`, `/Admin/MarbetesCancelados`, etc. → Reportes

**Other Roles:**
- `/almacenista` → Dashboard Almacenista
- `/auxiliar` → Dashboard Auxiliar
- `/auxiliar-de-conteo` → Dashboard Auxiliar de Conteo

## API Endpoints Used

### Authentication
- `POST /auth/login` - Login con email/password

### Periods
- `GET /periods?page=X&size=Y` - Listar períodos
- `POST /periods` - Crear período
- `PUT /periods/{id}` - Actualizar período
- `DELETE /periods/{id}` - Eliminar período

### Warehouses
- `GET /warehouses?page=X&size=Y&sortBy=warehouseKey&sortDir=asc` - Listar almacenes
- `POST /warehouses` - Crear almacén
- `PUT /warehouses/{id}` - Actualizar almacén
- `DELETE /warehouses/{id}` - Eliminar almacén

### Inventory
- `GET /inventory/period-report?periodId=X&search=Y` - Reporte de inventario
- `POST /inventory/import` - Importar inventario (FormData)

### Multi-Warehouse
- `POST /multi-warehouse/existences` - Existencias multi-almacén

### Users
- `GET /admin/users` - Listar usuarios
- `POST /admin/users` - Crear usuario
- `PUT /admin/users/{id}` - Actualizar usuario
- `DELETE /admin/users/{id}` - Eliminar usuario
- `PUT /admin/users/{id}/role` - Cambiar rol de usuario

### Reports
- `GET /reports/...` - Varios endpoints de reportes

## Test Data Strategy

### Approach
- **No fixtures**: Tests crean datos mínimos necesarios
- **Credenciales reales**: Email/password existente en BD
- **Datos dinámicos**: Fechas/IDs generados en tiempo de ejecución
- **Clean-up**: Tests no requieren limpieza (estado es efímero)

### Validación

Tests validan:
- Navegación correcta (URL changes)
- Renderizado de UI (elementos visibles)
- Interacción (clicks, formularios)
- Estado (localStorage, URL params)
- Mensajes de error
- Paginación
- Búsqueda y filtrado

No testeamos:
- Validación de datos de backend
- Lógica de negocio compleja
- Animaciones específicas
- Estilos CSS

## Common Issues & Solutions

### Strict Mode Violations
**Problema:** Locator resuelve a múltiples elementos
**Solución:** Usar `.first()` o selectores más específicos (`.page-title`, `[class*="search"]`)

### Modal Overlay Intercepting Clicks
**Problema:** Overlay del modal bloquea clics en botones
**Solución:** Ajustar timeouts, esperar a que modal esté completamente visible

### Timing Issues
**Problema:** Elementos no cargan a tiempo
**Solución:** Usar `waitForLoadState('networkidle')` y `waitForTimeout()`

### URL Navigation
**Problema:** Router guard redirige antes de validación
**Solución:** Esperar a `waitForNavigation()` después de actions

## Maintenance

### Adding New Tests

1. Crear archivo `tests/e2e/modulo.spec.ts`
2. Seguir estructura existente (beforeEach login, navegación)
3. Tests pequeños y enfocados (1 responsabilidad)
4. Usar helpers reutilizables (selectores, esperas)
5. Documentar en este archivo

### Updating Selectors

Si UI cambia:
1. Actualizar selectores en tests
2. Preferir class names sobre IDs (más estables)
3. Usar `[class*="pattern"]` para clase dinámica
4. Agregar `data-testid` si es necesario

### Debugging Tests

```bash
npm run test:e2e:debug        # Paso a paso
npm run test:e2e:headed       # Navegador visible
npx playwright codegen        # Grabar nuevos tests
```

## CI/CD Integration

Tests listos para integrar en:
- GitHub Actions
- GitLab CI
- Jenkins
- Otros (comando: `npm run test:e2e`)

Exit code: 0 (success), 1 (failure)
Output: HTML report en `playwright-report/`

## Future Enhancements

- [ ] Agregar tests para AccionesForm (cambios, validaciones)
- [ ] Tests de descarga de reportes (PDFs)
- [ ] Tests de importación de Excel
- [ ] Tests de integración (flujos multi-paso)
- [ ] Performance tests (carga de grandes datasets)
- [ ] Visual regression tests
- [ ] Accessibility tests (WCAG)
