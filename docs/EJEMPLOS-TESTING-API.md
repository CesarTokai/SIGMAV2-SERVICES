# 🧪 Ejemplos de Testing - API Catálogo de Inventario

## Configuración Previa

### 1. Obtener Token de Autenticación

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "tu_password"
  }'
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "admin",
  "roles": ["ROLE_ADMIN"]
}
```

**Guardar el token para usarlo en los siguientes requests:**
```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 📡 Endpoints del Catálogo de Inventario

### 1. Obtener Último Periodo

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/latest-period" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada:**
```json
{
  "id": 12,
  "date": "2024-01-01",
  "comments": "Periodo enero 2024",
  "state": "OPEN"
}
```

---

### 2. Listar Todos los Periodos

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/all-periods" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada:**
```json
[
  {
    "id": 10,
    "date": "2023-11-01",
    "comments": "Periodo noviembre 2023",
    "state": "CLOSED"
  },
  {
    "id": 11,
    "date": "2023-12-01",
    "comments": "Periodo diciembre 2023",
    "state": "CLOSED"
  },
  {
    "id": 12,
    "date": "2024-01-01",
    "comments": "Periodo enero 2024",
    "state": "OPEN"
  }
]
```

---

### 3. Consultar Inventario (Primera Página, 10 Registros)

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12&page=0&size=10&sort=cveArt,asc" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada:**
```json
{
  "content": [
    {
      "cveArt": "PROD001",
      "descr": "Tornillo 1/4 x 2",
      "uniMed": "PZA",
      "existQty": 1500.00,
      "status": "A"
    },
    {
      "cveArt": "PROD002",
      "descr": "Tuerca 1/4",
      "uniMed": "PZA",
      "existQty": 2000.00,
      "status": "A"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 15,
  "totalElements": 150,
  "last": false,
  "first": true,
  "size": 10,
  "number": 0,
  "numberOfElements": 10,
  "empty": false
}
```

---

### 4. Buscar Productos con "BOLT"

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12&search=BOLT&page=0&size=25" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada:**
```json
{
  "content": [
    {
      "cveArt": "BOLT001",
      "descr": "Perno hexagonal 1/2",
      "uniMed": "PZA",
      "existQty": 500.00,
      "status": "A"
    },
    {
      "cveArt": "BOLT002",
      "descr": "Perno allen 3/8",
      "uniMed": "PZA",
      "existQty": 750.00,
      "status": "A"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "size": 25,
  "number": 0
}
```

---

### 5. Ordenar por Existencias (Descendente)

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12&page=0&size=10&sort=existQty,desc" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta:** Productos ordenados de mayor a menor existencia

---

### 6. Filtrar por Almacén Específico

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12&warehouseId=5&page=0&size=50" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta:** Solo productos del almacén ID=5

---

### 7. Paginación - Segunda Página con 25 Registros

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12&page=1&size=25&sort=descr,asc" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta:** Registros 26-50 ordenados alfabéticamente por descripción

---

### 8. Búsqueda + Ordenación + Paginación

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12&search=tornillo&page=0&size=10&sort=existQty,desc" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta:** Productos que contienen "tornillo", ordenados por existencias descendente, primera página de 10

---

## 🔬 Casos de Prueba

### Test Case 1: Periodo Sin Datos

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=999&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado Esperado:**
```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "empty": true
}
```

---

### Test Case 2: Búsqueda Sin Resultados

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12&search=PRODUCTO_INEXISTENTE" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado Esperado:**
```json
{
  "content": [],
  "totalElements": 0
}
```

---

### Test Case 3: Sin Token (Error de Autenticación)

```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12"
```

**Resultado Esperado:**
```
HTTP 401 Unauthorized
{
  "error": "Unauthorized",
  "message": "Full authentication is required"
}
```

---

### Test Case 4: Usuario Sin Rol Admin (Error de Autorización)

```bash
# Login como usuario normal
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "password"}'

# Intentar acceder al inventario
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12" \
  -H "Authorization: Bearer $USER_TOKEN"
```

**Resultado Esperado:**
```
HTTP 403 Forbidden
{
  "error": "Forbidden",
  "message": "Access Denied"
}
```

---

## 🎯 Tests Funcionales con Postman

### Colección Postman

```json
{
  "info": {
    "name": "SIGMA - Catálogo Inventario",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Login",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"username\": \"admin\",\n  \"password\": \"password\"\n}"
        },
        "url": "http://localhost:8080/api/auth/login"
      }
    },
    {
      "name": "2. Get Latest Period",
      "request": {
        "method": "GET",
        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
        "url": "http://localhost:8080/api/sigmav2/inventory/latest-period"
      }
    },
    {
      "name": "3. Get Inventory Report",
      "request": {
        "method": "GET",
        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
        "url": {
          "raw": "http://localhost:8080/api/sigmav2/inventory/period-report",
          "query": [
            {"key": "periodId", "value": "12"},
            {"key": "page", "value": "0"},
            {"key": "size", "value": "10"},
            {"key": "sort", "value": "cveArt,asc"}
          ]
        }
      }
    }
  ]
}
```

---

## 📊 Testing de Rendimiento

### Test de Carga con Apache Bench

```bash
# 100 requests, 10 concurrentes
ab -n 100 -c 10 \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12&page=0&size=10"
```

**Métricas a observar:**
- Time per request
- Requests per second
- Transfer rate
- Failed requests (debe ser 0)

---

## 🐛 Debugging

### Verificar Logs del Servidor

```bash
tail -f logs/spring.log | grep "inventory"
```

### Habilitar Debug SQL

En `application.properties`:
```properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

---

## ✅ Checklist de Testing

### Funcionalidad
- [ ] Login exitoso con admin
- [ ] Obtener último periodo
- [ ] Listar todos los periodos
- [ ] Consultar inventario sin filtros
- [ ] Buscar por clave de producto
- [ ] Buscar por nombre de producto
- [ ] Buscar por unidad de medida
- [ ] Ordenar ascendente
- [ ] Ordenar descendente
- [ ] Paginación (primera, última, anterior, siguiente)
- [ ] Cambiar tamaño de página
- [ ] Filtrar por almacén

### Seguridad
- [ ] Rechaza requests sin token
- [ ] Rechaza requests de usuarios sin rol admin
- [ ] Token expirado retorna 401

### Performance
- [ ] Respuesta < 500ms con 100 productos
- [ ] Respuesta < 1s con 1000 productos
- [ ] Sin memory leaks después de 1000 requests

### UI/UX
- [ ] Carga automática del último periodo
- [ ] Búsqueda con debounce funciona
- [ ] Indicadores de ordenación visibles
- [ ] Paginación funcional
- [ ] Estados de carga mostrados
- [ ] Errores manejados correctamente

---

**Fecha:** 24 de Noviembre de 2025
**Versión API:** 1.0
**Base URL:** http://localhost:8080

