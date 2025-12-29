# Validaciones en Registro de Conteos - Marbetes Sin Existencias

## ❓ Pregunta
¿Existe alguna validación que al ingresar los conteos no me deje ingresar conteos en marbetes que no tienen existencias?

## ✅ Respuesta Directa

**NO**, actualmente **NO existe ninguna validación** que impida registrar o actualizar conteos en marbetes de productos sin existencias.

---

## 🔍 Validaciones Actuales en Registro de Conteos

### 1️⃣ Registro de Conteo C1 (`registerCountC1`)

**Archivo**: `LabelServiceImpl.java` - Líneas 483-522

#### Validaciones Implementadas:

```java
✅ 1. Validación de ROL
   - Roles permitidos: ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO

✅ 2. Validación de EXISTENCIA del marbete
   - El folio debe existir en la tabla labels

✅ 3. Validación de ACCESO al almacén
   - El usuario debe tener acceso al almacén del marbete

✅ 4. Validación de ESTADO del marbete
   - NO debe estar CANCELADO
   - DEBE estar IMPRESO

✅ 5. Validación de DUPLICIDAD
   - NO debe existir ya un C1 registrado
   - NO debe existir ya un C2 (secuencia rota)

❌ NO VALIDA: Existencias del producto
```

#### Código Actual:

```java
public LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole) {
    // Validación de rol
    String roleUpper = userRole.toUpperCase();
    boolean allowed = roleUpper.equals("ADMINISTRADOR") ||
                     roleUpper.equals("ALMACENISTA") ||
                     roleUpper.equals("AUXILIAR") ||
                     roleUpper.equals("AUXILIAR_DE_CONTEO");

    // Verificar que el marbete exista
    Optional<Label> optLabel = persistence.findByFolio(dto.getFolio());
    if (optLabel.isEmpty()) {
        throw new LabelNotFoundException("El folio no existe");
    }
    Label label = optLabel.get();

    // Validar acceso al almacén
    warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

    // Validar estado
    if (label.getEstado() == Label.State.CANCELADO) {
        throw new InvalidLabelStateException("No se puede registrar conteo: el marbete está CANCELADO.");
    }
    if (label.getEstado() != Label.State.IMPRESO) {
        throw new InvalidLabelStateException("No se puede registrar conteo: el marbete no está IMPRESO.");
    }

    // ❌ NO HAY VALIDACIÓN DE EXISTENCIAS AQUÍ

    // Validar duplicidad
    if (persistence.hasCountNumber(dto.getFolio(), 1)) {
        throw new DuplicateCountException("El conteo C1 ya fue registrado para este folio.");
    }

    // Guardar conteo
    return persistence.saveCountEvent(dto.getFolio(), userId, 1, dto.getCountedValue(), roleEnum, false);
}
```

---

### 2️⃣ Registro de Conteo C2 (`registerCountC2`)

**Archivo**: `LabelServiceImpl.java` - Líneas 527-570

#### Validaciones Implementadas:

```java
✅ 1. Validación de ROL
   - Roles permitidos: ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO

✅ 2. Validación de EXISTENCIA del marbete
   - El folio debe existir en la tabla labels

✅ 3. Validación de ACCESO al almacén
   - El usuario debe tener acceso al almacén del marbete

✅ 4. Validación de ESTADO del marbete
   - NO debe estar CANCELADO
   - DEBE estar IMPRESO

✅ 5. Validación de SECUENCIA
   - DEBE existir C1 previo
   - NO debe existir ya un C2 registrado

❌ NO VALIDA: Existencias del producto
```

---

### 3️⃣ Actualización de Conteo C1 (`updateCountC1`)

**Archivo**: `LabelServiceImpl.java` - Líneas 573-620

#### Validaciones Implementadas:

```java
✅ 1. Validación de ROL
✅ 2. Validación de EXISTENCIA del marbete
✅ 3. Validación de ACCESO al almacén
✅ 4. Validación de ESTADO (IMPRESO, no CANCELADO)
✅ 5. Validación de que EXISTA C1 previo

❌ NO VALIDA: Existencias del producto
```

---

### 4️⃣ Actualización de Conteo C2 (`updateCountC2`)

**Archivo**: `LabelServiceImpl.java` - Líneas 623-670

#### Validaciones Implementadas:

```java
✅ 1. Validación de ROL (ADMINISTRADOR, ALMACENISTA, AUXILIAR_DE_CONTEO)
✅ 2. Validación de EXISTENCIA del marbete
✅ 3. Validación de ACCESO al almacén
✅ 4. Validación de ESTADO (IMPRESO, no CANCELADO)
✅ 5. Validación de que EXISTA C2 previo

❌ NO VALIDA: Existencias del producto
```

---

## 📊 Resumen de Validaciones

| Validación | C1 Registrar | C2 Registrar | C1 Actualizar | C2 Actualizar |
|------------|--------------|--------------|---------------|---------------|
| Rol de usuario | ✅ | ✅ | ✅ | ✅ |
| Marbete existe | ✅ | ✅ | ✅ | ✅ |
| Acceso almacén | ✅ | ✅ | ✅ | ✅ |
| Estado IMPRESO | ✅ | ✅ | ✅ | ✅ |
| No CANCELADO | ✅ | ✅ | ✅ | ✅ |
| Secuencia (C1→C2) | ✅ | ✅ | ✅ | ✅ |
| No duplicar | ✅ | ✅ | N/A | N/A |
| **Existencias > 0** | ❌ | ❌ | ❌ | ❌ |

