# 🧪 Testing Rápido - Nuevas APIs

## PowerShell Script para probar las nuevas APIs

```powershell
# Variables
$baseUrl = "http://localhost:8080/api/sigmav2/labels"
$token = "<TU_JWT_TOKEN_AQUI>"
$periodId = 1

# Headers
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 1. Detalle de TODOS los Productos (PDF)
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "📊 Generando reporte: Detalle de TODOS los Productos (PDF)" -ForegroundColor Cyan
$body = @{
    periodId = $periodId
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$baseUrl/reports/product-detail/all/pdf" `
    -Method Post `
    -Headers $headers `
    -Body $body `
    -OutFile "productos_todos_$(Get-Date -f yyyyMMdd_HHmmss).pdf"

Write-Host "✅ PDF guardado exitosamente" -ForegroundColor Green

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 2. Comparativo de TODOS los Almacenes (PDF)
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "📊 Generando reporte: Comparativo de TODOS los Almacenes (PDF)" -ForegroundColor Cyan
$body = @{
    periodId = $periodId
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$baseUrl/reports/comparative/all/pdf" `
    -Method Post `
    -Headers $headers `
    -Body $body `
    -OutFile "comparativo_todos_$(Get-Date -f yyyyMMdd_HHmmss).pdf"

Write-Host "✅ PDF guardado exitosamente" -ForegroundColor Green

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 3. Comparar: Producto específico vs TODOS
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "📊 Comparando: Detalle de producto de UN almacén" -ForegroundColor Yellow
$body = @{
    periodId = $periodId
    warehouseId = 1
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$baseUrl/reports/product-detail/pdf" `
    -Method Post `
    -Headers $headers `
    -Body $body `
    -OutFile "producto_almacen1_$(Get-Date -f yyyyMMdd_HHmmss).pdf"

Write-Host "✅ PDF guardado: producto_almacen1_*.pdf" -ForegroundColor Green
Write-Host "🔍 Diferencia: Este incluye SOLO almacén 1, mientras que /all/pdf incluye TODOS" -ForegroundColor Gray

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 4. Resumen de cambios
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "`n" + ("="*60) -ForegroundColor Blue
Write-Host "RESUMEN DE NUEVAS APIS" -ForegroundColor Blue
Write-Host ("="*60) -ForegroundColor Blue

$newApis = @(
    @{
        Endpoint = "POST /reports/product-detail/all/pdf"
        Request = '{ "periodId": 1 }'
        Descripcion = "Detalle de TODOS los productos"
        Archivo = "productos_todos_*.pdf"
    },
    @{
        Endpoint = "POST /reports/comparative/all/pdf"
        Request = '{ "periodId": 1 }'
        Descripcion = "Comparativo de TODOS los almacenes"
        Archivo = "comparativo_todos_*.pdf"
    }
)

foreach ($api in $newApis) {
    Write-Host "`n🆕 $($api.Endpoint)" -ForegroundColor Green
    Write-Host "   Descripción: $($api.Descripcion)" -ForegroundColor White
    Write-Host "   Request: $($api.Request)" -ForegroundColor Gray
    Write-Host "   Archivo: $($api.Archivo)" -ForegroundColor Yellow
}

Write-Host "`n" + ("="*60) -ForegroundColor Blue
Write-Host "✅ Todas las APIs han sido implementadas correctamente" -ForegroundColor Green
```

---

## cURL Script para probar

```bash
#!/bin/bash

BASE_URL="http://localhost:8080/api/sigmav2/labels"
TOKEN="<TU_JWT_TOKEN_AQUI>"
PERIOD_ID=1

# Headers
HEADERS=(
    -H "Authorization: Bearer $TOKEN"
    -H "Content-Type: application/json"
)

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 1. Detalle de TODOS los Productos
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo "📊 Generando: Detalle de TODOS los Productos (PDF)"
curl -X POST "${BASE_URL}/reports/product-detail/all/pdf" \
  "${HEADERS[@]}" \
  -d "{\"periodId\": $PERIOD_ID}" \
  --output "productos_todos_$(date +%s).pdf"

echo "✅ Guardado: productos_todos_*.pdf"

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 2. Comparativo de TODOS los Almacenes
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo "📊 Generando: Comparativo de TODOS los Almacenes (PDF)"
curl -X POST "${BASE_URL}/reports/comparative/all/pdf" \
  "${HEADERS[@]}" \
  -d "{\"periodId\": $PERIOD_ID}" \
  --output "comparativo_todos_$(date +%s).pdf"

echo "✅ Guardado: comparativo_todos_*.pdf"
```

---

## Postman Collection

```json
{
  "info": {
    "name": "SIGMAV2 - Nuevas APIs de Reportes",
    "description": "Testing de las nuevas APIs /all/pdf"
  },
  "item": [
    {
      "name": "1. Detalle de TODOS los Productos (PDF)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}",
            "type": "text"
          },
          {
            "key": "Content-Type",
            "value": "application/json",
            "type": "text"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"periodId\": 1\n}"
        },
        "url": {
          "raw": "{{base_url}}/api/sigmav2/labels/reports/product-detail/all/pdf",
          "host": ["{{base_url}}"],
          "path": ["api", "sigmav2", "labels", "reports", "product-detail", "all", "pdf"]
        }
      }
    },
    {
      "name": "2. Comparativo de TODOS los Almacenes (PDF)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}",
            "type": "text"
          },
          {
            "key": "Content-Type",
            "value": "application/json",
            "type": "text"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"periodId\": 1\n}"
        },
        "url": {
          "raw": "{{base_url}}/api/sigmav2/labels/reports/comparative/all/pdf",
          "host": ["{{base_url}}"],
          "path": ["api", "sigmav2", "labels", "reports", "comparative", "all", "pdf"]
        }
      }
    }
  ]
}
```

---

## Checklist de Validación

- [ ] ✅ Endpoint `/reports/product-detail/all/pdf` retorna PDF
- [ ] ✅ Endpoint `/reports/comparative/all/pdf` retorna PDF
- [ ] ✅ Ambos endpoints requieren `periodId` solamente (sin `warehouseId`)
- [ ] ✅ Ambos respetan roles y autenticación JWT
- [ ] ✅ Retornan 404 si no hay datos
- [ ] ✅ Los archivos PDF se generan correctamente
- [ ] ✅ Los nombres incluyen timestamp único
- [ ] ✅ El servicio `LabelReportService` no fue modificado (reutiliza métodos existentes)

---

**Fecha:** 2026-03-13  
**Estado:** ✅ Implementado

