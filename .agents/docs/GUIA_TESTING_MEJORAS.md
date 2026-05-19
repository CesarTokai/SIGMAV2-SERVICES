# Guía de Testing - Nuevas Mejoras

## 🧪 Pruebas Recomendadas

Ejecuta estas pruebas para validar que las mejoras funcionan correctamente.

---

## 📋 TEST 1: Búsqueda Flexible (Espacios)

### Escenario: Buscar con espacios inconsistentes

**Datos previos:**
- Período: 20/12/2025
- Almacén: Almacén A
- Productos en tabla: "PROD001", "PROD_ABC", "PRODUCTOXYZ"

**Pasos:**

1. Abre "Consulta Captura" (Admin o Auxiliar)
2. Selecciona Período: 20/12/2025
3. Selecciona Almacén: Almacén A
4. En el SearchBar, escribe: `P R O D 0 0 1`

**Resultado esperado:**
- ✅ Debería encontrar "PROD001" incluso con espacios
- ✅ La tabla se filtra mostrando solo ese producto
- ✅ Resultado en < 600ms (debido al debounce de 500ms)

**Variantes a probar:**
- `prod001` (minúsculas)
- `PROD   001` (múltiples espacios)
- `p r o d 0 0 1` (todos separados)
- `P R O D_A B C` (con underscore)

---

## ✅ TEST 2: Prevención de Duplicados

### Escenario: Generar marbetes sin enviar todos

**Datos previos:**
- Tabla con 8 productos
- De los 8, algunos tienen `Folios Existentes = 0` (pendientes)

**Pasos:**

1. Abre tabla con marbetes
2. En el header, verifica que hay un checkbox `☑` (Seleccionar Todo)
3. **Deselecciona todos** clickeando el checkbox del header
4. Selecciona manualmente solo **2 productos** (pon check en sus filas)
5. Presiona botón "Generar Marbetes"

**Resultado esperado:**

Modal debería mostrar:
```
📋 Productos a generar (2):
  • Producto A: X folio(s) a generar
  • Producto B: Y folio(s) a generar
```

**NO debería mostrar:**
- Los 8 productos de la tabla
- Productos no seleccionados

**Confirmación en Backend (opcional):**
- Revisar logs del servidor
- Debería recibir: `products: [{productId: X, ...}, {productId: Y, ...}]`
- **NO** debería recibir 8 productos

---

## 🎯 TEST 3: Seleccionar Todos / Deseleccionar Todos

### Escenario: Selección masiva

**Pasos:**

1. Abre tabla con 5+ productos
2. Todos los checkboxes están ☐ (sin marcar)
3. Click en checkbox del header (lado izquierdo, arriba)

**Resultado esperado:**
- ✅ Todos los checkboxes de filas se marcan ☑
- ✅ Todas las filas se resaltan con fondo amarillo
- ✅ Clase `.selected-row` aplica

**Segundo click en header:**
- ✅ Todos los checkboxes se desmarcan ☐
- ✅ Se quita el highlighting amarillo

---

## 📊 TEST 4: Modal Detallado

### Escenario: Verificar información en el modal

**Pasos:**

1. Tabla con estos productos:
   ```
   Producto   | Folios Sol. | Folios Ext.
   -----------|-------------|------------
   PROD_001   | 1           | 0
   PROD_002   | 2           | 1
   PROD_003   | 1           | 1
   ```

2. Selecciona PROD_001 y PROD_002 (checkbox)
3. Presiona "Generar Marbetes"

**Resultado esperado - Modal debe mostrar:**

```
¿Generar Marbetes?

📅 Período: [fecha actual]
🏢 Almacén: [almacén actual]

📋 Productos a generar (2):
  • PROD_001: 1 folio(s) a generar
  • PROD_002: 1 folio(s) a generar    ← (2-1 = 1 faltante)
```

**NOT:**
- ❌ PROD_003 (no seleccionado)
- ❌ Productos sin folios solicitados

---

## 🔍 TEST 5: Filtrado + Selección Combinados

### Escenario: Búsqueda y selección

**Pasos:**

1. Tabla tiene 10 productos
2. Escribe en SearchBar: `ABC`
3. Tabla filtra mostrando solo 3 productos con "ABC"
4. Selecciona 2 de los 3 mostrados (checkbox)
5. Presiona "Generar Marbetes"

**Resultado esperado:**

Modal debe mostrar:
- ✅ Solo 2 productos (los 2 seleccionados)
- ✅ NO los 10 originales
- ✅ NO los otros resultados de búsqueda sin marcar

---

## ⚠️ TEST 6: Validaciones (Casos de Error)

### 6.1 - Sin seleccionar productos

**Pasos:**
1. Abre tabla
2. **No selecciones nada** (todos los checkboxes ☐)
3. Presiona "Generar Marbetes"

**Resultado esperado:**
- ✅ Error: "Sin productos para generar"
- ✅ O message similar indicando que selecciones productos

### 6.2 - Sin período/almacén

**Pasos:**
1. Abre Consulta Captura
2. **No selecciones Período ni Almacén**
3. Intenta presionar "Generar Marbetes"

**Resultado esperado:**
- ✅ Botón está disabled (gris)
- ✅ O error si logras presionarlo

### 6.3 - Período cerrado

**Pasos:**
1. Selecciona un Período con estado "CERRADO"
2. Intenta generar

**Resultado esperado:**
- ✅ Error: "Período cerrado"
- ✅ No permite generar

---

## 📝 TEST 7: Casos Edge (Extremos)

### 7.1 - Muchos productos seleccionados

**Pasos:**
1. Tabla con 100+ productos
2. Selecciona todo (checkbox en header)
3. Presiona "Generar Marbetes"

**Resultado esperado:**
- ✅ Modal sigue siendo rápido
- ✅ Lista scrolleable si hay muchos
- ✅ Se procesa sin errores

### 7.2 - Un solo producto

**Pasos:**
1. Tabla con 1 solo producto
2. Selecciona el checkbox
3. Presiona "Generar Marbetes"

**Resultado esperado:**
- ✅ Modal muestra 1 producto
- ✅ Se genera correctamente
- ✅ Sin errores

### 7.3 - Texto de búsqueda muy largo

**Pasos:**
1. SearchBar: escribe texto muy largo con muchos espacios
2. Sistema debería normalizar sin problemas

**Resultado esperado:**
- ✅ No hay lag/delay excesivo
- ✅ Búsqueda funciona normalmente

---

## 🔄 TEST 8: Flujo Completo

### Escenario: Usuario típico - Generar folios faltantes

**Pasos:**

```
1. Abre Consulta Captura (Admin o Auxiliar)
   ✓ Sistema carga tabla

2. Selecciona Período: 20/12/2025
   ✓ Tabla se actualiza
   ✓ Se ven productos con diferentes estados

3. Selecciona Almacén: Almacén A
   ✓ Tabla filtra por almacén

4. Observa la tabla:
   - PROD001: Folios Sol=1, Ext=0 (falta)
   - PROD002: Folios Sol=2, Ext=2 (completo)
   - PROD003: Folios Sol=1, Ext=0 (falta)

5. Busca en SearchBar: "PROD 00" (con espacios)
   ✓ Encuentra PROD001, PROD002, PROD003
   ✓ Ignora espacios correctamente

6. Selecciona solo PROD001 y PROD003 (checkboxes)
   ✓ Filas se resaltan en amarillo

7. Presiona "Generar Marbetes"
   ✓ Modal muestra:
     - 2 productos a generar
     - 1 folio cada uno
     - Detalles específicos

8. Confirma "Sí, generar"
   ✓ Sistema envía 2 productos (NO 3)
   ✓ Sin duplicados

9. Resultado:
   ✓ PROD001: Folio X asignado
   ✓ PROD003: Folio Y asignado
   ✓ PROD002: No afectado
```

**Resultado esperado:**
- ✅ Flujo completo sin errores
- ✅ Folios asignados correctamente
- ✅ No hay duplicados

---

## 🐛 TEST 9: Debugging (Si algo falla)

### Para investigar problemas:

1. **Abre DevTools** (F12)
2. **Console:** Busca mensajes con 📥 o 📤
   ```
   📤 Generando marbetes: { total: 2, productIds: [1, 3], ... }
   ```
3. **Network:** Mira la petición POST a `/labels/generate/batch`
   - Verifica que solo envía los productos seleccionados
   - Busca el campo `products` en el body

4. **Elements:** Inspecciona checkboxes
   - Deberían tener atributo `type="checkbox"`
   - `v-model="marbete.selected"`

---

## ✅ Checklist de Validación

- [ ] TEST 1: Búsqueda con espacios funciona
- [ ] TEST 2: Solo se envían productos seleccionados
- [ ] TEST 3: Seleccionar/Deseleccionar todo funciona
- [ ] TEST 4: Modal muestra detalles correctos
- [ ] TEST 5: Filtrado + selección combinados
- [ ] TEST 6: Validaciones funcionan
- [ ] TEST 7: Casos extremos no rompen nada
- [ ] TEST 8: Flujo completo exitoso
- [ ] TEST 9: Debugging mostró valores correctos

---

## 📊 Reporte de Pruebas

Crea un reporte con:

```markdown
## Pruebas Ejecutadas: [FECHA]

### TEST 1: Búsqueda Flexible
- ✅ PASÓ / ❌ FALLÓ
- Notas: [observaciones]

### TEST 2: Prevención de Duplicados
- ✅ PASÓ / ❌ FALLÓ
- Notas: [observaciones]

... (continuar para todos)

### Resultado Final
✅ TODAS PASARON
(O listar las que fallaron)
```

---

## 🚀 Próximos Pasos

Si **TODAS** las pruebas pasan:
1. ✅ Cambios listos para PRODUCCIÓN
2. ✅ Deploy a ambiente de staging
3. ✅ Feedback final del usuario
4. ✅ Deploy a producción

Si **ALGUNAS** fallan:
1. 🔍 Revisar los logs
2. 📧 Reportar el error específico
3. 🛠️ Ajustar el código
4. 🔄 Re-ejecutar pruebas

---

**Fecha de Testing:** [Completa con tu fecha]
**Tester:** [Tu nombre]
**Resultado:** ✅ PASÓ / ❌ FALLÓ

---

Para consultas sobre las pruebas, revisar:
- `docs/VISUAL_MEJORAS.md` - Cómo se ve la UI
- `docs/SOLUCION_ENVIO_DUPLICADOS.md` - Detalles técnicos
- `docs/MEJORA_BUSCADOR_ESPACIOS.md` - Detalles del buscador

