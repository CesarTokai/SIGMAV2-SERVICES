# 🔧 Guía de Integración Frontend: Solución al Problema de Folios

## 🎯 Problema Identificado

Estabas viendo información **agrupada por producto** en la tabla, pero intentabas usar esos datos como si fueran marbetes individuales.

**Ejemplo del problema:**
```
Tabla muestra:
- Producto: COM-5CLNQ
- Folios Existentes: 3

Pero NO muestra que esos 3 folios son: [3, 6, 9]
```

Cuando intentabas imprimir o consultar, usabas el valor "3" pensando que era un folio, pero en realidad "3" es la **cantidad** de folios, no un folio individual.

---

## ✅ Solución Implementada

### 1. Endpoint Mejorado: `/summary` (Modificado)

Ahora incluye la información de folios individuales:

**Request:**
```http
POST /api/sigmav2/labels/summary
Content-Type: application/json
Authorization: Bearer {token}

{
  "periodId": 1,
  "warehouseId": 369,
  "page": 0,
  "size": 10
}
```

**Response MEJORADA:**
```json
[
  {
    "productId": 6626,
    "claveProducto": "COM-5CLNQ",
    "nombreProducto": "CUBRE FLAMA M4L NIQUELADO",
    "claveAlmacen": "15",
    "nombreAlmacen": "Almacén 15",
    "foliosSolicitados": 3,
    "foliosExistentes": 3,
    "estado": "A",
    "existencias": 158532,
    "impreso": true,
    "fechaImpresion": "2025-12-05T14:17:00",

    // ✨ NUEVO: Información de folios individuales
    "primerFolio": 3,
    "ultimoFolio": 9,
    "folios": [3, 6, 9]  // ← Lista de folios individuales
  }
]
```

### 2. Nuevo Endpoint: `/product/{productId}` (Detalle Completo)

Para obtener información detallada de cada marbete individual:

**Request:**
```http
GET /api/sigmav2/labels/product/6626?periodId=1&warehouseId=369
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "folio": 3,
    "productId": 6626,
    "claveProducto": "COM-5CLNQ",
    "nombreProducto": "CUBRE FLAMA M4L NIQUELADO",
    "warehouseId": 369,
    "claveAlmacen": "15",
    "nombreAlmacen": "Almacén 15",
    "periodId": 1,
    "estado": "IMPRESO",
    "createdAt": "2025-12-05T14:00:00",
    "impresoAt": "2025-12-05T14:17:00",
    "existencias": 158532
  },
  {
    "folio": 6,
    "productId": 6626,
    "claveProducto": "COM-5CLNQ",
    "nombreProducto": "CUBRE FLAMA M4L NIQUELADO",
    "warehouseId": 369,
    "claveAlmacen": "15",
    "nombreAlmacen": "Almacén 15",
    "periodId": 1,
    "estado": "GENERADO",
    "createdAt": "2025-12-05T14:00:00",
    "impresoAt": null,
    "existencias": 158532
  },
  {
    "folio": 9,
    "productId": 6626,
    "claveProducto": "COM-5CLNQ",
    "nombreProducto": "CUBRE FLAMA M4L NIQUELADO",
    "warehouseId": 369,
    "claveAlmacen": "15",
    "nombreAlmacen": "Almacén 15",
    "periodId": 1,
    "estado": "GENERADO",
    "createdAt": "2025-12-05T14:00:00",
    "impresoAt": null,
    "existencias": 158532
  }
]
```

---

## 📱 Integración en Vue.js

### Servicio API (marbetes-service.js)

