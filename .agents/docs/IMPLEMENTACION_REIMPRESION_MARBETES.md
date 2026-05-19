## 📄 REIMPRESIÓN EXTRAORDINARIA DE MARBETES

### ✅ Implementación Completada

Se ha creado la nueva pantalla **ReimpresionMarbetes.vue** con todas las funcionalidades necesarias.

---

### 🎯 UBICACIÓN

**Ruta en la aplicación:**
```
Admin → Gestión de Marbetes → Pestaña "Reimpresión" (📄)
```

**Ruta del archivo:**
```
src/modules/admin/views/marbetesAdmin/ReimpresionMarbetes.vue
```

**Integración:**
- Añadida como submódulo en `MarbetesLayout.vue`
- Accesible desde las 4 opciones del menú de Gestión de Marbetes

---

### 🔧 CARACTERÍSTICAS PRINCIPALES

#### 1. **Búsqueda de Marbetes**
- Selecciona Período y Almacén
- Ingresa el número de folio
- Busca marbetes en estado **IMPRESO**

#### 2. **Validaciones de Seguridad**
✅ Solo busca marbetes IMPRESOS
✅ Valida periodo y almacén seleccionados
✅ Previene reimpresión de marbetes cancelados
✅ Valida estado antes de reimprimir

#### 3. **Información Completa del Marbete**
Muestra:
- Folio (con badge azul)
- Producto y clave
- Almacén
- Estado (IMPRESO - con badge verde)
- Fecha de impresión original
- Cantidad de reimpresiones previas
- Existencias esperadas

#### 4. **Reimpresión Extraordinaria**
- Modal de confirmación con detalles
- Llamada API: `POST /labels/print` con `forceReprint=true`
- Genera PDF descargable automáticamente
- Registra histórico de reimpresiones
- Nombre archivo: `reimpresion_folio_[FOLIO]_[TIMESTAMP].pdf`

---

### 🎮 USO DE ATAJOS DE TECLADO

```
Alt + F    → Enfoca en input de folio
Alt + L    → Limpia el formulario
Escape     → Limpia y enfoca en folio
Enter      → Ejecuta búsqueda (desde input de folio)
```

---

### 📡 ENDPOINTS UTILIZADOS

#### 1. Buscar marbete IMPRESO
```http
POST /labels/for-reprint
Body: {
  "folio": 195,
  "periodId": 7,
  "warehouseId": 218
}

Response: {
  "id": 123,
  "folio": 195,
  "estado": "IMPRESO",
  "producto": "...",
  "claveProducto": "...",
  "fechaImpresion": "2026-02-19T10:30:00",
  "reimpresionesAnteriores": 0,
  ...
}
```

#### 2. Generar reimpresión (PDF)
```http
POST /labels/print
Body: {
  "periodId": 7,
  "warehouseId": 218,
  "folios": [195],
  "forceReprint": true
}

Response: blob (PDF)
```

---

### 🔐 SEGURIDAD IMPLEMENTADA

**4 niveles de validación:**

1. **Frontend - Búsqueda:**
   - Valida que folio no esté vacío
   - Verifica período y almacén seleccionados

2. **Frontend - Información:**
   - Solo muestra marbetes en estado IMPRESO
   - Bloquea si el estado es diferente

3. **Frontend - Confirmación:**
   - Modal pide confirmación explícita
   - Muestra historial de reimpresiones

4. **Backend - API:**
   - Valida folio existe
   - Verifica estado IMPRESO
   - Registra reimpresión en BD

**Matriz de Protección:**

| Escenario | Acción | Resultado |
|-----------|--------|-----------|
| Folio no impreso | Intentar reimprimir | ❌ ERROR: Estado inválido |
| Folio cancelado | Intentar reimprimir | ❌ ERROR: No encontrado |
| Folio impreso | Buscar y reimprimir | ✅ Genera PDF |
| Sin folio | Intentar reimprimir | ❌ ERROR: Ingresa folio |

---

### 🎨 DISEÑO Y UX

**Patrón de diseño:** Idéntico a `ConteoMarbetes.vue`
- Card de información de período
- Información completa del marbete
- Sección de búsqueda clara
- Botones de acción diferenciados por color
- Responsive para móvil
- Info box de ayuda

**Colores:**
- Botón de reimpresión: Rojo (#d32f2f)
- Estado IMPRESO: Verde (#4CAF50)
- Folio badge: Azul (#2196F3)

---

### 🚀 FLUJO COMPLETO

```
1. Usuario accede a Admin > Gestión de Marbetes > Reimpresión
   ↓
2. Selecciona Período (se guarda en store)
   ↓
3. Selecciona Almacén
   ↓
4. Ingresa folio y presiona Buscar (o Enter)
   ↓
5. Se busca en API: /labels/for-reprint
   ↓
6. Se valida que esté IMPRESO
   ↓
7. Se muestra información completa del marbete
   ↓
8. Usuario ve botón "📄 Reimprimir Marbete"
   ↓
9. Al hacer click: Modal de confirmación
   ↓
10. Confirma: Envía POST /labels/print con forceReprint=true
   ↓
11. Backend genera PDF y devuelve blob
   ↓
12. Frontend descarga automáticamente: reimpresion_folio_[N]_[timestamp].pdf
   ↓
13. Muestra Toast: "Reimpresión completada"
   ↓
14. Limpia formulario y enfoca en folio
   ↓
15. Listo para siguiente búsqueda
```

---

### 📋 ESTADO DE IMPLEMENTACIÓN

✅ Componente Vue creado
✅ Integrado en MarbetesLayout
✅ Búsqueda de marbetes IMPRESOS
✅ Reimpresión extraordinaria
✅ Descarga automática de PDF
✅ Validaciones completas
✅ Atajos de teclado
✅ Responsive design
✅ Mismo patrón visual que Conteo

---

### 💡 NOTAS IMPORTANTES

1. **Endpoint Backend:** Se requiere que el backend tenga:
   - `POST /labels/for-reprint` - para buscar marbete IMPRESO
   - `POST /labels/print` - con soporte para `forceReprint=true`

2. **Store de Período:** Reutiliza `usePeriodoStore()` para mantener consistencia

3. **Historial:** El campo `reimpresionesAnteriores` muestra cuántas veces se ha reimpreso

4. **Descarga:** Automática sin necesidad de confirmación adicional

---

### ❓ FAQ

**P: ¿Puedo reimprimir un marbete que no está IMPRESO?**
R: No. Solo se pueden reimprimir marbetes en estado IMPRESO.

**P: ¿Se registra el historial de reimpresiones?**
R: Sí. El campo `reimpresionesAnteriores` se incrementa cada reimpresión.

**P: ¿Qué ocurre si cancelo la confirmación?**
R: Se cancela la reimpresión y no se genera PDF.

**P: ¿Puedo reimprimir múltiples folios a la vez?**
R: En esta versión, se reimprimen uno por uno. El API soporta múltiples folios si es necesario.

---

### 🔄 PRÓXIMAS MEJORAS (Opcionales)

- [ ] Buscar múltiples folios en una pantalla
- [ ] Reimpresión en lote
- [ ] Historial visual de reimpresiones
- [ ] Exportar reporte de reimpresiones

