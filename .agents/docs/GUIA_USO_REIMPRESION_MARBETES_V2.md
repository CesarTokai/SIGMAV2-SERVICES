# 📄 Guía de Uso: Reimpresión de Marbetes (Nueva Versión)

## 🎯 Introducción

La **Reimpresión Extraordinaria de Marbetes** ahora utiliza el **mismo patrón intuitivo que el Conteo de Marbetes**, permitiendo:

1. ✅ **Búsqueda simple**: Ingresa período → ingresa folio → obtienes información
2. ✅ **Validaciones automáticas**: El sistema valida que el marbete esté IMPRESO
3. ✅ **Reimpresión con confirmación**: Modal de confirmación antes de descargar PDF
4. ✅ **Historial automático**: El backend registra cada reimpresión

---

## 📋 Flujo Paso a Paso

### **1️⃣ Acceder al Módulo**

```
Menú Principal
    ↓
Módulo: Administrador de Marbetes
    ↓
Opción: Reimpresión de Marbetes
```

### **2️⃣ Seleccionar Período**

En la sección superior, verás:
```
Período: [Dropdown con fechas y comentarios]
```

**Acciones:**
- Si tienes un período guardado anteriormente → Se carga automáticamente
- Si no → Selecciona el período que necesitas de la lista

**Ejemplo:**
```
Período: 2026-02-19 - Inventario Mensual
```

### **3️⃣ Seleccionar Almacén**

Aunque no aparezca en el selector del título, el sistema:
- ✅ Usa el almacén cargado de sesiones anteriores
- ✅ O auto-selecciona el primero disponible

**Nota:** El almacén se sincroniza automáticamente cuando seleccionas el período.

### **4️⃣ Buscar Marbete por Folio**

En la sección "Buscar Folio para Reimprimir":

```
┌─────────────────────────────────┐
│ Buscar Folio para Reimprimir:   │
│                                 │
│ [Ingresa folio] [Buscar]        │
│                                 │
│ 💡 Busca marbetes en estado     │
│    IMPRESO para reimprimir      │
└─────────────────────────────────┘
```

**Pasos:**
1. Haz click en el input de folio
2. Ingresa el número del folio (ej: `195`)
3. Presiona `Enter` o hace click en "Buscar"

**Validaciones automáticas:**

| Escenario | Resultado |
|-----------|-----------|
| Folio no ingresado | ❌ Error: "Ingresa un folio para buscar" |
| Período no seleccionado | ❌ Error: "Selecciona período y almacén" |
| Folio no existe | ❌ Error: "No se encontró el folio" |
| Folio está GENERADO | ❌ Error: "Estado inválido - Solo IMPRESOS" |
| Folio está CANCELADO | ❌ Error: "Marbete Cancelado" |
| Folio está IMPRESO ✅ | ✅ Marbete cargado |

### **5️⃣ Información del Marbete**

Una vez encontrado el marbete, se muestra:

```
┌─────────────────────────────────────────────┐
│ Folio: 195                                  │
│ Producto: PRODUCTO ABC                      │
│ Clave: PROD-001                             │
│                                             │
│ Almacén: ALMACÉN CENTRAL                    │
│ Estado: ✓ IMPRESO                           │
│ Existencias: 1,250 unidades                 │
└─────────────────────────────────────────────┘
```

**Campos mostrados:**
- 📌 **Folio**: Número del marbete
- 📦 **Producto**: Nombre del producto
- 🏷️ **Clave**: Código del producto
- 🏢 **Almacén**: Ubicación del almacén
- 📊 **Estado**: Debe ser "IMPRESO"
- 📈 **Existencias**: Cantidad esperada

### **6️⃣ Reimprimir Marbete**

Una vez cargado el marbete, verás dos botones:

```
┌──────────────────────────────────┐
│ 📄 Reimprimir Marbete │ Limpiar  │
└──────────────────────────────────┘
```