```javascript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/sigmav2';

// Configurar interceptor para token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default {

  /**
   * Obtener resumen de marbetes (con folios individuales)
   */
  async consultarResumen(periodId, warehouseId) {
    try {
      const response = await axios.post(`${API_BASE_URL}/labels/summary`, {
        periodId,
        warehouseId,
        page: 0,
        size: 100,
        sortBy: 'claveProducto',
        sortDirection: 'ASC'
      });
      return { success: true, data: response.data };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Error al consultar'
      };
    }
  },

  /**
   * Obtener detalles de marbetes de un producto (NUEVO)
   */
  async obtenerMarbetesProducto(productId, periodId, warehouseId) {
    try {
      const response = await axios.get(
        `${API_BASE_URL}/labels/product/${productId}`,
        { params: { periodId, warehouseId } }
      );
      return { success: true, data: response.data };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Error al consultar marbetes'
      };
    }
  },

  /**
   * Imprimir marbetes (usando folios reales)
   */
  async imprimirMarbetes(periodId, warehouseId, folioInicial, folioFinal) {
    try {
      const response = await axios.post(
        `${API_BASE_URL}/labels/print`,
        {
          periodId,
          warehouseId,
          startFolio: folioInicial,
          endFolio: folioFinal
        },
        { responseType: 'blob' }
      );

      // Descargar PDF
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `marbetes_${folioInicial}_${folioFinal}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();

      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Error al imprimir'
      };
    }
  }
};
```

### Componente Vue (ImpresionMarbetes.vue)

```vue
<template>
  <div class="impresion-marbetes">
    <h2>Impresión de Marbetes</h2>

    <!-- Filtros -->
    <div class="filtros">
      <select v-model="periodoSeleccionado">
        <option v-for="p in periodos" :key="p.id" :value="p.id">
          {{ p.nombre }}
        </option>
      </select>

      <select v-model="almacenSeleccionado">
        <option v-for="a in almacenes" :key="a.id" :value="a.id">
          {{ a.nombre }}
        </option>
      </select>

      <button @click="cargarDatos">Consultar</button>
    </div>

    <!-- Tabla de Productos -->
    <table>
      <thead>
        <tr>
          <th>Clave</th>
          <th>Producto</th>
          <th>Folios Generados</th>
          <th>Rango</th>
          <th>Estado</th>
          <th>Acciones</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="producto in productos" :key="producto.productId">
          <td>{{ producto.claveProducto }}</td>
          <td>{{ producto.nombreProducto }}</td>
          <td>{{ producto.foliosExistentes }}</td>
          <td>
            <!-- ✨ USAR primerFolio y ultimoFolio -->
            <span v-if="producto.primerFolio && producto.ultimoFolio">
              {{ producto.primerFolio }} - {{ producto.ultimoFolio }}
            </span>
            <span v-else>-</span>
          </td>
          <td>
            <span :class="getEstadoClass(producto)">
              {{ getEstadoTexto(producto) }}
            </span>
          </td>
          <td>
            <!-- Botón para ver detalles -->
            <button
              @click="verDetalles(producto)"
              class="btn-secundario"
              v-if="producto.foliosExistentes > 0">
              Ver Detalles
            </button>

            <!-- Botón para imprimir TODOS los marbetes del producto -->
            <button
              @click="imprimirProducto(producto)"
              class="btn-primario"
              v-if="producto.foliosExistentes > 0"
              :disabled="!producto.primerFolio">
              Imprimir
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Modal de Detalles -->
    <div v-if="mostrarModal" class="modal">
      <div class="modal-content">
        <h3>Marbetes de {{ productoSeleccionado?.nombreProducto }}</h3>

        <table>
          <thead>
            <tr>
              <th>Folio</th>
              <th>Estado</th>
              <th>Fecha Creación</th>
              <th>Fecha Impresión</th>
              <th>Acción</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="marbete in marbetesDetalle" :key="marbete.folio">
              <td>{{ marbete.folio }}</td>
              <td>
                <span :class="getEstadoClassMarbete(marbete.estado)">
                  {{ marbete.estado }}
                </span>
              </td>
              <td>{{ formatFecha(marbete.createdAt) }}</td>
              <td>{{ formatFecha(marbete.impresoAt) }}</td>
              <td>
                <button
                  @click="imprimirMarbete(marbete.folio)"
                  class="btn-sm">
                  Imprimir
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <button @click="cerrarModal">Cerrar</button>
      </div>
    </div>
  </div>
</template>

<script>
import MarbetesService from '@/services/marbetes-service';
import Swal from 'sweetalert2';

