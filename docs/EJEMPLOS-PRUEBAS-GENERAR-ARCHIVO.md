# 🧪 Ejemplos de Pruebas - API Generar Archivo

**Módulo:** Generar Archivo de Existencias  
**Endpoint:** `/api/sigmav2/labels/generate-file`  
**Fecha:** 16 de enero de 2026

---

## 📋 Índice

1. [Configuración Inicial](#configuración-inicial)
2. [Prueba Exitosa](#prueba-exitosa)
3. [Pruebas de Errores](#pruebas-de-errores)
4. [Ejemplos con cURL](#ejemplos-con-curl)
5. [Ejemplos con Postman](#ejemplos-con-postman)
6. [Ejemplos con JavaScript](#ejemplos-con-javascript)
7. [Validación del Archivo Generado](#validación-del-archivo-generado)

---

## 🔧 Configuración Inicial

### Variables de Entorno

Para facilitar las pruebas, configure las siguientes variables:

```bash
# URL Base del API
BASE_URL=http://localhost:8080

# Token de autenticación (obtenerlo del endpoint de login)
TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# ID del periodo a usar en las pruebas
PERIOD_ID=16
```

### Obtener Token de Autenticación

Primero, debe autenticarse para obtener un token JWT:

```bash
curl -X POST http://localhost:8080/api/sigmav2/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@tokai.com.mx",
    "password": "password123"
  }'
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "email": "admin@tokai.com.mx",
  "roles": ["ADMINISTRADOR"]
}
```

---

## ✅ Prueba Exitosa

### Caso de Éxito - Generar Archivo

#### Request con cURL

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/generate-file \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "periodId": 16
  }'
```

#### Request con PowerShell

```powershell
$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

$body = @{
    periodId = 16
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/generate-file" `
  -Method POST `
  -Headers $headers `
  -Body $body
```

#### Response Esperado (200 OK)

```json
{
  "fileName": "Existencias_Diciembre2016.txt",
  "filePath": "C:\\Sistemas\\SIGMA\\Documentos\\Existencias_Diciembre2016.txt",
  "totalProductos": 150,
  "mensaje": "Archivo generado exitosamente"
}
```

#### Verificación

1. El archivo debe existir en: `C:\Sistemas\SIGMA\Documentos\Existencias_Diciembre2016.txt`
2. Abrir el archivo y verificar el formato
3. Verificar que contenga 150 productos
4. Verificar que estén ordenados alfabéticamente

---

## ❌ Pruebas de Errores

### Error 1: Periodo No Encontrado (404/500)

#### Request

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/generate-file \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "periodId": 99999
  }'
```

#### Response Esperado

```json
{
  "error": "Periodo no encontrado",
  "status": 500,
  "timestamp": "2026-01-16T10:30:00.000+00:00",
  "path": "/api/sigmav2/labels/generate-file"
}
```

---

### Error 2: Token No Proporcionado (401)

#### Request

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/generate-file \
  -H "Content-Type: application/json" \
  -d '{
    "periodId": 16
  }'
```

#### Response Esperado

```json
{
  "error": "Unauthorized",
  "status": 401,
  "message": "Full authentication is required to access this resource"
}
```

---

### Error 3: Token Inválido (401)

#### Request

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/generate-file \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer token_invalido_123" \
  -d '{
    "periodId": 16
  }'
```

#### Response Esperado

```json
{
  "error": "Unauthorized",
  "status": 401,
  "message": "Invalid JWT token"
}
```

---

### Error 4: Sin Permisos (403)

Usuario con rol `AUXILIAR_DE_CONTEO` (no tiene permiso):

#### Request

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/generate-file \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer token_auxiliar_conteo..." \
  -d '{
    "periodId": 16
  }'
```

#### Response Esperado

```json
{
  "error": "Forbidden",
  "status": 403,
  "message": "Access Denied"
}
```

---

### Error 5: Validación - periodId Nulo (400)

#### Request

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/generate-file \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{}'
```

#### Response Esperado

```json
{
  "error": "Bad Request",
  "status": 400,
  "errors": [
    {
      "field": "periodId",
      "message": "El ID del periodo es obligatorio"
    }
  ]
}
```

---

### Error 6: Formato JSON Inválido (400)

#### Request

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/generate-file \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{ periodId: 16 }'
```

#### Response Esperado

```json
{
  "error": "Bad Request",
  "status": 400,
  "message": "JSON parse error"
}
```

---

## 📦 Colección de Postman

### Importar Colección

Copie el siguiente JSON y guárdelo como `generate-file-tests.postman_collection.json`:

```json
{
  "info": {
    "name": "SIGMA V2 - Generar Archivo",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    },
    {
      "key": "token",
      "value": ""
    },
    {
      "key": "periodId",
      "value": "16"
    }
  ],
  "item": [
    {
      "name": "1. Login - Obtener Token",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "if (pm.response.code === 200) {",
              "    var jsonData = pm.response.json();",
              "    pm.collectionVariables.set('token', jsonData.token);",
              "    pm.environment.set('token', jsonData.token);",
              "}"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n    \"email\": \"admin@tokai.com.mx\",\n    \"password\": \"password123\"\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/sigmav2/auth/login",
          "host": ["{{baseUrl}}"],
          "path": ["api", "sigmav2", "auth", "login"]
        }
      }
    },
    {
      "name": "2. Generar Archivo - Exitoso",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "pm.test('Status code is 200', function () {",
              "    pm.response.to.have.status(200);",
              "});",
              "",
              "pm.test('Response has fileName', function () {",
              "    var jsonData = pm.response.json();",
              "    pm.expect(jsonData).to.have.property('fileName');",
              "});",
              "",
              "pm.test('Response has filePath', function () {",
              "    var jsonData = pm.response.json();",
              "    pm.expect(jsonData).to.have.property('filePath');",
              "});",
              "",
              "pm.test('Response has totalProductos', function () {",
              "    var jsonData = pm.response.json();",
              "    pm.expect(jsonData).to.have.property('totalProductos');",
              "});"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n    \"periodId\": {{periodId}}\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/sigmav2/labels/generate-file",
          "host": ["{{baseUrl}}"],
          "path": ["api", "sigmav2", "labels", "generate-file"]
        }
      }
    },
    {
      "name": "3. Generar Archivo - Periodo Inexistente",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "pm.test('Status code is 500 or 404', function () {",
              "    pm.expect(pm.response.code).to.be.oneOf([500, 404]);",
              "});"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n    \"periodId\": 99999\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/sigmav2/labels/generate-file",
          "host": ["{{baseUrl}}"],
          "path": ["api", "sigmav2", "labels", "generate-file"]
        }
      }
    },
    {
      "name": "4. Generar Archivo - Sin Token",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "pm.test('Status code is 401', function () {",
              "    pm.response.to.have.status(401);",
              "});"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n    \"periodId\": {{periodId}}\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/sigmav2/labels/generate-file",
          "host": ["{{baseUrl}}"],
          "path": ["api", "sigmav2", "labels", "generate-file"]
        }
      }
    },
    {
      "name": "5. Generar Archivo - periodId Nulo",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "pm.test('Status code is 400', function () {",
              "    pm.response.to.have.status(400);",
              "});"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/sigmav2/labels/generate-file",
          "host": ["{{baseUrl}}"],
          "path": ["api", "sigmav2", "labels", "generate-file"]
        }
      }
    }
  ]
}
```

---

## 🔍 Validación del Archivo Generado

### Script PowerShell para Validar el Archivo

```powershell
# Ruta del archivo generado
$filePath = "C:\Sistemas\SIGMA\Documentos\Existencias_Diciembre2016.txt"

