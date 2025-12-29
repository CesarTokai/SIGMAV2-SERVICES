# Solución: Error "El conteo C2 ya fue registrado para este folio"

## Problema Identificado

Al intentar modificar un conteo C2 existente, se recibía el error:
```
DuplicateCountException: El conteo C2 ya fue registrado para este folio.
```

## Causa Raíz

Se estaba usando el **endpoint incorrecto**. Hay dos endpoints diferentes para conteos:

### 1. Registrar Conteo (Primera vez)
- **Método**: `POST`
- **Endpoint**: `/api/sigmav2/labels/counts/c2`
- **Propósito**: Crear el conteo C2 por primera vez
- **Validación**: Lanza error si ya existe C2

### 2. Actualizar Conteo (Modificar existente)
- **Método**: `PUT`
- **Endpoint**: `/api/sigmav2/labels/counts/c2`
- **Propósito**: Modificar un conteo C2 ya existente
- **Validación**: Lanza error si NO existe C2

## Solución Implementada

### 1. Actualización del Servicio (`LabelServiceImpl.java`)

**Archivo**: `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelServiceImpl.java`

**Método**: `updateCountC2()`

**Cambio**:
```java
// ANTES
boolean allowed = roleUpper.equals("ADMINISTRADOR") || roleUpper.equals("AUXILIAR_DE_CONTEO");

// AHORA
boolean allowed = roleUpper.equals("ADMINISTRADOR") ||
                 roleUpper.equals("ALMACENISTA") ||
                 roleUpper.equals("AUXILIAR_DE_CONTEO");
```

### 2. Actualización del Controlador (`LabelsController.java`)

**Archivo**: `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/adapter/controller/LabelsController.java`

**Endpoint**: `PUT /counts/c2`

**Cambio**:
```java
// ANTES
@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR_DE_CONTEO')")

// AHORA
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
```

## Roles con Permisos

| Acción | ADMINISTRADOR | ALMACENISTA | AUXILIAR | AUXILIAR_DE_CONTEO |
|--------|---------------|-------------|----------|-------------------|
| Registrar C1 | ✅ | ✅ | ✅ | ✅ |
| Registrar C2 | ✅ | ✅ | ✅ | ✅ |
| Actualizar C1 | ✅ | ✅ | ✅ | ✅ |
| Actualizar C2 | ✅ | ✅ | ❌ | ✅ |

## Cómo Usar Correctamente

### Frontend (Vue.js)

```javascript
// 1️⃣ REGISTRAR C2 por primera vez (POST)
const registrarC2 = async (folio, valor) => {
  try {
    const response = await api.post('/labels/counts/c2', {
      folio: folio,
      countedValue: valor
    });
    console.log('C2 registrado:', response.data);
  } catch (error) {
    if (error.response?.data?.error === 'DUPLICATE_COUNT') {
      // Ya existe, usar actualización en su lugar
      await actualizarC2(folio, valor);
    }
  }
};

// 2️⃣ ACTUALIZAR C2 existente (PUT)
const actualizarC2 = async (folio, valor) => {
  try {
    const response = await api.put('/labels/counts/c2', {
      folio: folio,
      countedValue: valor
    });
    console.log('C2 actualizado:', response.data);
  } catch (error) {
    console.error('Error al actualizar C2:', error.response?.data);
  }
};

// 3️⃣ FUNCIÓN INTELIGENTE (detecta automáticamente)
const guardarC2 = async (folio, valor) => {
  try {
    // Intentar registrar primero
    await registrarC2(folio, valor);
  } catch (error) {
    if (error.response?.status === 400 &&
        error.response?.data?.message?.includes('ya fue registrado')) {
      // Si ya existe, actualizar
      await actualizarC2(folio, valor);
    } else {
      throw error;
    }
  }
};
```

### Ejemplo con Axios

```javascript
// REGISTRAR (POST)
axios.post('http://localhost:8080/api/sigmav2/labels/counts/c2', {
  folio: 15,
  countedValue: 501
});

// ACTUALIZAR (PUT)
axios.put('http://localhost:8080/api/sigmav2/labels/counts/c2', {
  folio: 15,
  countedValue: 502
});
```

## Validaciones del Sistema

### Al Registrar C2:
1. ✅ Marbete debe existir
2. ✅ Marbete debe estar en estado `IMPRESO`
3. ✅ Marbete NO debe estar `CANCELADO`
4. ✅ Debe existir C1 previo
5. ✅ NO debe existir C2 (lanza `DuplicateCountException`)
6. ✅ Usuario debe tener acceso al almacén

### Al Actualizar C2:
1. ✅ Marbete debe existir
2. ✅ Marbete debe estar en estado `IMPRESO`
3. ✅ Marbete NO debe estar `CANCELADO`
4. ✅ DEBE existir C2 previo (lanza `LabelNotFoundException`)
5. ✅ Usuario debe tener rol permitido
6. ✅ Usuario debe tener acceso al almacén

## Próximos Pasos

1. **Reiniciar la aplicación Spring Boot**
   ```bash
   mvn spring-boot:run
   ```

2. **Verificar en el frontend** que se está usando `PUT` en lugar de `POST`

3. **Probar con usuario ADMINISTRADOR** la actualización de C2

## Archivos Modificados

- ✅ `LabelServiceImpl.java` - Método `updateCountC2()`
- ✅ `LabelsController.java` - Endpoint `PUT /counts/c2`
- ✅ Compilación exitosa

---

**Fecha**: 2025-12-18
**Estado**: ✅ Resuelto

