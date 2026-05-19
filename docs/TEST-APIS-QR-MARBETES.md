# 🧪 APIs para Pruebas - Generación de QR en Marbetes

**Versión:** 1.0.0  
**Fecha:** 2026-03-24  
**Base URL:** `http://localhost:8080/api/sigmav2`

---

## 📋 Requisitos Previos

1. **Servidor ejecutándose:** `http://localhost:8080`
2. **Token JWT válido** - Obtener del login
3. **Período activo** - Tener al menos 1 período creado
4. **Almacén asignado** - Usuario debe tener acceso a un almacén
5. **Marbetes generados** - Marbetes en estado GENERADO para el período/almacén

---

## 🔐 1. OBTENER TOKEN (Prerequisito)

### **POST /auth/login**

```bash
curl -X POST http://localhost:8080/api/sigmav2/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@tokai.com",
    "password": "Tu_Contraseña_123"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "email": "usuario@tokai.com",
      "nombreCompleto": "Juan Pérez",
      "role": "ALMACENISTA",
      "almacen": "ALMACEN_A"
    },
    "expiresIn": 3600
  }
}
```

**Guardar el token en una variable:**
```bash
$TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 🎯 2. IMPRIMIR MARBETES CON QR

### **POST /labels/print-with-qr** ⭐ PRINCIPAL

Genera PDF con TODOS los marbetes de un período + almacén, incluyendo QR incrustados.

**Request:**
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/print-with-qr \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "periodId": 1,
    "warehouseId": 5
  }' \
  --output marbetes_con_qr.pdf
```

**Request Body:**
```json
{
  "periodId": 1,
  "warehouseId": 5
}
```

**Response:** 
- **200 OK** → Descarga archivo PDF con QRs
- **404 Not Found** → No hay marbetes en ese período/almacén
- **500 Internal Server Error** → Error al generar PDF

**Descripción:**
- 📄 Genera PDF con todos los marbetes del período 1, almacén 5
- 📱 Cada marbete tiene su QR incrustado en el PDF
- 🎯 El QR contiene el número del marbete (folio)
- 💾 Descarga automáticamente como `marbetes_con_qr.pdf`

---

## 🎯 3. IMPRIMIR MARBETES ESPECÍFICOS CON QR

### **POST /labels/print-specific-with-qr** ⭐ PARA REIMPRESIÓN

Genera PDF CON SOLO los marbetes que especifiques, con QR incluidos.

**Request:**
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/print-specific-with-qr \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "folios": [42, 43, 44, 45],
    "periodId": 1,
    "warehouseId": 5
  }' \
  --output marbetes_especificos_qr.pdf
```

**Request Body:**
```json
{
  "folios": [42, 43, 44, 45],
  "periodId": 1,
  "warehouseId": 5
}
```

**Response:**
- **200 OK** → PDF con esos 4 marbetes específicos + QR
- **404 Not Found** → No se encontraron esos folios
- **400 Bad Request** → Formato incorrecto

**Descripción:**
- 📄 Genera PDF SOLO con folios: 42, 43, 44, 45
- 📱 Perfecto para reimpresiones o marbetes perdidos
- 🎯 Cada QR escanea solo esos números
- 💾 Descarga como `marbetes_especificos_qr.pdf`

---

## 📊 FLUJO COMPLETO DE PRUEBA

### Paso 1: Login y obtener token
```bash
curl -X POST http://localhost:8080/api/sigmav2/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@tokai.com", "password":"password"}' \
  | jq '.data.token' -r > token.txt

$TOKEN = Get-Content token.txt
```

### Paso 2: Verificar períodos
```bash
curl -X GET http://localhost:8080/api/sigmav2/periods \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Paso 3: Verificar almacenes
```bash
curl -X GET http://localhost:8080/api/sigmav2/warehouse \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Paso 4: Imprimir marbetes CON QR
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/print-with-qr \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"periodId":1, "warehouseId":5}' \
  --output PDF_con_qr.pdf

# Abrir el PDF
start PDF_con_qr.pdf
```

### Paso 5: Escanear QR con móvil
- Abrir app Flutter en dispositivo/emulador
- Ir a pantalla de Scanner
- Apuntar cámara al QR del PDF
- Debe leer el número (ej: "42")
- APP llama GET `/labels/by-folio/42`
- Muestra detalles del marbete

---

## 🔑 PARÁMETROS REQUERIDOS

| Endpoint | Método | Params | Descripción |
|----------|--------|--------|-------------|
| `/print-with-qr` | POST | `periodId`, `warehouseId` | Todos los marbetes |
| `/print-specific-with-qr` | POST | `folios[]`, `periodId`, `warehouseId` | Marbetes específicos |

---

## ⚠️ CÓDIGOS DE ERROR