# Verificar que el archivo existe
if (Test-Path $filePath) {
    Write-Host "✅ Archivo encontrado: $filePath" -ForegroundColor Green
    
    # Leer contenido
    $content = Get-Content $filePath -Encoding UTF8
    
    # Verificar encabezado
    if ($content[0] -match "CLAVE_PRODUCTO.*DESCRIPCION.*EXISTENCIAS") {
        Write-Host "✅ Encabezado correcto" -ForegroundColor Green
    } else {
        Write-Host "❌ Encabezado incorrecto" -ForegroundColor Red
    }
    
    # Contar líneas de datos (excluyendo encabezado y separador)
    $dataLines = $content | Select-Object -Skip 2
    $totalProducts = $dataLines.Count
    Write-Host "📊 Total de productos: $totalProducts" -ForegroundColor Cyan
    
    # Mostrar primeros 5 productos
    Write-Host "`n📋 Primeros 5 productos:" -ForegroundColor Yellow
    $dataLines | Select-Object -First 5 | ForEach-Object {
        Write-Host $_
    }
    
    # Verificar ordenamiento alfabético
    $claves = $dataLines | ForEach-Object {
        ($_ -split "`t")[0]
    }
    $clavesOrdenadas = $claves | Sort-Object
    
    $ordenCorrecto = $true
    for ($i = 0; $i -lt $claves.Count; $i++) {
        if ($claves[$i] -ne $clavesOrdenadas[$i]) {
            $ordenCorrecto = $false
            break
        }
    }
    
    if ($ordenCorrecto) {
        Write-Host "`n✅ Productos ordenados alfabéticamente" -ForegroundColor Green
    } else {
        Write-Host "`n❌ Productos NO están ordenados" -ForegroundColor Red
    }
    
    # Información del archivo
    $fileInfo = Get-Item $filePath
    Write-Host "`n📁 Información del archivo:" -ForegroundColor Yellow
    Write-Host "   Tamaño: $($fileInfo.Length) bytes"
    Write-Host "   Creado: $($fileInfo.CreationTime)"
    Write-Host "   Modificado: $($fileInfo.LastWriteTime)"
    
} else {
    Write-Host "❌ Archivo no encontrado: $filePath" -ForegroundColor Red
}
```

### Ejecución del Script de Validación

```powershell
# Guardar el script anterior como validate-file.ps1
.\validate-file.ps1
```

**Salida Esperada:**
```
✅ Archivo encontrado: C:\Sistemas\SIGMA\Documentos\Existencias_Diciembre2016.txt
✅ Encabezado correcto
📊 Total de productos: 150