---

## 🎯 Flujo Completo: Conteo de Marbetes

```
┌─────────────────────────────────────────────────────────────┐
│  MARBETE IMPRESO (con o sin existencias)                    │
│  Estado: IMPRESO                                            │
│  Existencias: Puede ser 0 o cualquier valor                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  1️⃣ REGISTRAR CONTEO C1                                     │
│  Endpoint: POST /labels/counts/c1                           │
│                                                             │
│  Validaciones:                                              │
│  ✅ Folio existe                                             │
│  ✅ Estado = IMPRESO                                         │
│  ✅ No cancelado                                             │
│  ✅ No existe C1 previo                                      │
│  ❌ NO valida existencias                                    │
│                                                             │
│  Permite: Registrar C1 incluso si existencias = 0           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  2️⃣ REGISTRAR CONTEO C2                                     │
│  Endpoint: POST /labels/counts/c2                           │
│                                                             │
│  Validaciones:                                              │
│  ✅ Folio existe                                             │
│  ✅ Estado = IMPRESO                                         │
│  ✅ No cancelado                                             │
│  ✅ Existe C1 previo                                         │
│  ✅ No existe C2 previo                                      │
│  ❌ NO valida existencias                                    │
│                                                             │
│  Permite: Registrar C2 incluso si existencias = 0           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  3️⃣ ACTUALIZAR CONTEOS (Opcional)                           │
│  Endpoints: PUT /labels/counts/c1 o /counts/c2              │
│                                                             │
│  Validaciones:                                              │
│  ✅ Folio existe                                             │
│  ✅ Estado = IMPRESO                                         │
│  ✅ No cancelado                                             │
│  ✅ Existe conteo previo (C1 o C2)                           │
│  ❌ NO valida existencias                                    │
│                                                             │
│  Permite: Actualizar incluso si existencias = 0             │
└─────────────────────────────────────────────────────────────┘
```

---

## 💡 ¿Por Qué NO Se Validan Existencias?

### Razón de Negocio:

El sistema permite registrar conteos en marbetes sin existencias porque:

1. **Conteo Físico vs. Sistema**: El conteo físico puede encontrar productos que el sistema cree que no existen
2. **Detección de Discrepancias**: Permite identificar productos "fantasma" o errores de registro
3. **Inventario Completo**: El inventario físico debe incluir TODO, incluso productos con existencias teóricas = 0
4. **Ajustes de Inventario**: Los conteos generan diferencias que se usan para ajustar el sistema

### Ejemplo Real:

```
Producto: X-TARIMAS
Existencias Teóricas (Sistema): 0
Conteo Físico C1: 5 unidades encontradas
Conteo Físico C2: 5 unidades confirmadas
Diferencia: +5 (se encontraron productos no registrados)
```

Si se bloqueara el conteo por falta de existencias, **no podrías registrar estas discrepancias**.

---

## 🔧 Si Quieres Agregar Validación de Existencias

### ⚠️ NO RECOMENDADO (rompe lógica de negocio)

Si aún así quieres validar existencias, aquí está el código:

#### Modificar `registerCountC1`:

```java
@Override
@Transactional
public LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole) {
    // ...validaciones existentes...

    Label label = optLabel.get();

    // NUEVA VALIDACIÓN: Verificar existencias
    try {
        var stockOpt = inventoryStockRepository
            .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                label.getProductId(), label.getWarehouseId(), label.getPeriodId());

        if (stockOpt.isPresent()) {
            java.math.BigDecimal existencias = stockOpt.get().getExistQty();
            if (existencias == null || existencias.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new InvalidLabelStateException(
                    "No se puede registrar conteo: el producto no tiene existencias teóricas");
            }
        } else {
            throw new InvalidLabelStateException(
                "No se puede registrar conteo: no hay registro de existencias para este producto");
        }
    } catch (Exception e) {
        log.warn("Error al verificar existencias: {}", e.getMessage());
        throw new InvalidLabelStateException(
            "No se puede registrar conteo: error al verificar existencias");
    }

    // ...resto del código...
}
```

### ⚠️ Consecuencias de Agregar Esta Validación:

1. ❌ No podrás registrar conteos de productos con existencias = 0
2. ❌ No podrás detectar discrepancias positivas
3. ❌ Rompe el flujo de inventario cíclico
4. ❌ Usuarios tendrán que cancelar marbetes ya impresos

---

## 📋 Conclusión

### ✅ Estado Actual:
- **NO existe validación** de existencias al registrar conteos
- Esto es **CORRECTO** según mejores prácticas de inventario físico
- Permite detectar discrepancias en ambas direcciones (+ y -)

### 🎯 Recomendación:
- **NO agregar** validación de existencias
- Mantener el comportamiento actual
- Los conteos deben reflejar la realidad física, no las existencias teóricas

### 🔍 Si Necesitas Filtrar en Frontend:
En lugar de bloquear en backend, puedes:
1. Mostrar advertencia en UI cuando existencias = 0
2. Resaltar marbetes sin existencias con color diferente
3. Permitir al usuario decidir si continuar o no

---

**Fecha**: 2025-12-18
**Archivos Analizados**:
- `LabelServiceImpl.java` (2118 líneas)
- Métodos: `registerCountC1`, `registerCountC2`, `updateCountC1`, `updateCountC2`

**Conclusión Final**: El sistema está diseñado correctamente. NO debe validar existencias al registrar conteos.

