# Script para diagnosticar problema de visualización de marbetes
# Este script prueba el endpoint de debug y el endpoint de summary

$baseUrl = "http://localhost:8080"
$token = ""

Write-Host "=== Script de Diagnóstico de Marbetes ===" -ForegroundColor Cyan
Write-Host ""

# Leer token
Write-Host "Por favor ingresa el token JWT:" -ForegroundColor Yellow
$token = Read-Host

if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Host "ERROR: Token vacío" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Leer parámetros
Write-Host ""
Write-Host "Ingresa el ID del periodo (ej: 1):" -ForegroundColor Yellow
$periodId = Read-Host

Write-Host "Ingresa el ID del almacén (ej: 1):" -ForegroundColor Yellow
$warehouseId = Read-Host

Write-Host ""
Write-Host "=== 1. Consultando conteo de marbetes (endpoint debug) ===" -ForegroundColor Green

try {
    $debugUrl = "$baseUrl/api/labels/debug/count?periodId=$periodId&warehouseId=$warehouseId"
    Write-Host "GET $debugUrl" -ForegroundColor Gray

    $debugResponse = Invoke-RestMethod -Uri $debugUrl -Method Get -Headers $headers

    Write-Host "Respuesta del servidor:" -ForegroundColor Green
    $debugResponse | ConvertTo-Json -Depth 5 | Write-Host

    if ($debugResponse.totalLabels -eq 0) {
        Write-Host ""
        Write-Host "ADVERTENCIA: No se encontraron marbetes generados!" -ForegroundColor Red
        Write-Host "Verifica que:" -ForegroundColor Yellow
        Write-Host "  1. Has solicitado folios correctamente" -ForegroundColor Yellow
        Write-Host "  2. Has ejecutado 'Generar marbetes'" -ForegroundColor Yellow
        Write-Host "  3. El periodId y warehouseId son correctos" -ForegroundColor Yellow
    } else {
        Write-Host ""
        Write-Host "OK: Se encontraron $($debugResponse.totalLabels) marbetes" -ForegroundColor Green
    }
} catch {
    Write-Host "ERROR al consultar endpoint debug:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Write-Host "=== 2. Consultando resumen de marbetes (endpoint summary) ===" -ForegroundColor Green

try {
    $summaryUrl = "$baseUrl/api/labels/summary"
    Write-Host "POST $summaryUrl" -ForegroundColor Gray

    $summaryBody = @{
        periodId = [int]$periodId
        warehouseId = [int]$warehouseId
        page = 0
        size = 10
        sortBy = "claveProducto"
        sortDirection = "ASC"
    } | ConvertTo-Json

    Write-Host "Body:" -ForegroundColor Gray
    Write-Host $summaryBody -ForegroundColor Gray

    $summaryResponse = Invoke-RestMethod -Uri $summaryUrl -Method Post -Headers $headers -Body $summaryBody

    Write-Host ""
    Write-Host "Respuesta del servidor:" -ForegroundColor Green
    $summaryResponse | ConvertTo-Json -Depth 5 | Write-Host

    if ($summaryResponse -is [array] -and $summaryResponse.Count -gt 0) {
        Write-Host ""
        Write-Host "Se encontraron $($summaryResponse.Count) productos:" -ForegroundColor Green

        foreach ($item in $summaryResponse) {
            $status = if ($item.foliosExistentes -gt 0) { "✓" } else { "✗" }
            $color = if ($item.foliosExistentes -gt 0) { "Green" } else { "Yellow" }

            Write-Host "$status Producto: $($item.claveProducto) - $($item.nombreProducto)" -ForegroundColor $color
            Write-Host "   Solicitados: $($item.foliosSolicitados), Generados: $($item.foliosExistentes)" -ForegroundColor Gray
        }
    } else {
        Write-Host ""
        Write-Host "No se encontraron productos en el resumen" -ForegroundColor Yellow
    }
} catch {
    Write-Host "ERROR al consultar endpoint summary:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Write-Host "=== 3. Consultando solicitudes de folios ===" -ForegroundColor Green

try {
    # Este endpoint no existe pero podríamos agregarlo si necesitamos más debug
    Write-Host "Para ver las solicitudes, consulta directamente en la BD la tabla 'label_request'" -ForegroundColor Gray
    Write-Host "SELECT * FROM label_request WHERE id_period=$periodId AND id_warehouse=$warehouseId;" -ForegroundColor Cyan
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Diagnóstico completado ===" -ForegroundColor Cyan

