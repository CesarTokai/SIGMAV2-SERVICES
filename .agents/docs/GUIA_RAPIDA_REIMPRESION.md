## 🚀 GUÍA RÁPIDA: REIMPRESIÓN EXTRAORDINARIA

### ✨ LO QUE SE IMPLEMENTÓ

**Nueva pantalla:** Reimpresión Extraordinaria de Marbetes
**Ubicación:** Admin → Gestión de Marbetes → Pestaña "📄 Reimpresión"

---

### 🎯 CÓMO USAR EN 5 PASOS

#### 1️⃣ ACCEDER A LA PANTALLA
```
Dashboard → Admin → Gestión de Marbetes
           ↓
Verás 4 pestañas:
  📋 Consulta y Captura
  🖨️  Impresión
  🔢 Conteo
  📄 Reimpresión  ← NUEVA
```

#### 2️⃣ SELECCIONAR PERÍODO Y ALMACÉN
```
Período:  [Selector ▼] Elige el período
          (Se guarda automáticamente)

El almacén se carga en el dropdown inferior
```

#### 3️⃣ BUSCAR FOLIO
```
Ingresa: número del folio (ej: 195)
Presiona: Botón "Buscar" o Enter
          ↓
Si existe y está IMPRESO:
✅ Se carga automáticamente la información
```

#### 4️⃣ REVISAR INFORMACIÓN
```
Verás todos los detalles:
  ✓ Folio
  ✓ Producto y clave
  ✓ Almacén
  ✓ Estado (IMPRESO en verde)
  ✓ Fecha de impresión original
  ✓ Historial: cuántas veces se reimpresó
  ✓ Existencias esperadas
```

#### 5️⃣ REIMPRIMIR
```
Haz click en: "📄 Reimprimir Marbete" (botón rojo)
               ↓
Aparece modal: "¿Reimprimir marbete?"
               ↓
Confirma: "Sí, reimprimir"
               ↓
✅ PDF se descarga automáticamente
   Nombre: reimpresion_folio_195_[TIMESTAMP].pdf
```

---

### ⌨️ ATAJOS DE TECLADO

Para trabajar más rápido:

```
Alt + F  → Enfoca en búsqueda de folio (para nueva búsqueda)
Alt + L  → Limpia todo el formulario
Escape   → Limpia y enfoca en folio
Enter    → Busca (estando en input de folio)
```

---

### 🔒 ¿QUÉ ESTÁ PROTEGIDO?

Este sistema tiene **4 niveles de seguridad**:

✅ **Solo busca marbetes IMPRESOS**
   - No puedes reimprimir un marbete que no esté impreso

✅ **Bloquea marbetes cancelados**
   - Si el folio fue cancelado, dirá "No encontrado"

✅ **Valida periodo y almacén**
   - Debes seleccionar ambos antes de buscar

✅ **Pide confirmación explícita**
   - Modal avisa: "Esta es una reimpresión extraordinaria"

---

### 📊 INFORMACIÓN VISIBLE

Cuando busques un folio, verás:

```
┌─────────────────────────────────────────┐
│  Folio: [195]  ← Badge azul            │
├─────────────────────────────────────────┤
│  Producto: Producto A                   │
│  Clave: COM-5CLNQ                       │
│  Almacén: Almacén Principal             │
├─────────────────────────────────────────┤
│  Estado: ✓ IMPRESO (badge verde)       │
│  Reimpresiones previas: 0               │
│  Fecha de impresión: 19/02/2026 10:30  │
│  Existencias: 150 unidades              │
└─────────────────────────────────────────┘
```

---

### ❌ CASOS QUE NO FUNCIONAN

| Intento | Resultado |
|---------|-----------|
| Reimprimir folio GENERADO | ❌ ERROR: Estado inválido |
| Reimprimir folio CANCELADO | ❌ ERROR: No encontrado |
| Buscar sin período | ❌ ERROR: Selecciona período |
| Buscar sin almacén | ❌ ERROR: Selecciona almacén |
| Buscar folio que no existe | ❌ ERROR: No encontrado |

---

### 💾 LO QUE SUCEDE EN BACKEND

Cuando confirmas la reimpresión:

```
1. Se envía: POST /labels/print
   Parámetro clave: forceReprint = true

2. Backend:
   ✓ Verifica que folio esté IMPRESO
   ✓ Busca ese folio en la BD
   ✓ Genera PDF nuevo
   ✓ Incrementa contador de reimpresiones
   ✓ Registra la acción en logs

3. Devuelve:
   ✓ PDF (blob) para descargar
   ✓ Tu navegador descarga automáticamente
```

