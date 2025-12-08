# Script de Prueba para APIs de Cancelación y Reportes de Marbetes
# Fecha: 8 de Diciembre de 2025

# Configuración
$baseUrl = "http://localhost:8080/api/sigmav2/labels"
$token = "YOUR_JWT_TOKEN_HERE"  # Reemplazar con token real

$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Pruebas de APIs de Marbetes" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# ========== PRUEBA 1: Cancelar un Marbete ==========
Write-Host "PRUEBA 1: Cancelar Marbete" -ForegroundColor Yellow
$cancelBody = @{
    folio = 1001
    periodId = 1
    warehouseId = 2
    motivoCancelacion = "Prueba de cancelación desde PowerShell"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/cancel" -Method Post -Headers $headers -Body $cancelBody
    Write-Host "✓ Marbete cancelado exitosamente" -ForegroundColor Green
} catch {
    Write-Host "✗ Error al cancelar marbete: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Detalle: $($_.ErrorDetails.Message)" -ForegroundColor Red
}
Write-Host ""

# ========== PRUEBA 2: Reporte de Distribución ==========
Write-Host "PRUEBA 2: Reporte de Distribución" -ForegroundColor Yellow
$filterBody = @{
    periodId = 1
    warehouseId = $null
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reports/distribution" -Method Post -Headers $headers -Body $filterBody
    Write-Host "✓ Reporte generado con $($response.Count) registros" -ForegroundColor Green
    if ($response.Count -gt 0) {
        Write-Host "Primer registro:" -ForegroundColor Cyan
        $response[0] | ConvertTo-Json | Write-Host
    }
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ========== PRUEBA 3: Reporte de Listado Completo ==========
Write-Host "PRUEBA 3: Reporte de Listado de Marbetes" -ForegroundColor Yellow
$filterBody = @{
    periodId = 1
    warehouseId = 2
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reports/list" -Method Post -Headers $headers -Body $filterBody
    Write-Host "✓ Reporte generado con $($response.Count) registros" -ForegroundColor Green
    if ($response.Count -gt 0) {
        Write-Host "Primer marbete:" -ForegroundColor Cyan
        $response[0] | Format-Table -Property numeroMarbete, claveProducto, conteo1, conteo2, estado | Out-String | Write-Host
    }
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ========== PRUEBA 4: Reporte de Marbetes Pendientes ==========
Write-Host "PRUEBA 4: Reporte de Marbetes Pendientes" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reports/pending" -Method Post -Headers $headers -Body $filterBody
    Write-Host "✓ Reporte generado con $($response.Count) marbetes pendientes" -ForegroundColor Green
    if ($response.Count -gt 0) {
        Write-Host "Marbetes pendientes:" -ForegroundColor Cyan
        $response | Format-Table -Property numeroMarbete, claveProducto, conteo1, conteo2 | Out-String | Write-Host
    }
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ========== PRUEBA 5: Reporte de Marbetes con Diferencias ==========
Write-Host "PRUEBA 5: Reporte de Marbetes con Diferencias" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reports/with-differences" -Method Post -Headers $headers -Body $filterBody
    Write-Host "✓ Reporte generado con $($response.Count) marbetes con diferencias" -ForegroundColor Green
    if ($response.Count -gt 0) {
        Write-Host "Diferencias encontradas:" -ForegroundColor Cyan
        $response | Format-Table -Property numeroMarbete, claveProducto, conteo1, conteo2, diferencia | Out-String | Write-Host
    }
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ========== PRUEBA 6: Reporte de Marbetes Cancelados ==========
Write-Host "PRUEBA 6: Reporte de Marbetes Cancelados" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reports/cancelled" -Method Post -Headers $headers -Body $filterBody
    Write-Host "✓ Reporte generado con $($response.Count) marbetes cancelados" -ForegroundColor Green
    if ($response.Count -gt 0) {
        Write-Host "Marbetes cancelados:" -ForegroundColor Cyan
        $response | Format-Table -Property numeroMarbete, claveProducto, motivoCancelacion, canceladoPor | Out-String | Write-Host
    }
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ========== PRUEBA 7: Reporte Comparativo ==========
Write-Host "PRUEBA 7: Reporte Comparativo (Físico vs Teórico)" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reports/comparative" -Method Post -Headers $headers -Body $filterBody
    Write-Host "✓ Reporte generado con $($response.Count) productos analizados" -ForegroundColor Green
    if ($response.Count -gt 0) {
        Write-Host "Comparativo de existencias:" -ForegroundColor Cyan
        $response | Format-Table -Property claveProducto, existenciasFisicas, existenciasTeoricas, diferencia, porcentajeDiferencia | Out-String | Write-Host
    }
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ========== PRUEBA 8: Reporte de Almacén con Detalle ==========
Write-Host "PRUEBA 8: Reporte de Almacén con Detalle" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reports/warehouse-detail" -Method Post -Headers $headers -Body $filterBody
    Write-Host "✓ Reporte generado con $($response.Count) registros detallados" -ForegroundColor Green
    if ($response.Count -gt 0) {
        Write-Host "Primeros 5 registros:" -ForegroundColor Cyan
        $response | Select-Object -First 5 | Format-Table -Property numeroMarbete, claveProducto, cantidad, estado | Out-String | Write-Host
    }
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ========== PRUEBA 9: Reporte de Producto con Detalle ==========
Write-Host "PRUEBA 9: Reporte de Producto con Detalle" -ForegroundColor Yellow
$filterAllWarehouses = @{
    periodId = 1
    warehouseId = $null
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/reports/product-detail" -Method Post -Headers $headers -Body $filterAllWarehouses
    Write-Host "✓ Reporte generado con $($response.Count) registros detallados" -ForegroundColor Green
    if ($response.Count -gt 0) {
        Write-Host "Primeros 5 registros:" -ForegroundColor Cyan
        $response | Select-Object -First 5 | Format-Table -Property claveProducto, claveAlmacen, numeroMarbete, existencias, total | Out-String | Write-Host
    }
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Pruebas completadas" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