export default {
  name: 'ImpresionMarbetes',

  data() {
    return {
      periodos: [],
      almacenes: [],
      periodoSeleccionado: null,
      almacenSeleccionado: null,
      productos: [],
      mostrarModal: false,
      productoSeleccionado: null,
      marbetesDetalle: []
    };
  },

  mounted() {
    this.cargarPeriodos();
    this.cargarAlmacenes();
  },

  methods: {
    async cargarPeriodos() {
      // Implementar según tu API
    },

    async cargarAlmacenes() {
      // Implementar según tu API
    },

    async cargarDatos() {
      if (!this.periodoSeleccionado || !this.almacenSeleccionado) {
        Swal.fire('Error', 'Selecciona periodo y almacén', 'error');
        return;
      }

      const result = await MarbetesService.consultarResumen(
        this.periodoSeleccionado,
        this.almacenSeleccionado
      );

      if (result.success) {
        // ✨ Ahora cada producto tiene primerFolio, ultimoFolio y folios[]
        this.productos = result.data;
        console.log('Productos cargados:', this.productos);
      } else {
        Swal.fire('Error', result.error, 'error');
      }
    },

    async verDetalles(producto) {
      console.log('Ver detalles de producto:', producto);

      const result = await MarbetesService.obtenerMarbetesProducto(
        producto.productId,
        this.periodoSeleccionado,
        this.almacenSeleccionado
      );

      if (result.success) {
        this.productoSeleccionado = producto;
        this.marbetesDetalle = result.data;
        this.mostrarModal = true;

        console.log('Marbetes detalle:', this.marbetesDetalle);
      } else {
        Swal.fire('Error', result.error, 'error');
      }
    },

    async imprimirProducto(producto) {
      // ✨ Ahora usamos primerFolio y ultimoFolio correctamente
      if (!producto.primerFolio || !producto.ultimoFolio) {
        Swal.fire('Error', 'No hay folios disponibles para imprimir', 'error');
        return;
      }

      const confirmacion = await Swal.fire({
        title: '¿Imprimir Marbetes?',
        html: `
          <p>Se imprimirán los folios del <strong>${producto.primerFolio}</strong>
          al <strong>${producto.ultimoFolio}</strong></p>
          <p>Total: <strong>${producto.foliosExistentes}</strong> marbete(s)</p>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: 'Sí, imprimir',
        cancelButtonText: 'Cancelar'
      });

      if (!confirmacion.isConfirmed) return;

      const result = await MarbetesService.imprimirMarbetes(
        this.periodoSeleccionado,
        this.almacenSeleccionado,
        producto.primerFolio,  // ← Usar folio REAL
        producto.ultimoFolio   // ← Usar folio REAL
      );

      if (result.success) {
        await Swal.fire({
          icon: 'success',
          title: '✅ Impresión Exitosa',
          text: 'El PDF se ha descargado correctamente',
          timer: 2000
        });

        // Recargar datos para actualizar estado
        this.cargarDatos();
      } else {
        Swal.fire('Error', result.error, 'error');
      }
    },

    async imprimirMarbete(folio) {
      // Imprimir un solo marbete
      const result = await MarbetesService.imprimirMarbetes(
        this.periodoSeleccionado,
        this.almacenSeleccionado,
        folio,  // startFolio
        folio   // endFolio (mismo folio)
      );

      if (result.success) {
        Swal.fire('Éxito', 'Marbete impreso', 'success');
        this.verDetalles(this.productoSeleccionado); // Recargar detalles
      } else {
        Swal.fire('Error', result.error, 'error');
      }
    },

    cerrarModal() {
      this.mostrarModal = false;
      this.productoSeleccionado = null;
      this.marbetesDetalle = [];
    },

    getEstadoClass(producto) {
      if (!producto.foliosExistentes) return 'estado-pendiente';
      if (producto.impreso) return 'estado-impreso';
      return 'estado-generado';
    },

    getEstadoTexto(producto) {
      if (!producto.foliosExistentes) return 'Sin generar';
      if (producto.impreso) return 'Impreso';
      return 'Generado';
    },

    getEstadoClassMarbete(estado) {
      return {
        'GENERADO': 'badge-warning',
        'IMPRESO': 'badge-success',
        'CANCELADO': 'badge-danger'
      }[estado] || 'badge-secondary';
    },

    formatFecha(fecha) {
      if (!fecha) return '-';
      return new Date(fecha).toLocaleString('es-MX');
    }
  }
};
</script>

<style scoped>
.estado-pendiente { color: gray; }
.estado-generado { color: orange; }
.estado-impreso { color: green; }

.badge-warning { background: orange; color: white; padding: 4px 8px; border-radius: 4px; }
.badge-success { background: green; color: white; padding: 4px 8px; border-radius: 4px; }
.badge-danger { background: red; color: white; padding: 4px 8px; border-radius: 4px; }

.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  max-width: 800px;
  max-height: 80vh;
  overflow-y: auto;
}
</style>
```

---

## 🔑 Puntos Clave

### ❌ Lo que NO debes hacer:
```javascript
// ❌ INCORRECTO: Usar foliosExistentes como folio
const folioInicial = producto.foliosExistentes; // 3
const folioFinal = producto.foliosExistentes;   // 3
// Esto imprimirá solo el folio 3, no los 3 marbetes!
```

### ✅ Lo que SÍ debes hacer:
```javascript
// ✅ CORRECTO: Usar primerFolio y ultimoFolio
const folioInicial = producto.primerFolio; // 3
const folioFinal = producto.ultimoFolio;   // 9
// Esto imprimirá los folios 3, 4, 5, 6, 7, 8, 9
```

O mejor aún:
```javascript
// ✅ MEJOR: Usar la lista de folios individuales
producto.folios.forEach(folio => {
  console.log('Folio:', folio);
  // Puedes consultar o imprimir cada uno
});
```

---

## 🧪 Pruebas

### 1. Probar Resumen con Folios
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/summary \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"periodId":1,"warehouseId":369,"page":0,"size":10}'
```

### 2. Probar Detalle de Producto
```bash
curl -X GET "http://localhost:8080/api/sigmav2/labels/product/6626?periodId=1&warehouseId=369" \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Probar Impresión con Folios Reales
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/print \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"periodId":1,"warehouseId":369,"startFolio":3,"endFolio":9}' \
  --output marbetes.pdf
```

---

## 📊 Resumen de Cambios

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `primerFolio` | Long | Primer folio generado del producto |
| `ultimoFolio` | Long | Último folio generado del producto |
| `folios` | List<Long> | Lista completa de folios individuales |

**Endpoint Nuevo:**
- `GET /api/sigmav2/labels/product/{productId}` - Detalle completo de marbetes

---

## ✅ Checklist de Integración

- [ ] Actualizar servicio de API con nuevo endpoint
- [ ] Modificar componente para usar `primerFolio` y `ultimoFolio`
- [ ] Agregar botón "Ver Detalles" que llame al nuevo endpoint
- [ ] Usar folios reales al imprimir (no usar `foliosExistentes`)
- [ ] Mostrar modal con lista de marbetes individuales
- [ ] Probar impresión individual de marbetes
- [ ] Probar impresión por rango de folios

---

**¡Ahora tu frontend tendrá la información correcta de los folios! 🎉**

