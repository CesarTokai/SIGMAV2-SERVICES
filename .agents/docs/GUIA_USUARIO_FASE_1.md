# 📘 GUÍA DE USO - MEJORAS FASE 1

## 🎯 Resumen
Esta guía explica las nuevas características implementadas en la Fase 1 del módulo de Consulta y Captura de Marbetes.

---

## 🆕 **NUEVAS CARACTERÍSTICAS**

### 1️⃣ **Mensaje de Ayuda Contextual**

Al abrir la página, verás un mensaje destacado en color púrpura:

```
💡 Tip: Ingrese cantidades en "Folios Solicitados" antes de generar.
     Los marbetes se numeran automáticamente.
```

**Qué significa:**
- Debes ingresar las cantidades que deseas generar ANTES de hacer click en "Generar Marbetes"
- Los números de folio se asignan automáticamente (no necesitas especificarlos)

---

### 2️⃣ **Tooltips de Ayuda**

En los encabezados de la tabla verás iconos de ayuda **❓**

**Cómo usar:**
1. Coloca el mouse sobre el ícono **❓**
2. Aparecerá un tooltip explicando qué significa esa columna

**Ejemplo:**
- Hover en "❓" junto a "Folios Solicitados"
- Ve: *"Cantidad de marbetes a generar. Se recomienda generar la misma cantidad que las existencias."*

---

### 3️⃣ **Indicador de Guardado**

Cuando ingresas una cantidad y sales del campo (blur):

**Qué verás:**
- Esquina inferior derecha: 💾 **Guardando...**
- El indicador desaparece al terminar
- Toast verde: **"Guardado - 10 folio(s) solicitado(s)"**

**Si hay error:**
- Toast rojo con mensaje específico
- El valor se restaura al anterior

---

### 4️⃣ **Validaciones Automáticas**

Al hacer click en **"Generar Marbetes"**, el sistema valida automáticamente:

#### ❌ **Errores que detienen la operación:**
- Período no seleccionado
- Almacén no seleccionado
- Período cerrado o bloqueado
- Sin productos con folios solicitados

#### ⚠️ **Advertencias (puedes continuar):**
- Productos sin existencias

**Ejemplo de error:**
```
❌ Período no seleccionado
   Debe seleccionar un período antes de generar marbetes.
```

---

### 5️⃣ **Confirmación con Resumen**

Antes de generar, verás un resumen:

```
¿Generar Marbetes?

Período: Enero 2026 - Inventario Mensual
Almacén: CEDIS Principal
Productos: Se generarán marbetes para 15 producto(s)

[Cancelar]  [Sí, generar]
```

---

### 6️⃣ **Advertencia de Productos sin Existencias**

Si intentas generar marbetes para productos con existencias = 0:

```
⚠️ Productos sin existencias

Se generarán marbetes para 3 producto(s) sin existencias:

• Producto A
• Producto B
• Producto C

¿Desea continuar?

[Cancelar]  [Sí, continuar]
```

**Recomendación:** Solo continúa si estás seguro. Normalmente, los marbetes se generan para productos con existencias.

---

### 7️⃣ **Indicador Durante Generación**

Mientras se generan los marbetes:

1. Botón cambia a: **"Generando..."**
2. Botón se deshabilita (no puedes hacer click)
3. Loading overlay: **"Generando marbetes..."**

**Espera** hasta que termine el proceso.

---

### 8️⃣ **Resultado Detallado**

Al finalizar exitosamente, verás:

```
✅ ¡Éxito!

✅ Total productos procesados: 15
🏷️ Folios generados: 150
📋 Rango de folios: 1001 - 1150

[Aceptar]
```

**Qué significa:**
- **Total productos:** Cuántos productos se procesaron
- **Folios generados:** Cantidad total de marbetes creados
- **Rango:** Primer y último número de folio asignado

---

### 9️⃣ **Mensajes de Error Específicos**

Si algo sale mal, verás mensajes claros y accionables:

#### Ejemplos:

**Error de período cerrado:**
```
❌ Error al generar marbetes

El período está cerrado. No se pueden realizar cambios.
Contacte al administrador.
```

**Error de producto sin existencias (backend):**
```
❌ Error al generar marbetes

El producto no tiene existencias registradas en el almacén.
```

**Error de red:**
```
❌ Error al cargar marbetes

Error de conexión. Verifique su conexión a internet.
```

---

## 🔄 **FLUJO COMPLETO DE USO**

### **Paso 1: Seleccionar Filtros**
1. Selecciona un **Período** del dropdown
2. Selecciona un **Almacén** del dropdown
3. Espera a que cargue la tabla

