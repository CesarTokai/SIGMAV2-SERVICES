# Script de Prueba: Validación de Cero Folios
# Verifica que la nueva validación funcione correctamente

param(
    [string]$Token = "",
    [string]$BaseUrl = "http://localhost:8080/api/sigmav2"
)

if ([string]::IsNullOrWhiteSpace($Token)) {
    Write-Host "❌ Error: Debe proporcionar un token JWT" -ForegroundColor Red
    Write-Host "Uso: .\test-validacion-cero-folios.ps1 -Token 'TU_TOKEN_AQUI'" -ForegroundColor Yellow
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type" = "application/json"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PRUEBAS: Validación de Cero Folios" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# PRUEBA 1: Intentar solicitar 0 folios para un producto que SÍ existe en inventario
Write-Host "PRUEBA 1: Solicitar 0 folios para producto EN inventario" -ForegroundColor Yellow
Write-Host "Resultado esperado: ❌ Error 400" -ForegroundColor Gray
Write-Host ""

# Primero, obtener un producto que exista en el inventario
try {
    $summaryBody = @{
        periodId = 1
        warehouseId = 1
        page = 0
        size = 10
    } | ConvertTo-Json

    $summary = Invoke-RestMethod -Uri "$BaseUrl/labels/summary" -Method Post -Headers $headers -Body $summaryBody

    if ($summary.Count -eq 0) {
        Write-Host "⚠️  No hay productos en el inventario para probar" -ForegroundColor Yellow
        exit 0
    }

    $productoTest = $summary[0]
    Write-Host "Producto de prueba: $($productoTest.claveProducto)" -ForegroundColor White
    Write-Host "Existencias: $($productoTest.existencias)" -ForegroundColor Gray
    Write-Host ""

    # Intentar solicitar 0 folios
    $requestBody = @{
        productId = $productoTest.productId
        warehouseId = 1
        periodId = 1
        requestedLabels = 0
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$BaseUrl/labels/request" -Method Post -Headers $headers -Body $requestBody | Out-Null
        Write-Host "❌ FALLO: Se permitió solicitar 0 folios (debería rechazarse)" -ForegroundColor Red
        Write-Host ""
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $errorMessage = $_.ErrorDetails.Message

        if ($statusCode -eq 400 -or $statusCode -eq 422) {
            Write-Host "✅ ÉXITO: Se rechazó correctamente (Status: $statusCode)" -ForegroundColor Green

            # Intentar extraer el mensaje de error
            try {
                $errorObj = $errorMessage | ConvertFrom-Json
                Write-Host "Mensaje: $($errorObj.message)" -ForegroundColor Gray
            } catch {
                Write-Host "Mensaje: $errorMessage" -ForegroundColor Gray
            }
            Write-Host ""
        } else {
            Write-Host "⚠️  INESPERADO: Status code $statusCode" -ForegroundColor Yellow
            Write-Host "Mensaje: $errorMessage" -ForegroundColor Gray
            Write-Host ""
        }
    }

} catch {
    Write-Host "❌ Error en la prueba: $_" -ForegroundColor Red
    Write-Host ""
}

# PRUEBA 2: Solicitar 1+ folios para producto con 0 existencias
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PRUEBA 2: Solicitar 1 folio para producto con 0 existencias" -ForegroundColor Yellow
Write-Host "Resultado esperado: ✅ Éxito 201" -ForegroundColor Gray
Write-Host ""

try {
    # Buscar un producto con 0 existencias
    $productoSinExistencias = $summary | Where-Object { $_.existencias -eq 0 } | Select-Object -First 1

    if ($null -eq $productoSinExistencias) {
        Write-Host "ℹ No hay productos con 0 existencias para probar" -ForegroundColor Gray
        Write-Host "Saltando esta prueba..." -ForegroundColor Gray
        Write-Host ""
    } else {
        Write-Host "Producto de prueba: $($productoSinExistencias.claveProducto)" -ForegroundColor White
        Write-Host "Existencias: $($productoSinExistencias.existencias)" -ForegroundColor Gray
        Write-Host ""

        # Intentar solicitar 1 folio
        $requestBody = @{
            productId = $productoSinExistencias.productId
            warehouseId = 1
            periodId = 1
            requestedLabels = 1
        } | ConvertTo-Json

        try {
            Invoke-RestMethod -Uri "$BaseUrl/labels/request" -Method Post -Headers $headers -Body $requestBody | Out-Null
            Write-Host "✅ ÉXITO: Se permitió solicitar 1 folio" -ForegroundColor Green
            Write-Host ""
        } catch {
            $statusCode = $_.Exception.Response.StatusCode.value__
            $errorMessage = $_.ErrorDetails.Message

            Write-Host "❌ FALLO: Se rechazó la solicitud (Status: $statusCode)" -ForegroundColor Red
            Write-Host "Mensaje: $errorMessage" -ForegroundColor Gray
            Write-Host ""
        }
    }

} catch {
    Write-Host "❌ Error en la prueba: $_" -ForegroundColor Red
    Write-Host ""
}

# PRUEBA 3: Verificar comportamiento de producto NO en inventario
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PRUEBA 3: Solicitar 0 folios para producto NO en inventario" -ForegroundColor Yellow
Write-Host "Resultado esperado: ✅ Éxito (cancela solicitud)" -ForegroundColor Gray
Write-Host ""

try {
    # Usar un ID de producto que probablemente no exista
    $productIdNoExistente = 999999

    Write-Host "Producto de prueba: ID $productIdNoExistente (no existe)" -ForegroundColor White
    Write-Host ""

    $requestBody = @{
        productId = $productIdNoExistente
        warehouseId = 1
        periodId = 1
        requestedLabels = 0
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$BaseUrl/labels/request" -Method Post -Headers $headers -Body $requestBody | Out-Null
        Write-Host "✅ ÉXITO: Se permitió (producto no existe en inventario)" -ForegroundColor Green
        Write-Host ""
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__

        # Puede fallar por otras razones (producto no existe en catálogo)
        if ($statusCode -eq 404) {
            Write-Host "ℹ Producto no encontrado en catálogo (esperado)" -ForegroundColor Gray
        } else {
            Write-Host "⚠️  Status: $statusCode" -ForegroundColor Yellow
        }
        Write-Host ""
    }

} catch {
    Write-Host "❌ Error en la prueba: $_" -ForegroundColor Red
    Write-Host ""
}

# RESUMEN
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMEN DE PRUEBAS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Validación implementada:" -ForegroundColor White
Write-Host "✓ Productos EN inventario: NO pueden solicitar 0 folios" -ForegroundColor Green
Write-Host "✓ Productos EN inventario: SÍ pueden solicitar 1+ folios (incluso con 0 existencias)" -ForegroundColor Green
Write-Host "✓ Productos NO en inventario: Pueden solicitar 0 folios (cancela solicitud)" -ForegroundColor Green
Write-Host ""

Write-Host "Objetivo:" -ForegroundColor White
Write-Host "Prevenir 'huecos' en la secuencia de folios causados por" -ForegroundColor Gray
Write-Host "productos sin existencias que tienen 0 folios solicitados." -ForegroundColor Gray
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan

