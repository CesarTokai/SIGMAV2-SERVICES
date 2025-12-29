# Guía Rápida: Nueva API de Impresión de Marbetes

## 🎯 Cambio Principal

**Ya NO se requieren `startFolio` y `endFolio`**

El sistema ahora imprime automáticamente todos los marbetes pendientes.

---

## 📋 Endpoint

```
POST /api/sigmav2/labels/print
```

---

## 🔑 Autenticación

Requiere JWT token en header:
```
Authorization: Bearer {token}
```

**Roles permitidos:**
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

---

## 📥 Casos de Uso

### 1️⃣ Impresión Automática (RECOMENDADO)

**Caso más común**: Imprimir todos los marbetes pendientes de un periodo/almacén

```json
POST /api/sigmav2/labels/print

{
  "periodId": 16,
  "warehouseId": 369
}
```

**Respuesta:**
- Archivo PDF descargable
- Nombre: `marbetes_P16_A369_20251216_120000.pdf`
- Contiene TODOS los marbetes con estado GENERADO

**Comportamiento:**
- ✅ Busca automáticamente marbetes pendientes
- ✅ Los ordena por folio
- ✅ Los marca como IMPRESOS
- ✅ Genera el PDF

---

### 2️⃣ Impresión por Producto

**Caso**: Imprimir solo marbetes de un producto específico

```json
POST /api/sigmav2/labels/print

{
  "periodId": 16,
  "warehouseId": 369,
  "productId": 123
}
```

**Uso típico:**
- Imprimir marbetes de productos urgentes
- Organizar impresión por categorías
- Distribuir impresión entre operadores

---

### 3️⃣ Reimpresión Selectiva

**Caso**: Reimprimir folios específicos (por daño, pérdida, etc.)

```json
POST /api/sigmav2/labels/print

{
  "periodId": 16,
  "warehouseId": 369,
  "folios": [25, 26, 27],
  "forceReprint": true
}
```

**⚠️ Importante:**
- `folios`: Lista de folios específicos a reimprimir
- `forceReprint`: OBLIGATORIO en `true` para reimprimir folios ya impresos
- Sin `forceReprint: true`, la API retornará error si los folios ya están impresos

---

## 🚨 Errores Comunes y Soluciones

### Error: "No hay marbetes pendientes de impresión"

**Causa:** Todos los marbetes ya están impresos o no hay marbetes generados

**Solución:**
1. Verificar que se hayan generado marbetes
2. Si necesita reimprimir, usar modo selectivo con `forceReprint: true`

```json
{
  "periodId": 16,
  "warehouseId": 369,
  "folios": [1, 2, 3, 4, 5],
  "forceReprint": true
}
```

---

### Error: "El folio X ya está IMPRESO. Use forceReprint=true"

**Causa:** Intentó reimprimir sin autorización explícita

**Solución:**
Agregar `forceReprint: true`:

```json
{
  "periodId": 16,
  "warehouseId": 369,
  "folios": [10],
  "forceReprint": true
}
```

---

### Error: "El folio X está CANCELADO"

**Causa:** Intentó imprimir un folio cancelado

**Solución:**
- Los folios cancelados NO se pueden imprimir
- Remover el folio de la lista
- Generar un nuevo marbete si es necesario

---

### Error: "Folio X no encontrado"

**Causa:** El folio no existe para el periodo/almacén especificado

**Solución:**
- Verificar que el folio sea correcto
- Verificar periodo y almacén
- Consultar folios disponibles con `/api/sigmav2/labels/for-count/list`

---

## 💡 Ejemplos de Integración Frontend

### React/TypeScript

```typescript
// Impresión automática
const printAllPendingLabels = async (periodId: number, warehouseId: number) => {
  try {
    const response = await fetch('/api/sigmav2/labels/print', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        periodId,
        warehouseId
      })
    });

    if (!response.ok) {
      throw new Error('Error al imprimir marbetes');
    }

    // Descargar PDF
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `marbetes_P${periodId}_A${warehouseId}.pdf`;
    a.click();

  } catch (error) {
    console.error('Error:', error);
    alert('No se pudieron imprimir los marbetes');
  }
};

// Reimpresión selectiva
const reprintSpecificLabels = async (
  periodId: number,
  warehouseId: number,
  folios: number[]
) => {
  try {
    const response = await fetch('/api/sigmav2/labels/print', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        periodId,
        warehouseId,
        folios,
        forceReprint: true
      })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Error al reimprimir');
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `reimpresion_marbetes.pdf`;
    a.click();

  } catch (error) {
    console.error('Error:', error);
    alert(error.message);
  }
};
```

---

### JavaScript/Fetch

```javascript
// Imprimir todos los pendientes
async function imprimirMarbetes(periodId, warehouseId) {
  const response = await fetch('/api/sigmav2/labels/print', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + localStorage.getItem('token')
    },
    body: JSON.stringify({
      periodId: periodId,
      warehouseId: warehouseId
    })
  });

  if (response.ok) {
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    window.open(url);
  } else {
    const error = await response.json();
    alert('Error: ' + error.message);
  }
}
```

---

### Axios

```javascript
import axios from 'axios';

// Imprimir automático
const printLabels = async (periodId, warehouseId) => {
  try {
    const response = await axios.post(
      '/api/sigmav2/labels/print',
      {
        periodId,
        warehouseId
      },
      {
        headers: {
          'Authorization': `Bearer ${token}`
        },
        responseType: 'blob' // Importante para PDFs
      }
    );

    // Crear URL del blob y descargar
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `marbetes_${periodId}_${warehouseId}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();

  } catch (error) {
    console.error('Error:', error.response?.data || error.message);
    alert('Error al imprimir marbetes');
  }
};

// Reimprimir con folios específicos
const reprintLabels = async (periodId, warehouseId, folios) => {
  try {
    const response = await axios.post(
      '/api/sigmav2/labels/print',
      {
        periodId,
        warehouseId,
        folios,
        forceReprint: true
      },
      {
        headers: {
          'Authorization': `Bearer ${token}`
        },
        responseType: 'blob'
      }
    );

    // Descargar PDF
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'reimpresion_marbetes.pdf');
    document.body.appendChild(link);
    link.click();
    link.remove();

  } catch (error) {
    if (error.response?.status === 400) {
      alert('Error: ' + (error.response.data.message || 'Solicitud inválida'));
    } else {
      alert('Error al reimprimir marbetes');
    }
  }
};
```

---

## 🎨 UI Recomendada

### Botón Principal: "Imprimir Marbetes Pendientes"

```jsx
<button onClick={() => printAllPendingLabels(periodId, warehouseId)}>
  📄 Imprimir Marbetes Pendientes
</button>
```

**Comportamiento:**
- Click → Imprime automáticamente todos los pendientes
- Sin diálogos de confirmación adicionales
- Descarga directa del PDF

---

### Opción Avanzada: "Reimprimir Folios"

```jsx
<button onClick={() => showReprintDialog()}>
  🔄 Reimprimir Folios Específicos
</button>
```

**Dialog de reimpresión:**
```
┌─────────────────────────────────────┐
│ Reimprimir Marbetes                 │
├─────────────────────────────────────┤
│ Ingrese los folios a reimprimir:    │
│ (separados por coma)                │
│                                     │
│ [_____________________________]     │
│ Ejemplo: 10, 15, 20                 │
│                                     │
│ [Cancelar]  [Reimprimir]            │
└─────────────────────────────────────┘
```

---

## 📊 Flujo Completo Recomendado

```
1. Usuario selecciona Periodo y Almacén
   ↓
2. Sistema verifica si hay marbetes pendientes
   ↓
3a. SI HAY PENDIENTES:
    → Mostrar botón "Imprimir Marbetes Pendientes"
    → Click → Imprime automáticamente
    ↓
3b. NO HAY PENDIENTES:
    → Mostrar mensaje "Todos los marbetes están impresos"
    → Opción: "Reimprimir folios específicos"
```

---

## ✅ Checklist de Migración Frontend

- [ ] Eliminar campos `startFolio` y `endFolio` de formularios
- [ ] Cambiar a impresión automática por defecto
- [ ] Agregar opción de reimpresión selectiva (opcional)
- [ ] Manejar error "No hay pendientes"
- [ ] Actualizar mensajes de usuario
- [ ] Probar descarga de PDF
- [ ] Validar permisos por rol

---

## 🔗 APIs Relacionadas

Para consultar qué marbetes existen:

```
POST /api/sigmav2/labels/for-count/list
{
  "periodId": "16",
  "warehouseId": "369"
}
```

Retorna lista con todos los marbetes y su estado (GENERADO, IMPRESO, CANCELADO).

---

## 📞 Soporte

Si tienes dudas sobre la integración, revisa:
- `MEJORA-IMPRESION-AUTOMATICA-MARBETES.md` (documentación completa)
- Ejemplos en `/frontend-examples/`
- Postman collection actualizada

