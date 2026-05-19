# Guía de Actualización de Diseño - Cambios Específicos

## 📐 Comparativa Antes vs Después

### 1. Header Section

**ANTES:**
```css
.header-section {
  background: #dc3545;  /* O variaba entre reportes */
  color: white;
  padding: 20px;
  border-radius: 8px 8px 0 0;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
}

.subtitle {
  font-size: 16px;
  color: #f8d7da;
}
```

**DESPUÉS:**
```css
.header-section {
  background: #f8f9fa;  /* Consistente en todos */
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.page-title {
  font-size: 24px;
  font-weight: 600;  /* Reducido a 600 */
  color: #333;
}

.subtitle {
  font-size: 14px;
  color: #6c757d;
}
```

### 2. Tabla - Header

**ANTES (Comparativos.vue como ejemplo):**
```css
.data-table thead {
  background: #f8f9fa;  /* Gris */
}

.data-table thead th {
  padding: 12px;
  color: #333;  /* Texto oscuro */
}
```

**DESPUÉS:**
```css
.data-table thead {
  background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);  /* Gradiente rojo */
}

.data-table thead th {
  padding: 16px;  /* +4px */
  color: white;   /* Texto blanco */
}
```

### 3. Tabla - Body (Hover)

**ANTES:**
```css
.data-table tbody tr:hover {
  background-color: #f1f1f1;  /* Gris neutro */
}
```

**DESPUÉS:**
```css
.data-table tbody tr:hover {
  background-color: #fff5f5;  /* Rojo muy claro (más armónico) */
}
```

### 4. Tabla - Celdas

**ANTES:**
```css
.data-table tbody td {
  padding: 12px;
}
```

**DESPUÉS:**
```css
.data-table tbody td {
  padding: 16px;  /* +4px para mejor espaciado */
}
```

### 5. Paginación - Botones

**ANTES:**
```css
.pagination-controls button:hover:not(:disabled) {
  background-color: #007bff;  /* Azul */
  color: white;
  border-color: #007bff;
}

.page-indicator {
  color: #007bff;  /* Azul */
}
```

**DESPUÉS:**
```css
.pagination-controls button:hover:not(:disabled) {
  background-color: #dc3545;  /* Rojo (consistente) */
  color: white;
  border-color: #dc3545;
}

.page-indicator {
  color: #dc3545;  /* Rojo */
}
```

## 🏷️ Badges - Sistema Consistente

### Warehouse Key (Púrpura)
```css
.warehouse-key {
  display: inline-block;
  padding: 4px 10px;
  background-color: #f3e5f5;  /* Púrpura muy claro */
  color: #7b1fa2;             /* Púrpura oscuro */
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
}
```

### Number Badge (Gris)
```css
.number-badge {
  display: inline-block;
  padding: 6px 12px;
  background-color: #e9ecef;  /* Gris claro */
  color: #495057;             /* Gris oscuro */
  border-radius: 6px;
  font-weight: 600;
  font-size: 14px;
}
```

### Folio Badge
```css
.folio-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 15px;
}

.folio-badge.cancelled {
  background-color: #f8d7da;  /* Rojo claro */
  color: #721c24;             /* Rojo oscuro */
}
```

### Status Badge (Multi-color)
```css
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 500;
  font-size: 12px;
}

.status-badge .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}

.status-badge.status-cancelled {
  background-color: #f8d7da;
  color: #721c24;
}

.status-badge.status-printed {
  background-color: #d1ecf1;
  color: #0c5460;
}

.status-badge.status-default {
  background-color: #e2e3e5;
  color: #495057;
}
```

## 📦 Estructura General del Contenedor

**ANTES (Variaba):**
```css
.reporte-container {
  display: flex;
  flex-direction: column;
  padding: 24px;
  background-color: #f8f9fa;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  gap: 24px;
}
```

**DESPUÉS (Consistente):**
```css
.reporte-container {
  display: flex;
  flex-direction: column;
  gap: 20px;  /* Reducido de 24px */
}
```

## 🎯 Controls Section

**ANTES (Variaba):**
- A veces con background #f8f9fa, a veces sin
- Padding inconsistente

**DESPUÉS (Consistente):**
```css
.controls-section {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  min-width: 160px;
  flex: 1;
}

.filter-select {
  width: 100%;
  padding: 10px 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #667eea;  /* Púrpura claro */
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}
```

## 💫 Animaciones y Transiciones

Todos los elementos mantienen:
```css
transition: all 0.3s ease;
```

### Stats Mini Hover Effect
```css
.stat-mini {
  transition: all 0.2s ease;
}

.stat-mini:hover {
  transform: translateY(-2px);  /* Sube 2px */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
```

## 📱 Responsive - Media Queries

Mantener consistente en todos los reportes:
```css
@media (max-width: 768px) {
  .pagination-section {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-controls {
    flex-wrap: wrap;
    justify-content: center;
  }
}
```

## 🔍 Verificación de Cambios

Para verificar que todos los cambios se aplicaron correctamente, buscar:

1. **Gradiente Rojo en Tablas**:
   ```
   background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
   ```

2. **Padding de 16px en Celdas**:
   ```
   padding: 16px;
   ```

3. **Hover Background Rojo Claro**:
   ```
   background-color: #fff5f5;
   ```

4. **Page Indicator Rojo**:
   ```
   color: #dc3545;
   ```

---

**Documentación completada**: Marzo 2026

