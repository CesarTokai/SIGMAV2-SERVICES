# Script para Probar el Historial de Conteos
# Uso: .\test-count-history.ps1 -Email "usuario@tokai.com" -Password "password123"

param(
    [string]$Email = "admin@tokai.com",
    [string]$Password = "admin123",
    [string]$BaseUrl = "http://localhost:8080"
)

# Colores para output
function Write-Success { Write-Host $args -ForegroundColor Green }
function Write-Info { Write-Host $args -ForegroundColor Cyan }
function Write-Error { Write-Host $args -ForegroundColor Red }

# 1. Login para obtener JWT
Write-Info "📋 Paso 1: Obteniendo JWT Token..."
$loginBody = @{
    email = $Email
    password = $Password
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest -Uri "$BaseUrl/api/sigmav2/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody

    $loginData = $loginResponse.Content | ConvertFrom-Json
    $jwtToken = $loginData.token

    if ($jwtToken) {
        Write-Success "✅ Token obtenido: $($jwtToken.Substring(0, 50))..."
    } else {
        Write-Error "❌ No se pudo obtener el token"
        exit
    }
} catch {
    Write-Error "❌ Error en login: $_"
    exit
}

$headers = @{
    "Authorization" = "Bearer $jwtToken"
    "Content-Type" = "application/json"
}

# 2. Obtener mi historial de conteos
Write-Info "`n📊 Paso 2: Consultando MI HISTORIAL DE CONTEOS..."
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/sigmav2/labels/history/my-counts?page=0&size=10" `
        -Method GET `
        -Headers $headers

    $data = $response.Content | ConvertFrom-Json
    Write-Success "✅ Historial obtenido:"
    Write-Host ($data | ConvertTo-Json -Depth 5)
} catch {
    Write-Error "❌ Error: $_"
}

# 3. Obtener historial de un folio específico
Write-Info "`n🔍 Paso 3: Consultando HISTORIAL DE UN FOLIO ESPECÍFICO..."
$folioId = 100

try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/sigmav2/labels/history/folio/$folioId" `
        -Method GET `
        -Headers $headers

    $data = $response.Content | ConvertFrom-Json
    if ($data.data.Count -gt 0) {
        Write-Success "✅ Historial del folio $folioId obtenido:"
        Write-Host ($data.data | ConvertTo-Json -Depth 3)
    } else {
        Write-Info "ℹ️ No hay registros para el folio $folioId"
    }
} catch {
    Write-Info "ℹ️ El folio podría no existir: $_"
}

# 4. Obtener historial de un período (si tiene acceso de ADMINISTRADOR o ALMACENISTA)
Write-Info "`n📅 Paso 4: Consultando HISTORIAL DE PERÍODO..."
$periodId = 1

try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/sigmav2/labels/history/period/$periodId?page=0&size=5" `
        -Method GET `
        -Headers $headers

    $data = $response.Content | ConvertFrom-Json
    Write-Success "✅ Historial del período $periodId obtenido:"
    Write-Host "Total de registros: $($data.totalElements)"
    Write-Host "Total de páginas: $($data.totalPages)"
    Write-Host ($data.data | ConvertTo-Json -Depth 3)
} catch {
    Write-Info "ℹ️ No tiene acceso o el período no existe: $_"
}

# 5. Obtener historial de un almacén (si tiene acceso)
Write-Info "`n🏢 Paso 5: Consultando HISTORIAL DE ALMACÉN..."
$warehouseId = 1

try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/sigmav2/labels/history/warehouse/$warehouseId?page=0&size=5" `
        -Method GET `
        -Headers $headers

    $data = $response.Content | ConvertFrom-Json
    Write-Success "✅ Historial del almacén $warehouseId obtenido:"
    Write-Host "Total de registros: $($data.totalElements)"
    Write-Host ($data.data | ConvertTo-Json -Depth 3)
} catch {
    Write-Info "ℹ️ No tiene acceso o el almacén no existe: $_"
}

# 6. Obtener historial de usuario en período específico (solo ADMINISTRADOR/ALMACENISTA)
Write-Info "`n👤 Paso 6: Consultando HISTORIAL DE USUARIO EN PERÍODO..."
$userId = 1
$periodId = 1

try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/sigmav2/labels/history/user/$userId/period/$periodId?page=0&size=5" `
        -Method GET `
        -Headers $headers

    $data = $response.Content | ConvertFrom-Json
    Write-Success "✅ Historial del usuario $userId en período $periodId obtenido:"
    Write-Host "Total de conteos: $($data.totalConteosRegistrados)"
    Write-Host ($data.data | ConvertTo-Json -Depth 3)
} catch {
    Write-Info "ℹ️ No tiene acceso o los datos no existen: $_"
}

Write-Success "`n✅ Pruebas completadas"