**Paso 1: Click en "Reimprimir Marbete"**

El sistema mostrará una confirmación:

```
┌─────────────────────────────────────────┐
│ ¿Reimprimir marbete?                    │
│                                         │
│ Folio: 195                              │
│ Producto: PRODUCTO ABC                  │
│ Almacén: ALMACÉN CENTRAL                │
│ Estado: IMPRESO                         │
│                                         │
│ ⚠️  Esta es una reimpresión extraordinaria│
│                                         │
│ ¿Deseas continuar con la reimpresión?  │
│                                         │
│ [Sí, reimprimir]  [Cancelar]           │
└─────────────────────────────────────────┘
```

**Paso 2: Confirmar**

- Haz click en "Sí, reimprimir"
- El sistema generará el PDF
- Se descargará automáticamente con nombre: `reimpresion_folio_195_2026-02-23T15-30-45.pdf`

**Paso 3: Finalización**

```
✅ Reimpresión completada
Folio 195 reimpreso correctamente
```

El formulario se limpia automáticamente y queda listo para buscar otro folio.

---

## ⌨️ Atajos de Teclado

Para agilizar la búsqueda:

| Atajo | Acción | Dónde |
|-------|--------|-------|
| `Enter` | Buscar folio | Input de folio |
| `Alt + F` | Limpiar y enfocar folio | Cualquier lugar |
| `Alt + L` | Limpiar formulario | Cualquier lugar |
| `Escape` | Limpiar y enfocar folio | Cualquier lugar |

**Ejemplo de flujo rápido:**

```
1. Alt + F → Limpia y enfoca en input de folio
2. 195 → Ingresa folio
3. Enter → Busca automáticamente
4. Tab o click → Va a botón "Reimprimir"
5. Enter → Confirma reimpresión
```

---

## 🔄 Sincronización del Período

El período se **guarda automáticamente** cuando lo cambias:

**Sesión 1:**
```
Selecciono Período: 2026-02-19
Sistema guarda en store
```

**Sesión 2 (después de recargar):**
```
Abro ReimpresionMarbetes
Período cargado automáticamente: 2026-02-19 ✅
```

**Para cambiar a otro período:**
```
1. Click en selector de período
2. Elige nuevo período
3. Sistema guarda automáticamente
4. Puedes comenzar a buscar folios
```

---

## 📊 Tabla de Referencia: Estados Permitidos

| Estado | ¿Puede Reimprimirse? | Notas |
|--------|----------------------|-------|
| **GENERADO** | ❌ No | Debe imprimirse primero en ImpresionMarbetes |
| **IMPRESO** | ✅ Sí | Estado correcto para reimpresión |
| **CONTADO** | ❌ No | Requiere impresión del formato de conteo |
| **CANCELADO** | ❌ No | No se puede reimpresionar marbetes cancelados |

---

## 🚨 Errores Comunes y Soluciones

### **Error 1: "Ingresa un folio para buscar"**

**Causa:** No ingresaste el número del folio

**Solución:**
```
1. Click en el input "Buscar Folio"
2. Ingresa el número (ej: 195)
3. Presiona Enter o click "Buscar"
```

---

### **Error 2: "Selecciona período y almacén"**

**Causa:** No hay período seleccionado

**Solución:**
```
1. Ve a la sección superior
2. Click en el selector "Período"
3. Elige un período de la lista
4. Intenta buscar nuevamente
```

---

### **Error 3: "No se encontró el folio"**

**Causa:** El folio no existe en este período/almacén

**Soluciones:**
```
1. Verifica el número de folio (¿está correcto?)
2. Verifica el período seleccionado (¿es el correcto?)
3. Consulta en ConteoMarbetes si el folio existe
```

---

### **Error 4: "Solo se pueden reimprimir marbetes IMPRESOS"**

**Causa:** El folio está en estado GENERADO, CONTADO o CANCELADO