---

### 🎨 APARIENCIA

**Mismo diseño que Conteo de Marbetes:**
- Card de información clara
- Inputs bien diferenciados
- Botones con colores distintos
- Responsive (funciona en móvil)

**Colores:**
- 🔴 Botón Reimpresión (rojo - acción importante)
- 🟢 Estado IMPRESO (verde - confirmado)
- 🔵 Folio (azul - identificador)

---

### 📝 EJEMPLO COMPLETO

**Escenario:** Necesitas reimprimir el folio 195

```
1. Entra a Admin → Gestión de Marbetes → Reimpresión

2. Ves:
   ✓ Período: Enero 2026 (ya seleccionado)
   ✓ Almacén: Almacén Principal (cargado)

3. Escribes: 195 en el input
   Presionas: Enter o Buscar

4. Sistema busca y encuentra:
   ✓ Folio 195
   ✓ Producto: Harina de Trigo
   ✓ Clave: HAR-0001
   ✓ Estado: IMPRESO ✓
   ✓ Reimpresiones previas: 0
   ✓ Fecha: 15/02/2026 08:00
   ✓ Existencias: 500

5. Haces click: "📄 Reimprimir Marbete"

6. Aparece modal:
   "¿Reimprimir marbete?"
   Folio: 195
   Producto: Harina de Trigo
   [Cancelar] [Sí, reimprimir]

7. Haces click: "Sí, reimprimir"

8. Sistema procesa:
   - Genera PDF nuevo
   - Registra reimpresión en BD
   - Incrementa contador a 1

9. Tu navegador descarga:
   reimpresion_folio_195_2026-02-19T10-30-00.pdf

10. Ves Toast: "✅ Reimpresión completada"

11. Formulario se limpia:
    - Input de folio vacío
    - Información desaparece
    - Enfoque en input de folio

12. Listo para siguiente búsqueda
```

---

### 🔄 FLUJO TÍPICO DE TRABAJO

```
Usuario trabaja en ritmo:

1. Alt+F (enfoca folio) → Escribe 195 → Enter
2. Revisa información (2 seg)
3. Click en Reimprimir
4. Confirma en modal
5. Descarga automática (1 seg)
6. Toast "Completado"
7. Alt+F → Escribe 196 → Enter
8. Repite...

⏱️ Tiempo por marbete: ~5-10 segundos
```

---

### ✅ CHECKLIST DE USO

Antes de reimprimir, verifica:

- [ ] ¿El período está correcto?
- [ ] ¿El almacén es el correcto?
- [ ] ¿El folio existe?
- [ ] ¿El estado dice "IMPRESO"?
- [ ] ¿Revisaste historial de reimpresiones?
- [ ] ¿Las existencias son correctas?

Si todo está bien:
- [ ] Haz click en "Reimprimir"
- [ ] Confirma en modal
- [ ] Verifica que PDF se descargó

---

### 🆘 SOLUCIÓN DE PROBLEMAS

**P: "No encontrado" al buscar**
R: Verifica:
   - ¿Está IMPRESO? (no GENERADO ni CANCELADO)
   - ¿Es el período correcto?
   - ¿Es el almacén correcto?

**P: El botón "Reimprimir" está gris/deshabilitado**
R: Significa:
   - No hay folio seleccionado, o
   - El folio no está IMPRESO

**P: ¿Cómo sé si se reimpresó?**
R: Por los Toast:
   - ✅ Verde: Reimpresión completada
   - ❌ Rojo: Error en la reimpresión

**P: ¿Se puede reimprimir múltiples folios?**
R: Esta versión: uno por uno
   Próximas versiones podrían soportar lotes

---

### 📚 DOCUMENTACIÓN COMPLETA

Para detalles técnicos, ver:
```
docs/IMPLEMENTACION_REIMPRESION_MARBETES.md
docs/RESUMEN_REIMPRESION_MARBETES.md
```

---

### 🎓 RESUMEN

**¿Qué hace?**
Permite reimprimir marbetes que ya fueron impresos.

**¿Por qué es extraordinaria?**
Porque normalmente los marbetes se imprimen una sola vez.

**¿Es seguro?**
Sí. Tiene validaciones en 4 niveles (frontend + backend).

**¿Afecta marbetes anteriores?**
No. Solo genera PDF nuevo y registra la acción.

**¿Quién puede usarlo?**
Usuarios con rol ADMINISTRADOR.

---

**¡Listo para usar!** 🚀

Accede a: Admin → Gestión de Marbetes → Reimpresión