### **Paso 2: Ingresar Cantidades**
1. En la columna **"Folios Solicitados"**, ingresa las cantidades
2. Presiona Tab o click fuera del campo
3. Verás el indicador **"💾 Guardando..."**
4. Toast verde confirma el guardado

### **Paso 3: Generar Marbetes**
1. Click en botón **"Generar Marbetes"**
2. Sistema valida automáticamente
3. Si hay advertencias (productos sin existencias), decide si continuar
4. Confirma en el modal de resumen
5. Espera a que termine (botón dice "Generando...")
6. Ve el resultado detallado

### **Paso 4: Verificar Resultado**
1. La tabla se recarga automáticamente
2. Verifica que **"Folios Existentes"** ahora muestre las cantidades
3. Revisa los estados de los productos

---

## ���� **SOLUCIÓN DE PROBLEMAS**

### **Problema 1: No puedo generar marbetes**

**Posibles causas:**
- ❌ No seleccionaste período → Selecciona uno
- ❌ No seleccionaste almacén → Selecciona uno
- ❌ No ingresaste cantidades → Ingresa folios solicitados
- ❌ Período cerrado → Contacta al administrador

---

### **Problema 2: No se guardan las cantidades**

**Posibles causas:**
- ❌ Número inválido → Ingresa solo números enteros
- ❌ Número negativo → Usa números positivos o cero
- ❌ Sin conexión → Verifica tu internet
- ❌ Sin período/almacén → Selecciona ambos primero

---

### **Problema 3: No veo los tooltips**

**Solución:**
- Asegúrate de hacer hover (pasar el mouse) sobre el ícono **❓**
- Espera 0.5 segundos
- Si no aparece, refresca la página

---

### **Problema 4: El indicador de guardado no desaparece**

**Solución:**
- Espera unos segundos más
- Si persiste (>10 segundos), refresca la página
- Verifica tu conexión a internet

---

## 💡 **CONSEJOS Y MEJORES PRÁCTICAS**

### ✅ **Recomendado:**
1. **Genera cantidades iguales a existencias:** Si hay 50 unidades, genera 50 marbetes
2. **Verifica antes de confirmar:** Lee el resumen antes de hacer click en "Sí, generar"
3. **Espera a que termine:** No refresques la página mientras dice "Generando..."
4. **Lee los tooltips:** Te ayudarán a entender cada campo

### ❌ **Evita:**
1. **Generar marbetes sin existencias:** A menos que sea intencional
2. **Cerrar la ventana mientras genera:** Espera a que termine
3. **Hacer click múltiple en "Generar":** Click una vez y espera

---

## 📋 **ATAJOS Y TIPS**

### **Navegación rápida:**
- **Tab:** Moverse entre campos de "Folios Solicitados"
- **Enter:** Guardar y pasar al siguiente (si está configurado)

### **Validación visual:**
- 🟢 Verde = Éxito
- 🔴 Rojo = Error
- 🟠 Naranja = Advertencia
- 🔵 Azul = Información

### **Logs para debugging:**
- Abre consola del navegador (F12)
- Busca mensajes con emojis:
  - 📥 = Cargando
  - ✅ = Éxito
  - 📤 = Enviando
  - ❌ = Error

---

## ❓ **PREGUNTAS FRECUENTES**

### **¿Puedo generar marbetes sin existencias?**
Sí, pero el sistema te advertirá. Solo hazlo si estás seguro.

### **¿Los folios se asignan automáticamente?**
Sí, el sistema asigna números consecutivos automáticamente.

### **¿Puedo cambiar las cantidades después de generar?**
Sí, pero deberás cancelar los marbetes existentes primero.

### **¿Qué pasa si hay un error?**
El sistema te mostrará un mensaje específico explicando qué salió mal y cómo solucionarlo.

### **¿Puedo generar para múltiples productos a la vez?**
Sí, ingresa cantidades en varios productos y haz click una vez en "Generar Marbetes".

---

## 📞 **SOPORTE**

Si encuentras problemas no documentados aquí:

1. **Revisa la consola** (F12) para ver logs de error
2. **Toma screenshot** del error y del estado de la página
3. **Anota los pasos** que llevaron al problema
4. **Contacta al equipo de desarrollo** con toda la información

---

## 🎉 **¡Listo para usar!**

Ahora tienes todas las herramientas para trabajar eficientemente con el sistema de marbetes.

**Recuerda:**
- 💡 Los tooltips están ahí para ayudarte
- ⚠️ Las advertencias son importantes
- ✅ Las validaciones previenen errores

**¡Disfruta de las mejoras!** 🚀
