# Script para Corregir Folios Saltados
# Corrige productos con requestedLabels = 0 y genera sus folios faltantes

param(
    [string]$Token = "",
    [int]$PeriodId = 1,
    [int]$WarehouseId = 1,
    [string]$BaseUrl = "http://localhost:8080/api/sigmav2"
)

# Validar que se proporcionó el token
if ([string]::IsNullOrWhiteSpace($Token)) {
    Write-Host "❌ Error: Debe proporcionar un token JWT" -ForegroundColor Red
    Write-Host ""
    Write-Host "Uso:" -ForegroundColor Yellow
    Write-Host "  .\corregir-folios-saltados.ps1 -Token 'TU_TOKEN_AQUI'" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Opcional:" -ForegroundColor Gray
    Write-Host "  -PeriodId 1" -ForegroundColor Gray
    Write-Host "  -WarehouseId 1" -ForegroundColor Gray
    Write-Host "  -BaseUrl 'http://localhost:8080/api/sigmav2'" -ForegroundColor Gray
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type" = "application/json"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CORRECCIÓN DE FOLIOS SALTADOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Periodo: $PeriodId" -ForegroundColor Gray
Write-Host "Almacén: $WarehouseId" -ForegroundColor Gray
Write-Host ""

# PASO 1: Obtener resumen actual
Write-Host "PASO 1: Obteniendo resumen de marbetes..." -ForegroundColor Yellow
Write-Host ""

$summaryBody = @{
    periodId = $PeriodId
    warehouseId = $WarehouseId
    page = 0
    size = 1000
} | ConvertTo-Json

try {
    $summary = Invoke-RestMethod -Uri "$BaseUrl/labels/summary" -Method Post -Headers $headers -Body $summaryBody

    # Filtrar productos con 0 folios generados pero que SÍ tienen existencias (o 0 existencias pero están en inventario)
    $productosProblema = $summary | Where-Object {
        $_.cantidadFolios -eq 0
    }

    if ($productosProblema.Count -eq 0) {
        Write-Host "✅ No se encontraron productos con folios faltantes" -ForegroundColor Green
        Write-Host ""
        Write-Host "El sistema está correcto. Todos los productos en inventario tienen folios." -ForegroundColor Gray
        exit 0
    }

    Write-Host "⚠️  Productos con problemas encontrados: $($productosProblema.Count)" -ForegroundColor Yellow
    Write-Host ""

    foreach ($producto in $productosProblema) {
        Write-Host "  - $($producto.claveProducto): $($producto.producto)" -ForegroundColor Gray
        Write-Host "    Existencias: $($producto.existencias)" -ForegroundColor Gray
        Write-Host "    Folios actuales: $($producto.cantidadFolios)" -ForegroundColor Red
        Write-Host ""
    }

} catch {
    Write-Host "❌ Error obteniendo resumen: $_" -ForegroundColor Red
    exit 1
}

# PASO 2: Confirmar acción
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PASO 2: Confirmación" -ForegroundColor Yellow
Write-Host ""
Write-Host "Se van a realizar las siguientes acciones:" -ForegroundColor White
Write-Host "1. Solicitar 1 folio para cada producto sin folios" -ForegroundColor Gray
Write-Host "2. Generar el folio para cada producto" -ForegroundColor Gray
Write-Host ""
Write-Host "Total de productos a corregir: $($productosProblema.Count)" -ForegroundColor Yellow
Write-Host ""

$confirmacion = Read-Host "¿Desea continuar? (S/N)"

if ($confirmacion -ne "S" -and $confirmacion -ne "s") {
    Write-Host "❌ Operación cancelada por el usuario" -ForegroundColor Yellow
    exit 0
}

# PASO 3: Corregir cada producto
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PASO 3: Corrigiendo productos..." -ForegroundColor Yellow
Write-Host ""

$exitosos = 0
$errores = 0

foreach ($producto in $productosProblema) {
    Write-Host "Procesando: $($producto.claveProducto)..." -ForegroundColor White

    try {
        # 3.1 Solicitar 1 folio
        $requestBody = @{
            productId = $producto.productId
            warehouseId = $WarehouseId
            periodId = $PeriodId
            requestedLabels = 1
        } | ConvertTo-Json

        Write-Host "  → Solicitando 1 folio..." -ForegroundColor Gray

        try {
            Invoke-RestMethod -Uri "$BaseUrl/labels/request" -Method Post -Headers $headers -Body $requestBody | Out-Null
            Write-Host "  ✓ Solicitud creada" -ForegroundColor Green
        } catch {
            # Si ya existe la solicitud, puede fallar, intentar actualizar
            Write-Host "  ℹ Solicitud ya existe o error: $($_.Exception.Message)" -ForegroundColor Gray
        }

        # 3.2 Generar el folio
        $generateBody = @{
            productId = $producto.productId
            warehouseId = $WarehouseId
            periodId = $PeriodId
            labelsToGenerate = 1
        } | ConvertTo-Json

        Write-Host "  → Generando folio..." -ForegroundColor Gray

        $result = Invoke-RestMethod -Uri "$BaseUrl/labels/generate" -Method Post -Headers $headers -Body $generateBody

        Write-Host "  ✓ Folio generado: $($result.primerFolio)" -ForegroundColor Green
        Write-Host ""

        $exitosos++

        # Pequeña pausa para no saturar el servidor
        Start-Sleep -Milliseconds 200

    } catch {
        Write-Host "  ❌ Error: $_" -ForegroundColor Red
        Write-Host ""
        $errores++
    }
}

# PASO 4: Resumen final
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PASO 4: Resumen de la Corrección" -ForegroundColor Yellow
Write-Host ""

Write-Host "Total productos procesados: $($productosProblema.Count)" -ForegroundColor White
Write-Host "Exitosos: $exitosos" -ForegroundColor Green
Write-Host "Errores: $errores" -ForegroundColor $(if ($errores -gt 0) { "Red" } else { "Gray" })
Write-Host ""

# PASO 5: Verificación final
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PASO 5: Verificación Final" -ForegroundColor Yellow
Write-Host ""

Write-Host "Obteniendo nuevo resumen..." -ForegroundColor Gray

try {
    $summaryFinal = Invoke-RestMethod -Uri "$BaseUrl/labels/summary" -Method Post -Headers $headers -Body $summaryBody

    $productosAunConProblema = $summaryFinal | Where-Object { $_.cantidadFolios -eq 0 }

    if ($productosAunConProblema.Count -eq 0) {
        Write-Host "✅ ¡ÉXITO! Todos los productos tienen folios generados" -ForegroundColor Green
        Write-Host ""
        Write-Host "La secuencia de folios ahora debe ser continua" -ForegroundColor Gray
    } else {
        Write-Host "⚠️  Aún hay $($productosAunConProblema.Count) producto(s) sin folios:" -ForegroundColor Yellow
        Write-Host ""
        foreach ($prod in $productosAunConProblema) {
            Write-Host "  - $($prod.claveProducto)" -ForegroundColor Gray
        }
        Write-Host ""
        Write-Host "Puede que requieran corrección manual en la base de datos" -ForegroundColor Gray
    }

} catch {
    Write-Host "⚠️  No se pudo verificar el resultado final: $_" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RECOMENDACIONES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Ejecuta el script SQL de verificación:" -ForegroundColor White
Write-Host "   verificar-y-corregir-folios-saltados.sql (PASO 5)" -ForegroundColor Yellow
Write-Host ""
Write-Host "2. Verifica que no haya huecos en la secuencia:" -ForegroundColor White
Write-Host "   La consulta 5.1 del script SQL debe devolver 0 resultados" -ForegroundColor Yellow
Write-Host ""
Write-Host "3. Si aún hay huecos, puede ser por productos que:" -ForegroundColor White
Write-Host "   - Fueron creados ANTES de esta corrección" -ForegroundColor Gray
Write-Host "   - Los folios ya están ocupados por otros productos" -ForegroundColor Gray
Write-Host "   - Requieren corrección manual en BD" -ForegroundColor Gray
Write-Host ""

if ($exitosos -gt 0) {
    Write-Host "✓ Corrección completada exitosamente para $exitosos producto(s)" -ForegroundColor Green
} else {
    Write-Host "⚠️  No se corrigió ningún producto" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

