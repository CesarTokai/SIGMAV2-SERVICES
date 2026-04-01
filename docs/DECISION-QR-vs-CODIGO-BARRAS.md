# ⚙️ QR vs CÓDIGO DE BARRAS - ANÁLISIS TÉCNICO PARA SIGMAV2

**Fecha:** 24 de Marzo 2026  
**Contexto:** Decisión para sistema de escaneo móvil en marbetes  

---

## 🎯 COMPARATIVA TÉCNICA

| Criterio | QR | Código de Barras |
|----------|----|----|
| **Capacidad de datos** | 2953 bytes (alfanumérico) | ~20 caracteres máximo |
| **Información por imagen** | TODO: folio, período, almacén, producto | Solo número secuencial |
| **Corrección de errores** | 30% (puede estar dañado 30%) | <10% (muy sensible) |
| **Lectura móvil** | Instantánea con cámara | Requiere escáner especializado |
| **Posicionamiento** | Flexible (cualquier ángulo) | Estricto (línea recta) |
| **Resiliencia** | Alta (redundancia integrada) | Baja (cualquier daño = no lee) |
| **Escalabilidad** | Ilimitada (cabe más info) | Limitada (máximo ~20 chars) |
| **Costo de impresión** | Mismo que código de barras | Mismo que QR |
| **Adopción móvil** | 99% de smartphones | <1% de smartphones |
| **Tamaño imagen** | ~2-3 cm x 2-3 cm | ~5 cm x 1 cm (para legibilidad) |

---

## 📊 CASO DE USO SIGMAV2

### OPCIÓN 1: Código de Barras (Simple pero limitado)

```
Marbete impreso:
┌──────────────────────┐
│ Folio: #42           │
│ Producto: Laptop     │
│ ║ ║ ║ ║ ║ ║ ║ ║      │ ← Código de barras: "42"
│ SIGMA-42             │
└──────────────────────┘

Flujo:
1. Escanear: "42"
2. GET /labels/by-folio/42
3. Backend retorna: folio, producto, almacén, etc.

Problema: ¿Y si el usuario escanea en otro almacén?
          ¿Y si hay 2 marbetes #42 en períodos diferentes?
          → Necesita parámetros adicionales (almacén, período)
```

### OPCIÓN 2: QR Completo (Recomendado) ✅

```
Marbete impreso:
┌──────────────────────┐
│ Folio: #42           │
│ Producto: Laptop     │
│ ┌────────────────┐   │
│ │ █ █ █ █ █ █   │   │
│ │ █ █ █ █ █ █   │   │ ← QR contiene TODO
│ │ █ █ █ █ █ █   │   │
│ └────────────────┘   │
│ SIGMAV2-42-P16-W369  │
└──────────────────────┘

QR codifica: "SIGMAV2|FOLIO:42|PERIOD:16|WAREHOUSE:369|PRODUCT:123"

Flujo:
1. Escanear → Flutter lee string completo
2. Extraer: folio=42, period=16, warehouse=369
3. GET /labels/by-folio/42?periodId=16&warehouseId=369
4. Backend retorna: TODO confirmado

Ventaja: QR ya dice dónde se escaneó → validación automática
```

---

## 🔍 ANÁLISIS DE RIESGOS

### Código de Barras: Riesgos

**Riesgo 1: Ambigüedad**
```
Problema: Folio #42 podría existir en múltiples períodos
Solución: Flutter debe preguntar período + almacén ANTES de escanear
         → Complejidad añadida

Código de barras solo retorna: "42"
¿De qué período? ¿De qué almacén?
```

**Riesgo 2: Daño físico**
```
Problema: Marbete maltratado en almacén
Efectos:
  - Código de barras ilegible → No se escanea
  - QR con 30% daño → Se sigue leyendo

Probabilidad en ambiente de conteo: ALTA (manipulación constante)
```

**Riesgo 3: Escalabilidad futura**
```
Si agregan campos futuros (lote, serie, etc.):
  - Código de barras → No cabe
  - QR → Fácil agregar info

Inflexible a cambios de negocio
```

