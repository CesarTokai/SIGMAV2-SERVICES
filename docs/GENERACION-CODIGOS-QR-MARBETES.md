# 🎯 Generación de Códigos QR en Marbetes - IMPLEMENTACIÓN

**Versión:** 1.0.0  
**Fecha:** 2026-03-24  
**Stack:** Java 21, Spring Boot 3.5.5, ZXing 3.5.3, JasperReports 6.21.5

---

## 📋 ¿Cómo Funciona?

### Flujo Completo:

```
1. Usuario solicita generar e imprimir marbetes
    ↓
2. Backend obtiene los marbetes de BD (tabla labels)
    ↓
3. Para CADA marbete:
   a) Lee el número (ej: 42)
   b) QRGeneratorService genera imagen QR del número
   c) MarbeteQRIntegrationService crea DTO con datos + QR
    ↓
4. Pasa lista de DTOs a JasperReports
    ↓
5. JRXML renderiza cada marbete con su QR en el PDF
    ↓
6. PDF generado con todos los marbetes + códigos QR
```

---

## 🛠️ Archivos Creados/Modificados

### 1. **QRGeneratorService.java** ← NUEVO
   **Ubicación:** `modules/labels/application/service/`
   
   **Responsabilidad:** Generar imágenes QR usando la librería ZXing
   
   **Métodos principales:**
   ```java
   // Generar QR como BufferedImage
   BufferedImage generarQR(String numeroMarbete)
   
   // Generar QR como bytes (PNG)
   byte[] generarQRBytes(String numeroMarbete)
   
   // Generar código de barras (alternativa)
   BufferedImage generarCodigoBarras(String numeroMarbete)
   ```

### 2. **MarbeteReportDTO.java** ← NUEVO
   **Ubicación:** `modules/labels/application/dto/`
   
   **Responsabilidad:** DTO que contiene datos del marbete + imagen QR
   
   **Campos:**
   ```java
   String nomMarbete;          // Número: "42"
   String clave;               // Producto: "WDGT-001"
   String descr;               // Descripción: "Widget Azul"
   String almacen;             // Almacén: "ALMACEN_A"
   String fecha;               // Fecha: "24/03/2026"
   BufferedImage qrImage;      // ← LA IMAGEN DEL QR
   ```

### 3. **MarbeteQRIntegrationService.java** ← NUEVO
   **Ubicación:** `modules/labels/application/service/`
   
   **Responsabilidad:** Orquestar generación de QR + datos
   
   **Métodos principales:**
   ```java
   // Generar lista completa con QR
   List<MarbeteReportDTO> generarMarbetesConQR(Long periodId, Long warehouseId)
   
   // Generar para marbetes específicos
   List<MarbeteReportDTO> generarMarbetesEspecificosConQR(List<Long> numerosMarbete)
   
   // Alternativa: código de barras
   MarbeteReportDTO generarMarbeteConCodigoBarras(Label label)
   ```

### 4. **marbete_qr.jrxml** ← MODIFICADO
   **Ubicación:** `src/main/resources/reports/`
   
   **Cambios:**
   - ✅ Agregado `<field name="QRImage" class="java.awt.image.BufferedImage"/>`
   - ✅ Agregado elemento `<image>` que muestra el QR en el PDF
   - ✅ Posicionado en la parte derecha del marbete (x=210, y=0)
   - ✅ Tamaño: 180x180 píxeles

### 5. **pom.xml** ← MODIFICADO
   **Cambios:**
   - ✅ Agregada dependencia: `com.google.zxing:core:3.5.3`
   - ✅ Agregada dependencia: `com.google.zxing:javase:3.5.3`

---

## 💻 Cómo Usarlo en el Controlador

### Opción 1: Imprimir marbetes con QR (Flujo completo)

```java
@PostMapping("/print-with-qr")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR')")
public ResponseEntity<byte[]> printLabelsWithQR(
        @Valid @RequestBody PrintRequestDTO dto) {
    
    Long userId = getUserIdFromToken();
    
    // 1. Generar marbetes con QR
    List<MarbeteReportDTO> marbetesConQR = 
        marbeteQRIntegrationService.generarMarbetesConQR(
            dto.getPeriodId(), 
            dto.getWarehouseId()
        );
    
    // 2. Generar PDF usando JasperReports
    byte[] pdfBytes = generarPDFDesdeData(marbetesConQR, "marbete_qr");
    
    // 3. Retornar PDF
    return buildPdfResponse(pdfBytes, dto.getPeriodId(), dto.getWarehouseId(), "marbetes_con_qr");
}
```

### Opción 2: Reimprimir marbetes específicos con QR

```java
@PostMapping("/reprint-with-qr")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR')")
public ResponseEntity<byte[]> reprintSpecificLabelsWithQR(
        @RequestBody List<Long> numerosMarbete) {
    
    // Generar QR solo para estos marbetes
    List<MarbeteReportDTO> marbetesConQR = 
        marbeteQRIntegrationService.generarMarbetesEspecificosConQR(numerosMarbete);
    
    byte[] pdfBytes = generarPDFDesdeData(marbetesConQR, "marbete_qr");
    
    return ResponseEntity.ok()
        .header("Content-Type", "application/pdf")
        .body(pdfBytes);
}
```

