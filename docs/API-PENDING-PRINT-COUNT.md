# API: Contar Marbetes Pendientes de Impresión

**Endpoint:** `POST /api/sigmav2/labels/pending-print-count`

**Fecha de creación:** 2025-12-16

---

## 📋 Descripción

Esta API cuenta cuántos marbetes están **pendientes de impresión** (estado GENERADO) para un periodo y almacén específicos.

### ✨ Propósito

Permite al frontend:
- ✅ Verificar si hay marbetes pendientes antes de mostrar el botón "Imprimir"
- ✅ Mostrar al usuario cuántos marbetes se van a imprimir
- ✅ Evitar llamadas innecesarias a la API de impresión cuando no hay nada que imprimir
- ✅ Mejorar la UX mostrando información útil al usuario

---

## 🔐 Autenticación

**Requerida:** Sí (JWT Bearer Token)

**Roles permitidos:**
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

---

## 📤 Request

### Endpoint
```
POST /api/sigmav2/labels/pending-print-count
```

### Headers
```
Content-Type: application/json
Authorization: Bearer {token}
```

### Body (JSON)

#### Caso 1: Contar Todos los Pendientes
```json
{
  "periodId": 16,
  "warehouseId": 369
}
```

#### Caso 2: Contar Pendientes de un Producto Específico
```json
{
  "periodId": 16,
  "warehouseId": 369,
  "productId": 123
}
```

### Campos

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `periodId` | Long | ✅ Sí | ID del periodo de inventario |
| `warehouseId` | Long | ✅ Sí | ID del almacén |
| `productId` | Long | ❌ No | ID del producto (filtro opcional) |

---

## 📥 Response

### Response Exitoso (200 OK)

```json
{
  "count": 25,
  "periodId": 16,
  "warehouseId": 369,
  "warehouseName": "Almacén Principal",
  "periodName": "2025-12-16"
}
```

### Campos de Respuesta

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `count` | Long | Cantidad de marbetes pendientes de impresión |
| `periodId` | Long | ID del periodo consultado |
| `warehouseId` | Long | ID del almacén consultado |
| `warehouseName` | String | Nombre del almacén |
| `periodName` | String | Fecha del periodo (formato YYYY-MM-DD) |

---

## 🎯 Casos de Uso

### Caso 1: Verificar antes de Imprimir

**Flujo en Frontend:**

```javascript
// 1. Consultar si hay marbetes pendientes
const response = await fetch('/api/sigmav2/labels/pending-print-count', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    periodId: 16,
    warehouseId: 369
  })
});

const data = await response.json();

// 2. Mostrar botón solo si hay pendientes
if (data.count > 0) {
  showButton(`📄 Imprimir ${data.count} Marbetes Pendientes`);
  enablePrintButton();
} else {
  showMessage('✓ Todos los marbetes ya están impresos');
  disablePrintButton();
}
```

### Caso 2: Mostrar Progreso

```javascript
// Dashboard de inventario
const { count, warehouseName } = await getPendingCount(periodId, warehouseId);

displayCard({
  title: warehouseName,
  message: `${count} marbetes pendientes de impresión`,
  status: count === 0 ? 'complete' : 'pending'
});
```

### Caso 3: Validación antes de Cerrar Periodo

```javascript
// Antes de cerrar un periodo, verificar que todo esté impreso
const { count } = await getPendingCount(periodId, warehouseId);

if (count > 0) {
  alert(`No puede cerrar el periodo. Aún hay ${count} marbetes sin imprimir.`);
  return false;
}

// Continuar con cierre de periodo
```

---

## 🚨 Respuestas de Error

### Error 401: No Autenticado

```json
{
  "success": false,
  "message": "Token inválido o expirado",
  "error": "UNAUTHORIZED",
  "timestamp": "2025-12-16T12:00:00.000000"
}
```

### Error 403: Sin Acceso al Almacén

```json
{
  "success": false,
  "message": "No tiene acceso al almacén especificado",
  "error": "PERMISSION_DENIED",
  "timestamp": "2025-12-16T12:00:00.000000"
}
```

### Error 400: Validación de Campos

```json
{
  "success": false,
  "message": "Errores de validación",
  "errors": [
    {
      "field": "periodId",
      "message": "El periodo es obligatorio"
    }
  ],
  "error": "VALIDATION_ERROR",
  "timestamp": "2025-12-16T12:00:00.000000"
}
```

---

## 💻 Ejemplos de Implementación

### React/TypeScript

```typescript
interface PendingCountResponse {
  count: number;
  periodId: number;
  warehouseId: number;
  warehouseName: string;
  periodName: string;
}

async function getPendingPrintCount(
  periodId: number,
  warehouseId: number
): Promise<PendingCountResponse> {
  const response = await fetch('/api/sigmav2/labels/pending-print-count', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${getToken()}`
    },
    body: JSON.stringify({
      periodId,
      warehouseId
    })
  });

  if (!response.ok) {
    throw new Error('Error al consultar marbetes pendientes');
  }

  return await response.json();
}

