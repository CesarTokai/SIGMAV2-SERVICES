# 🐛 PROBLEMA: PDF de Marbetes CON QR No se Visualiza

## Síntomas Reportados
- ✅ Los QR se generan correctamente (logs muestran 43 imágenes generadas)
- ✅ El PDF se retorna (382KB)
- ❌ **El PDF no se visualiza ni se abre** (está corrupto o vacío)

## Causas Identificadas

### 1. **Compresión PDF Problèmatica**
```java
config.setCompressed(true);  // ❌ Puede corromper si hay datos nulos
```
Al comprimir, los datos null o BufferedImage pueden no serializarse correctamente, resultando en un PDF corrupto.

### 2. **Falta de Validación de Datos**
Los `MarbeteReportDTO` pueden tener campos nulos:
- `qrImage1`, `qrImage2`, `qrImage3` podrían ser `null`
- La plantilla JRXML referencia `$F{qrImage1}` sin validación
- Campos de texto vacíos (`nomMarbete`, `clave`, etc.)

### 3. **Falta de Logs Detallados**
Sin logs intermedios, es imposible saber en qué punto exacto falla la generación.

## Soluciones Implementadas

### ✅ **Desactivar Compresión**
```java
config.setCompressed(false);  // Temporal para debugging
```
**Nota:** Esto genera PDFs más grandes pero evita corrupción por compresión con datos problemáticos.

### ✅ **Agregar Validación de Datos**
```java
if (marbetes == null || marbetes.isEmpty()) {
    log.warn("⚠️ Lista de marbetes vacía o null");
    return new byte[0];
}

// Validación de cada marbete
for (int i = 0; i < marbetes.size(); i++) {
    MarbeteReportDTO dto = marbetes.get(i);
    log.debug("Marbete grupo {}: nom1={}, clave1={}, qr1={}", 
        i, dto.getNomMarbete1(), dto.getClave1(), dto.getQrImage1() != null ? "✅" : "❌");
}
```

### ✅ **Mejorar Logging**
Ahora se registran:
- Cantidad de páginas generadas
- Validación de cada grupo de marbetes
- Tamaño final del PDF
- Detección de PDF vacío

### ✅ **Validar Plantilla**
```java
if (jasperReport == null) {
    throw new RuntimeException("No se pudo cargar la plantilla de jasper");
}
```

## Pasos Siguientes

Después de estos cambios, revisa los logs para ver:
1. **¿Se generan todas las imágenes QR?** (`Generando QR para marbete: X`)
2. **¿Los datos están presentes en el DTO?** (campos nom, clave, almacén)
3. **¿El PDF tiene páginas?** (`Reporte llenado: X páginas`)
4. **¿El PDF final tiene contenido?** (`PDF con QR generado: X bytes`)

## Propuesta: Alternativa Si Sigue Fallando

Si el PDF sigue vacío después de estos cambios, el problema está en la **plantilla JRXML**:

```xml
<!-- Verificar que el band tiene altura -->
<band height="328" splitType="Stretch">
    ...
</band>

<!-- Verificar que hay al menos un textField o rectangle -->
<rectangle>
    <reportElement x="28" y="0" width="150" height="224" uuid="..."/>
</rectangle>
```

**Solución alternativa:**
- Generar PDF con estructura simple (sin QR) primero
- Verificar que funciona
- Luego agregar QR gradualmente

## Testing Recomendado

1. Recompila el proyecto:
   ```bash
   mvn clean compile
   ```

2. Ejecuta la API:
   ```bash
   POST /api/sigmav2/labels/print-with-qr
   Body: { "periodId": 1, "warehouseId": 8 }
   ```

3. Revisa los logs completos (especialmente los nuevos logs de validación)

4. Intenta abrir el PDF en diferentes lectores:
   - Adobe Reader
   - Chrome (visor integrado)
   - Windows Preview

## Archivos Modificados

- `LabelsController.java` → Método `generarPDFConQR()` mejorado

---
**Fecha:** 2026-04-07  
**Estado:** En debugging