---

### QR: Ventajas de seguridad

**Ventaja 1: Validación integrada**
```
QR contiene: FOLIO + PERIOD + WAREHOUSE

En Flutter:
  1. Escanear → Leer string completo
  2. Extraer parámetros
  3. Validar: ¿El período es el activo?
  4. Validar: ¿Es el almacén del usuario?
  
Previene errores humanos
```

**Ventaja 2: Auditoría completa**
```
En tabla label_count_events:
  - device_id: Qué móvil
  - scan_timestamp: Cuándo se escaneó
  - Datos en QR: Confirmación de contexto

Trazabilidad perfecta
```

**Ventaja 3: Desconexión de red**
```
QR contiene TODO lo necesario offline:
  - Flutter puede validar ANTES de enviar al servidor
  - Si falla conexión → Almacenar en caché local

Código de barras requiere consulta a BD para cada lectura
```

---

## 📱 IMPLEMENTACIÓN EN FLUTTER

### Con QR:

```dart
// 1. Escanear
String qrContent = await BarcodeScanner.scan();
// Resultado: "SIGMAV2|FOLIO:42|PERIOD:16|WAREHOUSE:369"

// 2. Parsear
Map<String, String> data = parseQr(qrContent);
int folio = int.parse(data['FOLIO']);
int period = int.parse(data['PERIOD']);
int warehouse = int.parse(data['WAREHOUSE']);

// 3. Validar en Flutter ANTES de servidor
if (warehouse != selectedWarehouse) {
  showError("Marbete pertenece a otro almacén");
  return;
}
if (period != activePeriod) {
  showError("Período no coincide");
  return;
}

// 4. Llamar API con parámetros ya validados
POST /labels/scan/count {
  folio: 42,
  periodId: 16,
  warehouseId: 369,
  ...
}
```

### Con Código de Barras:

```dart
// 1. Escanear
String barcode = await BarcodeScanner.scan();
// Resultado: "42"

// 2. Preguntar período + almacén ANTES
showDialog("¿Período? ¿Almacén?");
// Usuario selecciona manualmente → Complejidad

// 3. Llamar API
POST /labels/scan/count {
  folio: 42,
  periodId: 16,  // El usuario tuvo que seleccionar
  warehouseId: 369,  // Manualmente
  ...
}
```

---

## ✅ RECOMENDACIÓN FINAL: QR

**Por qué QR es la opción correcta:**

1. **Captura contexto completo** → Sin ambigüedad
2. **Resiliencia** → Funciona incluso dañado
3. **Experiencia móvil** → Punto y escanea
4. **Auditoría** → Todo documentado en QR
5. **Escalabilidad** → Fácil agregar campos
6. **Costo** → Mismo que código de barras
7. **Adopción** → 99% de móviles leen QR

**Desventajas de código de barras:**
- Ambigüedad (qué período, qué almacén)
- Frágil ante daño físico
- Requiere usuario seleccione parámetros
- Inflexible a cambios futuros

---

## 🏗️ FORMATO RECOMENDADO DEL QR

```
Formato: SIGMAV2|FOLIO:{folio}|PERIOD:{periodId}|WAREHOUSE:{warehouseId}|PRODUCT:{productId}

Ejemplo: SIGMAV2|FOLIO:42|PERIOD:16|WAREHOUSE:369|PRODUCT:123|HASH:abc123

Ventajas:
- Legible si falla parser (pipe-separated)
- Contiene HASH para validar integridad
- Fácil de parsear en Flutter
- Escalable (agregar más campos al final)
```

---

## 🛠️ PRÓXIMOS PASOS

1. ✅ Decidir formato del QR (arriba especificado)
2. ✅ Crear tabla de generación de QR con JasperReports
3. ✅ Usar este formato en nuevos marbetes
4. ✅ Implementar parser en Flutter
5. ✅ Validar contexto antes de llamar API

---

**Conclusión:** 🎯 **QR es obligatorio para este proyecto móvil.** Código de barras no tiene suficiente capacidad de datos ni resiliencia.