// Uso en componente
function PrintLabelsButton() {
  const [pendingCount, setPendingCount] = useState<number>(0);

  useEffect(() => {
    async function loadCount() {
      const data = await getPendingPrintCount(periodId, warehouseId);
      setPendingCount(data.count);
    }
    loadCount();
  }, [periodId, warehouseId]);

  if (pendingCount === 0) {
    return <div>✓ Todos los marbetes están impresos</div>;
  }

  return (
    <button onClick={handlePrint}>
      📄 Imprimir {pendingCount} Marbetes
    </button>
  );
}
```

### Vue

```javascript
export default {
  data() {
    return {
      pendingCount: 0,
      warehouseName: '',
      loading: false
    }
  },
  methods: {
    async loadPendingCount() {
      this.loading = true;
      try {
        const response = await this.$http.post(
          '/api/sigmav2/labels/pending-print-count',
          {
            periodId: this.selectedPeriod,
            warehouseId: this.selectedWarehouse
          }
        );

        this.pendingCount = response.data.count;
        this.warehouseName = response.data.warehouseName;
      } catch (error) {
        this.$toast.error('Error al cargar marbetes pendientes');
      } finally {
        this.loading = false;
      }
    }
  },
  computed: {
    hasPending() {
      return this.pendingCount > 0;
    }
  }
}
```

### Angular

```typescript
@Component({
  selector: 'app-print-labels',
  templateUrl: './print-labels.component.html'
})
export class PrintLabelsComponent implements OnInit {
  pendingCount$: Observable<number>;

  constructor(private labelsService: LabelsService) {}

  ngOnInit() {
    this.pendingCount$ = this.labelsService.getPendingCount(
      this.periodId,
      this.warehouseId
    ).pipe(
      map(response => response.count)
    );
  }
}

// Service
@Injectable()
export class LabelsService {
  getPendingCount(periodId: number, warehouseId: number) {
    return this.http.post<PendingCountResponse>(
      '/api/sigmav2/labels/pending-print-count',
      { periodId, warehouseId }
    );
  }
}
```

---

## 🧪 Testing

### cURL

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/pending-print-count \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "periodId": 16,
    "warehouseId": 369
  }'
```

### PowerShell

```powershell
$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer YOUR_TOKEN"
}

$body = @{
    periodId = 16
    warehouseId = 369
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/pending-print-count" `
    -Method Post `
    -Headers $headers `
    -Body $body

Write-Host "Marbetes pendientes: $($response.count)"
```

### Postman

```json
POST http://localhost:8080/api/sigmav2/labels/pending-print-count

Headers:
  Content-Type: application/json
  Authorization: Bearer {{token}}

Body (JSON):
{
  "periodId": 16,
  "warehouseId": 369
}
```

---

## 🔗 Relación con Otras APIs

### Flujo Completo de Impresión

```javascript
// 1. Verificar cuántos hay pendientes
const { count } = await getPendingPrintCount(periodId, warehouseId);

if (count === 0) {
  alert('No hay marbetes pendientes');
  return;
}

// 2. Confirmar con el usuario
if (!confirm(`¿Imprimir ${count} marbetes?`)) {
  return;
}

// 3. Imprimir
await printLabels(periodId, warehouseId);

// 4. Verificar que se imprimió todo
const { count: remaining } = await getPendingPrintCount(periodId, warehouseId);
if (remaining === 0) {
  alert('✓ Todos los marbetes fueron impresos exitosamente');
}
```

### APIs Relacionadas

| API | Propósito | Relación |
|-----|-----------|----------|
| `POST /labels/pending-print-count` | Contar pendientes | **Esta API** |
| `POST /labels/print` | Imprimir marbetes | Se llama después de verificar count > 0 |
| `POST /labels/for-count/list` | Listar marbetes | Muestra detalle de los marbetes |
| `POST /labels/generate` | Generar marbetes | Genera nuevos marbetes a imprimir |

---

## 📊 Reglas de Negocio

1. ✅ **Solo cuenta marbetes con estado GENERADO**
   - No cuenta IMPRESOS
   - No cuenta CANCELADOS

2. ✅ **Respeta permisos de almacén**
   - ADMINISTRADOR y AUXILIAR: Acceso a todos los almacenes
   - ALMACENISTA: Solo su almacén asignado

3. ✅ **Soporta filtro por producto**
   - Sin productId: Cuenta todos los pendientes
   - Con productId: Solo marbetes de ese producto

4. ✅ **No modifica datos**
   - Operación de solo lectura
   - Transaccional con readOnly=true

---

## 🎨 Mejores Prácticas UX

### ✅ DO: Mostrar Información Útil

```javascript
// Bueno
<div>
  <h3>{warehouseName}</h3>
  <p>{count} marbetes pendientes de impresión</p>
  <button>Imprimir Ahora</button>
</div>
```

### ❌ DON'T: Solo Mostrar Número

```javascript
// Malo
<div>
  <p>{count}</p>
  <button>Imprimir</button>
</div>
```

### ✅ DO: Actualizar en Tiempo Real

```javascript
// Actualizar después de imprimir
await printLabels();
await refreshPendingCount(); // Debería ser 0
```

### ✅ DO: Manejar Estado Vacío

```javascript
if (count === 0) {
  return (
    <div className="success">
      <Icon name="check-circle" />
      <p>Todos los marbetes están impresos</p>
    </div>
  );
}
```

---

## 📝 Notas Importantes

1. **Rendimiento:** La consulta es rápida (solo cuenta, no trae datos completos)

2. **Cache:** Considerar cachear el resultado por 10-30 segundos en el frontend

3. **Actualización:** Debe refrescarse después de:
   - Generar nuevos marbetes
   - Imprimir marbetes
   - Cancelar marbetes

4. **Consistencia:** El conteo es en tiempo real y siempre exacto

---

## 🔄 Changelog

### v1.0 - 2025-12-16
- ✅ Implementación inicial
- ✅ Soporte para filtro por producto
- ✅ Validación de permisos por almacén
- ✅ Información adicional (nombres de almacén y periodo)

---

**Documentación actualizada:** 2025-12-16

