# ✅ CHECKLIST DE PRUEBAS - FASE 1

## 🎯 Objetivo
Verificar que todas las mejoras de la Fase 1 funcionen correctamente.

---

## 📋 CHECKLIST COMPLETO

### ✅ **1. VALIDACIONES PREVIAS**

#### Test 1.1: Sin período seleccionado
- [ ] No seleccionar período
- [ ] Click en "Generar Marbetes"
- [ ] ✅ Debe mostrar: "Período no seleccionado - Debe seleccionar un período antes de generar marbetes."

#### Test 1.2: Sin almacén seleccionado
- [ ] Seleccionar período
- [ ] No seleccionar almacén
- [ ] Click en "Generar Marbetes"
- [ ] ✅ Debe mostrar: "Almacén no seleccionado - Debe seleccionar un almacén antes de generar marbetes."

#### Test 1.3: Sin productos con folios
- [ ] Seleccionar período y almacén
- [ ] NO ingresar cantidades en "Folios Solicitados"
- [ ] Click en "Generar Marbetes"
- [ ] ✅ Debe mostrar: "Sin productos para generar - No hay productos con folios solicitados..."

#### Test 1.4: Productos sin existencias
- [ ] Seleccionar período y almacén
- [ ] Ingresar cantidad en producto CON existencias = 0
- [ ] Click en "Generar Marbetes"
- [ ] ✅ Debe mostrar ADVERTENCIA: "⚠️ Productos sin existencias"
- [ ] ✅ Debe permitir continuar o cancelar

---

### ✅ **2. FEEDBACK VISUAL**

#### Test 2.1: Mensaje de ayuda
- [ ] Abrir página
- [ ] ✅ Ver mensaje púrpura con gradiente: "💡 Tip: Ingrese cantidades..."
- [ ] ✅ Verificar que el ícono tenga animación de pulso

#### Test 2.2: Tooltips en encabezados
- [ ] Hacer hover sobre "❓" en "Folios Solicitados"
- [ ] ✅ Ver tooltip: "Cantidad de marbetes a generar..."
- [ ] Hacer hover sobre "❓" en "Folios Existentes"
- [ ] ✅ Ver tooltip: "Cantidad de marbetes ya generados..."
- [ ] Hacer hover sobre "❓" en "Existencias"
- [ ] ✅ Ver tooltip: "Cantidad actual en inventario..."

#### Test 2.3: Indicador de guardado
- [ ] Ingresar cantidad en "Folios Solicitados"
- [ ] Hacer blur (click fuera del input)
- [ ] ✅ Ver indicador "💾 Guardando..." en esquina inferior derecha
- [ ] ✅ Indicador desaparece después de guardar

#### Test 2.4: Botón con estado de carga
- [ ] Click en "Generar Marbetes"
- [ ] ✅ Botón cambia a "Generando..."
- [ ] ✅ Botón se deshabilita
- [ ] ✅ Después de terminar, botón vuelve a "Generar Marbetes"

---

### ✅ **3. MANEJO DE ERRORES**

#### Test 3.1: Error de red
- [ ] Desconectar internet o apagar backend
- [ ] Intentar cargar marbetes
- [ ] ✅ Ver mensaje de error específico (no genérico)
- [ ] ✅ Toast con mensaje comprensible

#### Test 3.2: Período cerrado (simulado)
- [ ] Si hay período cerrado, seleccionarlo
- [ ] Intentar generar marbetes
- [ ] ✅ Debe mostrar: "Período cerrado - El período está en estado CERRADO..."

#### Test 3.3: Validación de entrada inválida
- [ ] Ingresar texto (no número) en "Folios Solicitados"
- [ ] Hacer blur
- [ ] ✅ No debe guardarse
- [ ] Ingresar número negativo
- [ ] Hacer blur
- [ ] ✅ Debe mostrar: "La cantidad no puede ser negativa"

---

### ✅ **4. GUARDADO DE FOLIOS**

#### Test 4.1: Guardar cantidad válida
- [ ] Ingresar "10" en un producto
- [ ] Hacer blur
- [ ] ✅ Ver "💾 Guardando..." (esquina inferior)
- [ ] ✅ Ver toast verde: "Guardado - 10 folio(s) solicitado(s)"
- [ ] ✅ Valor permanece en el input

#### Test 4.2: Guardar cero
- [ ] Cambiar cantidad a "0"
- [ ] Hacer blur
- [ ] ✅ Se guarda pero NO muestra toast de éxito
- [ ] ✅ Valor queda en 0

#### Test 4.3: Error al guardar
- [ ] Simular error (desconectar red)
- [ ] Ingresar cantidad y hacer blur
- [ ] ✅ Ver mensaje de error específico
- [ ] ✅ Valor se restaura al anterior

---

### ✅ **5. GENERACIÓN DE MARBETES**

#### Test 5.1: Generación exitosa
- [ ] Seleccionar período y almacén
- [ ] Ingresar cantidades en 3+ productos
- [ ] Click "Generar Marbetes"
- [ ] ✅ Ver validación previa (resumen)
- [ ] Confirmar
- [ ] ✅ Ver "Generando..." en botón
- [ ] ✅ Ver LoadAlert: "Generando marbetes..."
- [ ] ✅ Al terminar, ver modal con:
  - "✅ Total productos procesados: X"
  - "🏷️ Folios generados: Y"
  - "📋 Rango de folios: Z - W"

