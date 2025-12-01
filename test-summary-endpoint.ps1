# Script de PowerShell para probar el endpoint /api/sigmav2/labels/summary

Write-Host "=== OBTENIENDO TOKEN JWT ===" -ForegroundColor Yellow

# PASO 1: Hacer login (reemplaza con tus credenciales reales)
$loginBody = @{
    email = "tu_email@example.com"
    password = "tu_password"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/auth/login" `
    -Method Post `
    -ContentType "application/json" `
    -Body $loginBody

$token = $loginResponse.token
Write-Host "Token obtenido: $($token.Substring(0, [Math]::Min(50, $token.Length)))..." -ForegroundColor Green

# PASO 2: Llamar al endpoint /summary
Write-Host "`n=== LLAMANDO A /api/sigmav2/labels/summary ===" -ForegroundColor Yellow

$summaryBody = @{
    periodId = 1
    warehouseId = 2
} | ConvertTo-Json

Write-Host "Body a enviar: $summaryBody" -ForegroundColor Cyan

try {
    $summaryResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/summary" `
        -Method Post `
        -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $token" } `
        -Body $summaryBody

    Write-Host "`n=== RESPUESTA EXITOSA ===" -ForegroundColor Green
    $summaryResponse | ConvertTo-Json -Depth 10
} catch {
    Write-Host "`n=== ERROR ===" -ForegroundColor Red
    Write-Host "StatusCode: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "StatusDescription: $($_.Exception.Response.StatusDescription)"
    Write-Host "Message: $($_.Exception.Message)"
}

Write-Host "`n=== FIN ===" -ForegroundColor Yellow

