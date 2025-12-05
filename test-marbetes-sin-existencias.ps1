# Script de Prueba: Marbetes con Validación de Existencias
# Este script prueba la nueva funcionalidad de generación con validación

$baseUrl = "http://localhost:8080/api/sigmav2"
$token = ""

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Prueba: Marbetes Sin Existencias" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Solicitar token
Write-Host "Por favor ingresa el token JWT:" -ForegroundColor Yellow
$token = Read-Host

if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Host "❌ ERROR: Token vacío" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# Solicitar parámetros
Write-Host ""
Write-Host "Ingresa el ID del producto (ej: 123):" -ForegroundColor Yellow
$productId = Read-Host

Write-Host "Ingresa el ID del almacén (ej: 15):" -ForegroundColor Yellow
$warehouseId = Read-Host

Write-Host "Ingresa el ID del periodo (ej: 1):" -ForegroundColor Yellow
$periodId = Read-Host

Write-Host "Cantidad de marbetes a generar (ej: 5):" -ForegroundColor Yellow
$quantity = Read-Host

Write-Host ""
Write-Host "=== TEST 1: Generar Marbetes ===" -ForegroundColor Green
Write-Host "Generando $quantity marbetes para producto $productId..." -ForegroundColor Gray

try {
    $generateBody = @{
        productId = [int]$productId
        warehouseId = [int]$warehouseId
        periodId = [int]$periodId
        labelsToGenerate = [int]$quantity
    } | ConvertTo-Json

    $generateUrl = "$baseUrl/labels/generate"
    Write-Host "POST $generateUrl" -ForegroundColor Gray

    $response = Invoke-RestMethod -Uri $generateUrl -Method Post -Headers $headers -Body $generateBody

    Write-Host ""
    Write-Host "✅ Generación Completada" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 RESUMEN:" -ForegroundColor Cyan
    Write-Host "  Total Generados: $($response.totalGenerados)" -ForegroundColor White
    Write-Host "  Con Existencias: $($response.generadosConExistencias)" -ForegroundColor Green
    Write-Host "  Sin Existencias: $($response.generadosSinExistencias)" -ForegroundColor Red
    Write-Host "  Rango de Folios: $($response.primerFolio) - $($response.ultimoFolio)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "💬 Mensaje: $($response.mensaje)" -ForegroundColor Gray

    # Guardar folios para pruebas posteriores
    $primerFolio = $response.primerFolio
    $ultimoFolio = $response.ultimoFolio
    $sinExistencias = $response.generadosSinExistencias

    if ($sinExistencias -gt 0) {
        Write-Host ""
        Write-Host "⚠️  ADVERTENCIA: Hay $sinExistencias marbete(s) sin existencias" -ForegroundColor Yellow
        Write-Host "   Estos marbetes están en estado CANCELADO" -ForegroundColor Yellow

        Write-Host ""
        Write-Host "=== TEST 2: Consultar Marbetes Cancelados ===" -ForegroundColor Green

        Start-Sleep -Seconds 1

        $cancelledUrl = "$baseUrl/labels/cancelled?periodId=$periodId&warehouseId=$warehouseId"
        Write-Host "GET $cancelledUrl" -ForegroundColor Gray

        $cancelled = Invoke-RestMethod -Uri $cancelledUrl -Method Get -Headers $headers

        if ($cancelled.Count -gt 0) {
            Write-Host ""
            Write-Host "✅ Encontrados $($cancelled.Count) marbete(s) cancelado(s):" -ForegroundColor Green
            Write-Host ""

            foreach ($item in $cancelled) {
                Write-Host "  📝 Folio: $($item.folio)" -ForegroundColor White
                Write-Host "     Producto: $($item.claveProducto) - $($item.nombreProducto)" -ForegroundColor Gray
                Write-Host "     Existencias al cancelar: $($item.existenciasAlCancelar)" -ForegroundColor Gray
                Write-Host "     Existencias actuales: $($item.existenciasActuales)" -ForegroundColor Gray
                Write-Host "     Reactivado: $($item.reactivado)" -ForegroundColor $(if ($item.reactivado) { "Green" } else { "Red" })
                Write-Host ""
            }

            # Preguntar si quiere actualizar existencias
            Write-Host "¿Deseas actualizar existencias de algún folio? (S/N):" -ForegroundColor Yellow
            $respuesta = Read-Host

            if ($respuesta -eq "S" -or $respuesta -eq "s") {
                Write-Host ""
                Write-Host "=== TEST 3: Actualizar Existencias ===" -ForegroundColor Green

                Write-Host "Ingresa el folio a actualizar:" -ForegroundColor Yellow
                $folioActualizar = Read-Host

                Write-Host "Ingresa las nuevas existencias:" -ForegroundColor Yellow
                $nuevasExistencias = Read-Host

                Write-Host "Notas (opcional):" -ForegroundColor Yellow
                $notas = Read-Host

                $updateBody = @{
                    folio = [long]$folioActualizar
                    existenciasActuales = [int]$nuevasExistencias
                    notas = $notas
                } | ConvertTo-Json

                $updateUrl = "$baseUrl/labels/cancelled/update-stock"
                Write-Host "PUT $updateUrl" -ForegroundColor Gray

                $updated = Invoke-RestMethod -Uri $updateUrl -Method Put -Headers $headers -Body $updateBody

                Write-Host ""
                Write-Host "✅ Existencias Actualizadas" -ForegroundColor Green
                Write-Host "  Folio: $($updated.folio)" -ForegroundColor White
                Write-Host "  Existencias actuales: $($updated.existenciasActuales)" -ForegroundColor White
                Write-Host "  Reactivado: $($updated.reactivado)" -ForegroundColor $(if ($updated.reactivado) { "Green" } else { "Red" })

                if ($updated.reactivado) {
                    Write-Host ""
                    Write-Host "🎉 ¡Marbete REACTIVADO!" -ForegroundColor Green
                    Write-Host "   El marbete está ahora disponible para impresión" -ForegroundColor Green
                }
            }
        } else {
            Write-Host "ℹ️  No hay marbetes cancelados para este periodo y almacén" -ForegroundColor Cyan
        }
    } else {
        Write-Host ""
        Write-Host "✅ Todos los marbetes se generaron correctamente (con existencias)" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "=== TEST 4: Verificar en Base de Datos ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "Puedes verificar en MySQL con:" -ForegroundColor Gray
    Write-Host ""
    Write-Host "-- Ver marbetes generados (GENERADO)" -ForegroundColor Cyan
    Write-Host "SELECT * FROM labels WHERE folio BETWEEN $primerFolio AND $ultimoFolio;" -ForegroundColor White
    Write-Host ""
    Write-Host "-- Ver marbetes cancelados (CANCELADO)" -ForegroundColor Cyan
    Write-Host "SELECT * FROM labels_cancelled WHERE folio BETWEEN $primerFolio AND $ultimoFolio;" -ForegroundColor White
    Write-Host ""

} catch {
    Write-Host ""
    Write-Host "❌ ERROR:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red

    if ($_.ErrorDetails) {
        Write-Host ""
        Write-Host "Detalles:" -ForegroundColor Yellow
        Write-Host $_.ErrorDetails.Message -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Prueba Completada" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📚 Para más información, consulta:" -ForegroundColor Gray
Write-Host "   - IMPLEMENTACION-MARBETES-SIN-EXISTENCIAS.md" -ForegroundColor Gray
Write-Host "   - GUIA-RAPIDA-MARBETES-SIN-EXISTENCIAS.md" -ForegroundColor Gray