#### Test 5.2: Confirmación con resumen
- [ ] Iniciar generación
- [ ] ✅ Modal muestra:
  - Período seleccionado
  - Almacén seleccionado
  - Cantidad de productos
- [ ] ✅ Botones: "Sí, generar" y "Cancelar"

#### Test 5.3: Advertencia de productos sin existencias
- [ ] Generar producto con existencias = 0
- [ ] ✅ Ver modal de advertencia ANTES de confirmación
- [ ] ✅ Lista de productos sin existencias
- [ ] ✅ Pregunta: "¿Desea continuar?"
- [ ] Cancelar → No genera
- [ ] Continuar → Muestra confirmación normal

---

### ✅ **6. CARGA DE DATOS**

#### Test 6.1: Carga normal
- [ ] Seleccionar período y almacén
- [ ] ✅ Ver LoadAlert: "Cargando marbetes..."
- [ ] ✅ Ver tabla poblada con datos
- [ ] ✅ Verificar en consola: "✅ Cargados X marbetes"

#### Test 6.2: Sin datos
- [ ] Seleccionar período/almacén sin datos
- [ ] ✅ Ver mensaje: "No se encontraron marbetes..."

#### Test 6.3: Error de carga
- [ ] Desconectar red
- [ ] Cambiar período/almacén
- [ ] ✅ Ver error específico
- [ ] ✅ Tabla vacía con mensaje apropiado

---

### ✅ **7. ESTILOS Y ANIMACIONES**

#### Test 7.1: Mensaje de ayuda
- [ ] ✅ Gradiente púrpura visible
- [ ] ✅ Animación de entrada suave (slide-in)
- [ ] ✅ Ícono con pulso

#### Test 7.2: Indicador de guardado
- [ ] ✅ Aparece en esquina inferior derecha
- [ ] ✅ Fondo verde semi-transparente
- [ ] ✅ Spinner pequeño animado
- [ ] ✅ Transición de entrada/salida suave

#### Test 7.3: Tooltips
- [ ] Hover en "❓"
- [ ] ✅ Tooltip aparece arriba del ícono
- [ ] ✅ Fondo negro semi-transparente
- [ ] ✅ Flecha apuntando al ícono
- [ ] ✅ Ícono crece al hacer hover

#### Test 7.4: Botón generar
- [ ] Hover sobre botón (sin carga)
- [ ] ✅ Botón se eleva (translateY)
- [ ] ✅ Sombra azul más pronunciada

---

### ✅ **8. RESPONSIVE (OPCIONAL)**

#### Test 8.1: Móvil (< 768px)
- [ ] Abrir en móvil o reducir ventana
- [ ] ✅ Mensaje de ayuda: flex-direction column
- [ ] ✅ Botón "Generar" ocupa 100% ancho
- [ ] ✅ Tooltips siguen funcionando

---

### ✅ **9. LOGS Y DEBUGGING**

#### Test 9.1: Logs informativos
- [ ] Abrir consola del navegador
- [ ] Realizar operaciones
- [ ] ✅ Ver logs descriptivos:
  - "📥 Cargando marbetes: {body}"
  - "✅ Cargados X marbetes"
  - "📤 Generando marbetes: {total, body}"
  - "❌ Error al..." (en caso de error)

---

### ✅ **10. INTEGRACIÓN**

#### Test 10.1: Flujo completo
- [ ] Seleccionar período y almacén
- [ ] Esperar carga
- [ ] Ingresar cantidades en 5 productos
- [ ] Ver indicador "Guardando..." cada vez
- [ ] Click "Generar Marbetes"
- [ ] Ver advertencias si hay productos sin existencias
- [ ] Confirmar
- [ ] Ver "Generando..."
- [ ] Ver resultado con detalles
- [ ] Verificar que tabla se recarga automáticamente

---

## 📊 RESUMEN

Total de pruebas: **30+**

### Categorías:
- ✅ Validaciones: 4 tests
- ✅ Feedback visual: 4 tests
- ✅ Manejo de errores: 3 tests
- ✅ Guardado: 3 tests
- ✅ Generación: 3 tests
- ✅ Carga de datos: 3 tests
- ✅ Estilos: 4 tests
- ✅ Responsive: 1 test
- ✅ Logs: 1 test
- ✅ Integración: 1 test

---

## 🐛 REPORTE DE BUGS

Si encuentras algún problema, anótalo aquí:

### Bug #1
- **Qué hiciste:**
- **Qué esperabas:**
- **Qué pasó:**
- **Consola (errores):**

### Bug #2
- **Qué hiciste:**
- **Qué esperabas:**
- **Qué pasó:**
- **Consola (errores):**

---

## ✅ FIRMA DE APROBACIÓN

- [ ] Todas las pruebas pasaron
- [ ] Los errores encontrados fueron documentados
- [ ] La Fase 1 está lista para producción
- [ ] Listo para proceder con Fase 2

**Probado por:** _________________
**Fecha:** _________________
**Resultado:** ⭐⭐⭐⭐⭐ (1-5 estrellas)

---

## 🎉 ¡FASE 1 COMPLETA!

Una vez que todas las pruebas pasen, estaremos listos para la **Fase 2**.

**Próximas implementaciones:**
1. Consulta de pendientes antes de imprimir
2. Componente de registro de conteos (C1/C2)
3. Badges de estado con iconos
4. Validaciones de cancelación