| Código | Significado | Solución |
|--------|------------|----------|
| **200** | ✅ Éxito | Descarga PDF |
| **400** | Datos inválidos | Revisa JSON |
| **401** | Token expirado | Haz login de nuevo |
| **403** | Sin permisos | Usuario sin acceso |
| **404** | No hay marbetes | Crea marbetes primero |
| **500** | Error servidor | Revisa logs |

---

## 🔍 VALIDACIONES IMPORTANTES

✅ **Antes de probar:**
- ☑️ Servidor corriendo: `.\mvnw.cmd spring-boot:run`
- ☑️ BD conectada: `mysql -u root -p SIGMAV2_2`
- ☑️ Usuario existe: tabla `users`
- ☑️ Período existe: tabla `periods` (activo)
- ☑️ Almacén existe: tabla `warehouse`
- ☑️ Marbetes generados: tabla `labels` (estado = GENERADO)
- ☑️ Plantilla existe: `src/main/resources/reports/marbete_qr.jrxml`

---

## 🧪 SCRIPT COMPLETO (PowerShell)

```powershell
# ═════════════════════════════════════════════════════
# SCRIPT PARA PROBAR GENERACIÓN DE QR EN MARBETES
# ═════════════════════════════════════════════════════

$API_URL = "http://localhost:8080/api/sigmav2"
$EMAIL = "usuario@tokai.com"
$PASSWORD = "Tu_Contraseña_123"
$PERIOD_ID = 1
$WAREHOUSE_ID = 5

Write-Host "🔐 Autenticando..." -ForegroundColor Cyan

# 1. LOGIN
$loginResponse = curl -s -X POST "$API_URL/auth/login" `
  -H "Content-Type: application/json" `
  -d "{`"email`":`"$EMAIL`", `"password`":`"$PASSWORD`"}" | ConvertFrom-Json

if ($null -eq $loginResponse.data.token) {
    Write-Host "❌ Error de autenticación" -ForegroundColor Red
    exit 1
}

$TOKEN = $loginResponse.data.token
Write-Host "✅ Token obtenido: $($TOKEN.Substring(0, 20))..." -ForegroundColor Green

# 2. IMPRIMIR TODOS LOS MARBETES CON QR
Write-Host "`n📄 Generando PDF con QR para período=$PERIOD_ID, almacén=$WAREHOUSE_ID..." -ForegroundColor Cyan

curl -s -X POST "$API_URL/labels/print-with-qr" `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: application/json" `
  -d "{`"periodId`":$PERIOD_ID, `"warehouseId`":$WAREHOUSE_ID}" `
  --output "marbetes_con_qr_$(Get-Date -Format 'yyyyMMdd_HHmmss').pdf"

Write-Host "✅ PDF generado exitosamente" -ForegroundColor Green
Write-Host "📂 Archivo: marbetes_con_qr_*.pdf" -ForegroundColor Yellow

# 3. IMPRIMIR MARBETES ESPECÍFICOS
Write-Host "`n📄 Generando PDF con marbetes específicos..." -ForegroundColor Cyan

curl -s -X POST "$API_URL/labels/print-specific-with-qr" `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: application/json" `
  -d "{`"folios`":[42, 43, 44], `"periodId`":$PERIOD_ID, `"warehouseId`":$WAREHOUSE_ID}" `
  --output "marbetes_especificos_qr_$(Get-Date -Format 'yyyyMMdd_HHmmss').pdf"

Write-Host "✅ PDF específico generado" -ForegroundColor Green

# 4. ABRIR PDFS
Write-Host "`n📂 Abriendo archivos..." -ForegroundColor Cyan
Get-ChildItem -Filter "marbetes_*.pdf" | ForEach-Object {
    Start-Process $_.FullName
}

Write-Host "`n✅ Prueba completada!" -ForegroundColor Green
Write-Host "📱 Escanea los QRs con la app Flutter" -ForegroundColor Yellow
```

---

## 📱 PRUEBA CON FLUTTER

### Paso 1: Abrir app Flutter en emulador/dispositivo

### Paso 2: Ir a pantalla de Scanner

### Paso 3: Apuntar cámara al QR en el PDF

### Paso 4: App debe:
1. ✅ Leer número: "42"
2. ✅ Llamar: GET `/labels/by-folio/42`
3. ✅ Mostrar detalles del marbete
4. ✅ Permitir registrar conteo C1

---

## 📊 MONITOREO

### Ver logs en tiempo real:
```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--logging.level.tokai=DEBUG"
```

### Buscar en logs:
```
🎯 /print-with-qr
✅ PDF con QR generado
❌ Error al generar PDF
```

---

## ✅ CHECKLIST DE ÉXITO

- ☑️ Login exitoso (token obtenido)
- ☑️ PDF descargado (sin errores 500)
- ☑️ QR visible en el PDF
- ☑️ QR scaneable con cámara
- ☑️ Flutter lee el número correctamente
- ☑️ API retorna detalles del marbete
- ☑️ Pantalla muestra producto, stock, almacén

---

**¡Listo para probar! 🚀**