**Solución:**
```
- Si está GENERADO → Ve a ImpresionMarbetes.vue y imprímelo
- Si está CONTADO → Es un estado intermedio, contacta soporte
- Si está CANCELADO → No puede reimprimirse
```

---

### **Error 5: "Marbete Cancelado"**

**Causa:** El marbete fue cancelado y no puede reimprimirse

**Solución:**
```
- Este marbete no se puede recuperar
- Si fue cancelado por error, contacta al administrador
- Considera solicitar nuevos folios si es necesario
```

---

## 💡 Consejos de Uso

### **1. Usa el Almacenamiento de Período**
- El período se guarda automáticamente
- No necesitas seleccionarlo cada vez
- Cambiar de período es instantáneo

### **2. Valida Antes de Reimprimir**
- Lee la información mostrada
- Confirma que el folio sea el correcto
- Revisa el modal de confirmación

### **3. Descarga Organizada**
- Los PDF se descargan en tu carpeta de descargas
- Tienen timestamp para evitar sobrescrituras
- Puedes imprimir directamente desde el navegador

### **4. Usa Atajos de Teclado**
- `Alt + F` para búsqueda rápida
- `Enter` para buscar sin mouse
- `Escape` para limpiar y comenzar de nuevo

### **5. Ten Presente el Flujo**
```
Período → Folio → Buscar → Revisar Información → Reimprimir → Confirmar → Descargar
```

---

## 📱 Dispositivos Soportados

- ✅ **Desktop/Laptop**: Experiencia completa
- ✅ **Tablet**: Funcional con atajos limitados
- ⚠️ **Móvil**: Funcional pero no recomendado (PDF en pantalla pequeña)

---

## 🔐 Datos de Seguridad

**¿Qué información se registra?**

Cuando reimpres un marbete, el backend registra:
- ✅ Folio
- ✅ Período
- ✅ Almacén
- ✅ Fecha/Hora de reimpresión
- ✅ Usuario que realizó la reimpresión
- ✅ Número de reimpresión (1a, 2a, etc.)

**¿Es seguro reimprimir?**

✅ **100% Seguro** porque:
1. Solo se pueden reimprimir marbetes IMPRESOS
2. Existe confirmación modal
3. Cada reimpresión queda registrada
4. El backend valida estado antes de procesar

---

## 📞 Soporte

Si encuentras algún problema:

1. **Verifica los Errores Comunes** arriba
2. **Revisa la Documentación Técnica**: `IMPLEMENTACION_REIMPRESION_PATRON_CONTEO.md`
3. **Contacta al Administrador** si:
   - No puedes acceder al módulo
   - Ves marbetes en estado incorrecto
   - Los PDF no se descargan

---

## 📚 Documentación Relacionada

- 📖 `IMPLEMENTACION_REIMPRESION_PATRON_CONTEO.md` - Detalles técnicos
- 📖 `APIS_CONSUMIDAS_ALMACENISTA.md` - Endpoints utilizados
- 📖 `GUIA_USUARIO_FASE_1.md` - Guía general del sistema
- 📖 `MANUAL_USO_MARBETES.md` - Manual completo de marbetes

---

## ✅ Checklist de Uso Correcto

Antes de reimprimir, verifica:

- [ ] Seleccioné el período correcto
- [ ] Ingresé el folio correcto
- [ ] El sistema encontró el marbete
- [ ] El estado es "IMPRESO"
- [ ] La información del marbete es correcta
- [ ] Hice click en "Reimprimir"
- [ ] Confirmé en el modal
- [ ] Se descargó el PDF
- [ ] Puedo reimprimir sin errores
- [ ] El formulario se limpió automáticamente

---

**Versión:** 2.0 (Actualizada 2026-02-23)  
**Compatible con:** Reimpresión Extraordinaria v2.0  
**Patrón:** Basado en ConteoMarbetes.vue