📋 Primeros 5 productos:
PROD001    Tornillo M8 x 20mm         1500
PROD002    Tuerca M8                  2000
PROD003    Arandela plana M8          3500
PROD004    Pintura azul 1L            125
PROD005    Aceite motor SAE 10W-40    450

✅ Productos ordenados alfabéticamente

📁 Información del archivo:
   Tamaño: 8547 bytes
   Creado: 16/01/2026 10:30:15
   Modificado: 16/01/2026 10:30:15
```

---

## 📊 Script de Pruebas Automatizadas

### Bash Script para Linux/Mac

```bash
#!/bin/bash

# Variables
BASE_URL="http://localhost:8080"
TOKEN=""

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🧪 Iniciando pruebas de API - Generar Archivo"
echo "============================================="

# Paso 1: Login
echo -e "\n${YELLOW}1. Obteniendo token de autenticación...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/sigmav2/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tokai.com.mx","password":"password123"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')

if [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
    echo -e "${GREEN}✅ Token obtenido exitosamente${NC}"
else
    echo -e "${RED}❌ Error al obtener token${NC}"
    exit 1
fi

# Paso 2: Generar archivo exitoso
echo -e "\n${YELLOW}2. Generando archivo (caso exitoso)...${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/sigmav2/labels/generate-file" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"periodId":16}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✅ Archivo generado exitosamente${NC}"
    echo "$BODY" | jq '.'
else
    echo -e "${RED}❌ Error al generar archivo (HTTP $HTTP_CODE)${NC}"
    echo "$BODY"
fi

# Paso 3: Periodo inexistente
echo -e "\n${YELLOW}3. Probando periodo inexistente...${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/sigmav2/labels/generate-file" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"periodId":99999}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "500" ] || [ "$HTTP_CODE" == "404" ]; then
    echo -e "${GREEN}✅ Error manejado correctamente (HTTP $HTTP_CODE)${NC}"
else
    echo -e "${RED}❌ Error inesperado (HTTP $HTTP_CODE)${NC}"
fi

# Paso 4: Sin token
echo -e "\n${YELLOW}4. Probando sin token...${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/sigmav2/labels/generate-file" \
  -H "Content-Type: application/json" \
  -d '{"periodId":16}')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" == "401" ]; then
    echo -e "${GREEN}✅ Autenticación requerida correctamente (HTTP $HTTP_CODE)${NC}"
else
    echo -e "${RED}❌ Error inesperado (HTTP $HTTP_CODE)${NC}"
fi

echo -e "\n${GREEN}=============================================${NC}"
echo -e "${GREEN}Pruebas completadas${NC}"
```

---

## 📝 Notas Finales

### ✅ Checklist de Pruebas

Antes de considerar las pruebas como completas, verifique:

- [ ] Prueba exitosa ejecutada y archivo generado
- [ ] Archivo existe en la ubicación correcta
- [ ] Formato del archivo es correcto
- [ ] Productos ordenados alfabéticamente
- [ ] Existencias calculadas correctamente
- [ ] Error 401 (sin token) funciona
- [ ] Error 403 (sin permisos) funciona
- [ ] Error 404/500 (periodo inexistente) funciona
- [ ] Error 400 (validación) funciona
- [ ] Sobrescritura de archivo funciona

---

**© 2026 Tokai - Sistema SIGMA V2**
