# Script de prueba para verificar el endpoint de labels/summary
# Este script prueba diferentes escenarios de consulta

$baseUrl = "http://localhost:8080/api/sigmav2"
$token = ""  # Reemplazar con tu token JWT

Write-Host "=== PRUEBAS DE ENDPOINT /labels/summary ===" -ForegroundColor Cyan
Write-Host ""

# Función para hacer peticiones
function Test-LabelsSummary {
    param(
        [string]$TestName,
        [hashtable]$Body
    )

    Write-Host "Prueba: $TestName" -ForegroundColor Yellow
    Write-Host "Body: $($Body | ConvertTo-Json -Compress)" -ForegroundColor Gray

    try {
        $headers = @{
            "Authorization" = "Bearer $token"
            "Content-Type" = "application/json"
        }

        $response = Invoke-RestMethod -Uri "$baseUrl/labels/summary" `
            -Method POST `
            -Headers $headers `
            -Body ($Body | ConvertTo-Json) `
            -ErrorAction Stop

        Write-Host "✅ Respuesta exitosa" -ForegroundColor Green
        Write-Host "Total items: $($response.Count)" -ForegroundColor Green

        if ($response.Count -gt 0) {
            Write-Host "Primer item:" -ForegroundColor Gray
            $response[0] | ConvertTo-Json | Write-Host -ForegroundColor Gray
        }

    } catch {
        Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails) {
            Write-Host "Detalles: $($_.ErrorDetails.Message)" -ForegroundColor Red
        }
    }

    Write-Host ""
}

# Test 1: Consulta con periodo y almacén por defecto
Test-LabelsSummary -TestName "1. Periodo y almacén por defecto" -Body @{
    page = 0
    size = 10
    sortBy = "claveProducto"
    sortDirection = "ASC"
}

# Test 2: Consulta con periodo y almacén específicos
Test-LabelsSummary -TestName "2. Periodo y almacén específicos" -Body @{
    periodId = 7
    warehouseId = 250
    page = 0
    size = 50
    sortBy = "nombreProducto"
    sortDirection = "ASC"
}

# Test 3: Búsqueda por texto
Test-LabelsSummary -TestName "3. Búsqueda por texto 'prod'" -Body @{
    periodId = 7
    warehouseId = 250
    page = 0
    size = 50
    searchText = "prod"
    sortBy = "nombreProducto"
    sortDirection = "ASC"
}

# Test 4: Ordenar por existencias descendente
Test-LabelsSummary -TestName "4. Ordenar por existencias DESC" -Body @{
    periodId = 7
    warehouseId = 250
    page = 0
    size = 10
    sortBy = "existencias"
    sortDirection = "DESC"
}

# Test 5: Paginación - segunda página
Test-LabelsSummary -TestName "5. Paginación - página 1 (segunda)" -Body @{
    periodId = 7
    warehouseId = 250
    page = 1
    size = 25
    sortBy = "claveProducto"
    sortDirection = "ASC"
}

# Test 6: Tamaño de página personalizado
Test-LabelsSummary -TestName "6. Tamaño de página 100" -Body @{
    periodId = 7
    warehouseId = 250
    page = 0
    size = 100
    sortBy = "claveProducto"
    sortDirection = "ASC"
}

Write-Host "=== FIN DE PRUEBAS ===" -ForegroundColor Cyan

