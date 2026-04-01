# 🚀 COMANDOS CURL LISTOS PARA PROBAR - QR EN MARBETES

## 1️⃣ LOGIN (obtener token)

```bash
curl -X POST http://localhost:8080/api/sigmav2/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@tokai.com","password":"password123"}'
```

**Guardar el token:**
```bash
$TOKEN="tu_token_aqui"
```

---

## 2️⃣ IMPRIMIR TODOS LOS MARBETES CON QR

**Genera PDF con QR para TODOS los marbetes del período + almacén**

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/print-with-qr \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"periodId":1,"warehouseId":5}' \
  --output marbetes_con_qr.pdf
```

📌 Reemplaza:
- `1` = ID del período
- `5` = ID del almacén

---

## 3️⃣ IMPRIMIR MARBETES ESPECÍFICOS CON QR

**Genera PDF con QR para SOLO algunos marbetes**

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/print-specific-with-qr \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"folios":[42,43,44],"periodId":1,"warehouseId":5}' \
  --output marbetes_especificos.pdf
```

📌 Reemplaza:
- `[42,43,44]` = Números de folios a imprimir
- `1` = ID período
- `5` = ID almacén

---

## 4️⃣ VERIFICAR PERÍODOS (información previa)

```bash
curl -X GET http://localhost:8080/api/sigmav2/periods \
  -H "Authorization: Bearer $TOKEN"
```

---

## 5️⃣ VERIFICAR ALMACENES (información previa)

```bash
curl -X GET http://localhost:8080/api/sigmav2/warehouse \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📝 EJEMPLO COMPLETO (PowerShell Windows)

```powershell
# Definir variables
$API = "http://localhost:8080/api/sigmav2"
$EMAIL = "usuario@tokai.com"
$PASSWORD = "password123"

# 1. LOGIN
Write-Host "🔐 Autenticando..." -ForegroundColor Cyan
$login = Invoke-RestMethod -Uri "$API/auth/login" -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body "{`"email`":`"$EMAIL`",`"password`":`"$PASSWORD`"}"

$TOKEN = $login.data.token
Write-Host "✅ Token: $($TOKEN.Substring(0,20))..." -ForegroundColor Green

# 2. GENERAR PDF CON QR
Write-Host "📄 Generando PDF con QR..." -ForegroundColor Cyan
$response = Invoke-RestMethod -Uri "$API/labels/print-with-qr" -Method POST `
  -Headers @{"Authorization"="Bearer $TOKEN"; "Content-Type"="application/json"} `
  -Body '{"periodId":1,"warehouseId":5}' -OutFile "marbetes_qr.pdf"

Write-Host "✅ PDF generado: marbetes_qr.pdf" -ForegroundColor Green

# 3. ABRIR PDF
start marbetes_qr.pdf
```

---

## 📱 PRUEBA EN FLUTTER

1. Ejecutar app en emulador/dispositivo
2. Ir a pantalla "Scanner"
3. Apuntar cámara al QR en el PDF
4. App debe leer el número (42, 43, etc.)
5. Mostrar detalles del marbete
6. Permitir registrar conteo C1/C2

---

## ⚡ ATAJOS RÁPIDOS

**Token fijo (si no cambia):**
```bash
set TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Descargar PDF directamente:**
```bash
curl -o output.pdf "http://localhost:8080/api/sigmav2/labels/print-with-qr" ...
```

**Ver respuesta antes de descargar:**
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/sigmav2/periods
```

---

## 🔍 VALIDAR ANTES DE PROBAR

- ✅ Servidor corriendo: http://localhost:8080/swagger-ui.html
- ✅ MySQL conectado: base de datos `SIGMAV2_2`
- ✅ Usuario existe en tabla `users`
- ✅ Período activo en tabla `periods`
- ✅ Almacén existe en tabla `warehouse`
- ✅ Marbetes generados en tabla `labels`

---

**¡Listo! Copia y pega los comandos en tu terminal** 🚀