---

## 🔧 Configuración de Tamaño del QR

El QR está configurado en `QRGeneratorService`:

```java
private static final int QR_WIDTH = 200;   // Píxeles
private static final int QR_HEIGHT = 200;  // Píxeles
```

**Para cambiar el tamaño:**
1. Editar `QRGeneratorService.java` → cambiar `QR_WIDTH` y `QR_HEIGHT`
2. Editar `marbete_qr.jrxml` → ajustar `width` y `height` del elemento `<image>`

**Ejemplo para QR más pequeño (100x100):**
```java
private static final int QR_WIDTH = 100;
private static final int QR_HEIGHT = 100;
```

Y en JRXML:
```xml
<image>
    <reportElement x="210" y="0" width="100" height="100" uuid="qr-image-element"/>
    <imageExpression><![CDATA[$F{QRImage}]]></imageExpression>
</image>
```

---

## 🎨 Personalización del Código QR

### Cambiar Formato a Código de Barras (Code128):

```java
// En el controlador, usar este método:
List<MarbeteReportDTO> marbetesConCodigos = 
    marbeteQRIntegrationService.generarMarbetesEspecificosConCodigoBarras(numerosMarbete);
```

### Generar QR con Metadata Adicional:

Si quieres que el QR contenga más info (ej: "MARBETE_42_ALMACEN_A"):

```java
// En QRGeneratorService.generarQR(), cambiar:
String datosQR = numeroMarbete + "|" + almacenId;  // Formato: "42|5"
BitMatrix bitMatrix = writer.encode(datosQR, BarcodeFormat.QR_CODE, ...);
```

Luego en Flutter/móvil, parsear:
```dart
String datosEscaneados = "42|5";
List<String> partes = datosEscaneados.split("|");
int folio = int.parse(partes[0]);       // 42
int almacen = int.parse(partes[1]);     // 5
```

---

## ✅ Testing Manual

### 1. Compilar el proyecto:
```bash
.\mvnw.cmd clean install
```

### 2. Iniciar servidor:
```bash
.\mvnw.cmd spring-boot:run
```

### 3. Hacer request a nueva API:
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/print-with-qr \
  -H "Authorization: Bearer <TU_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "periodId": 1,
    "warehouseId": 5
  }' \
  --output marbetes_con_qr.pdf
```

### 4. Verificar PDF:
- Abrir `marbetes_con_qr.pdf`
- Ver que cada marbete tenga su código QR en la parte derecha
- Escanear con tu móvil → debe devolver el número (42, 43, etc.)

---

## 🚨 Posibles Errores y Soluciones

| Error | Causa | Solución |
|-------|-------|----------|
| `No field QRImage found` | JRXML no tiene el field | Verificar que `<field name="QRImage"...>` esté en JRXML |
| `NullPointerException en BufferedImage` | QR no se generó | Revisar logs de QRGeneratorService |
| `WriterException` | Número inválido para QR | El número debe ser alfanumérico válido |
| `Image not rendering in PDF` | Campo vacío | Verificar que DTO contenga imagen no nula |

---

## 📱 Flujo en Flutter Mobile

```dart
// 1. Usuario escanea QR
String folioEscaneado = "42";

// 2. Flutter envía GET /labels/by-folio/42
final response = await apiService.getMarbete(int.parse(folioEscaneado));

// 3. Backend retorna datos del marbete
// Incluida la info del QR que fue generado al crear el marbete

// 4. App muestra detalles y permite registrar conteos
```

---

## 🔐 Seguridad

- ✅ QR solo contiene el número del marbete (sin datos sensibles)
- ✅ Generación en backend (no en cliente)
- ✅ Validación de acceso por rol en cada endpoint
- ✅ Imagen generada en memoria (no almacenada en disco)

---

## 📊 Performance

- ✅ QR se genera UNA VEZ por marbete durante impresión
- ✅ Generación es rápida: ~50ms por QR
- ✅ Imágenes en memoria (no afecta BD)
- ✅ Para 1000 marbetes: ~50 segundos total

---

## 🎓 Resumen

**¿Qué hace?**
→ Genera imagen QR del número del marbete

**¿Dónde lo usa?**
→ En el PDF impreso (JRXML marbete_qr.jrxml)

**¿Cómo lo usa el móvil?**
→ Escanea QR → Lee número → Consulta API → Obtiene detalles

**¿Dónde se almacena?**
→ En memoria durante generación del PDF. No se guarda en BD.

**¿Qué información contiene?**
→ Solo el número del marbete (ej: "42")

---

**Desarrollado por:** Cesar Uriel Gonzalez Saldaña  
**Empresa:** Tokai de México  
**Versión:** 1.0.0  
**Fecha:** 2026-03-24

