# GUIA DE PRUEBAS DEL SISTEMA SIGMAV2

## Requisitos previos

- Servidor corriendo en `http://localhost:8080`
- Herramienta para pruebas REST: **Postman**, **Insomnia** o **curl**
- Base de datos conectada y con datos iniciales

---

## PASO 1 - Verificar que el servidor esta activo

```
GET http://localhost:8080/api/sigmav2/auth/health
```

Respuesta esperada:
```json
{
  "data": {
    "status": "OK",
    "message": "API funcionando correctamente"
  }
}
```

---

## PASO 2 - Autenticacion (Login)

Todos los endpoints requieren un token JWT. Primero debes hacer login.

```
POST http://localhost:8080/api/sigmav2/auth/login
Content-Type: application/json

{
  "email": "tu_usuario@tokai.com.mx",
  "password": "tu_contrasena"
}
```

Respuesta:
```json
{
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "email": "tu_usuario@tokai.com.mx",
    "role": "ADMINISTRADOR"
  }
}
```

Guarda el valor de `token`. Lo usaras en todos los demas endpoints como:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

---

## PASO 3 - Gestionar Periodos

### 3.1 Listar todos los periodos

```
GET http://localhost:8080/api/sigmav2/inventory/all-periods
Authorization: Bearer {TOKEN}
```

### 3.2 Crear un periodo nuevo (solo ADMINISTRADOR)

```
POST http://localhost:8080/api/sigmav2/periods
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "date": "2026-03-01",
  "comments": "Periodo Marzo 2026"
}
```

Respuesta:
```json
{
  "id": 8,
  "date": "2026-03-01",
  "comments": "Periodo Marzo 2026",
  "state": "OPEN"
}
```

Guarda el `id` del periodo (ejemplo: `8`). Lo usaras en los siguientes pasos.

### 3.3 Obtener un periodo especifico

```
GET http://localhost:8080/api/sigmav2/periods/8
Authorization: Bearer {TOKEN}
```

### 3.4 Listar periodos con paginacion

```
GET http://localhost:8080/api/sigmav2/periods?page=0&size=10
Authorization: Bearer {TOKEN}
```

---

## PASO 4 - Consultar Almacenes y Productos

### 4.1 Listar almacenes

```
GET http://localhost:8080/api/sigmav2/inventory/warehouses
Authorization: Bearer {TOKEN}
```

Respuesta de ejemplo:
```json
[
  { "id": 254, "warehouseKey": "1", "nameWarehouse": "Almacen 1" },
  { "id": 255, "warehouseKey": "2", "nameWarehouse": "Almacen 2" }
]
```

Guarda el `id` del almacen que quieres usar (ejemplo: `254` para Almacen 1).

### 4.2 Listar productos

```
GET http://localhost:8080/api/sigmav2/inventory/products
Authorization: Bearer {TOKEN}
```

---

## PASO 5 - Importar Inventario Inicial

Si es la primera vez que usas el periodo, necesitas importar las existencias teoricas desde un archivo Excel/CSV.

```
POST http://localhost:8080/api/sigmav2/inventory/import
Authorization: Bearer {TOKEN}
Content-Type: multipart/form-data

periodId: 8
warehouseId: 254
file: [selecciona tu archivo Excel]
```

---

## PASO 6 - Generar Marbetes

### 6.1 Verificar cuantos marbetes hay pendientes de impresion

```
POST http://localhost:8080/api/sigmav2/labels/pending-print-count
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "periodId": 8,
  "warehouseId": 254
}
```

Respuesta:
```json
{
  "count": 44,
  "warehouseId": 254,
  "periodId": 8
}
```

### 6.2 Generar marbetes para una lista de productos

```
POST http://localhost:8080/api/sigmav2/labels/generate/batch
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "periodId": 8,
  "warehouseId": 254,
  "products": [
    { "productId": 5050 },
    { "productId": 5051 }
  ]
}
```

### 6.3 Generar e imprimir marbetes en un solo paso

```
POST http://localhost:8080/api/sigmav2/labels/generate-and-print
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "periodId": 8,
  "warehouseId": 254,
  "products": [
    { "productId": 5050 }
  ]
}
```

La respuesta sera un archivo PDF descargable.

---

## PASO 7 - Imprimir Marbetes

Si los marbetes ya fueron generados y quieres imprimirlos:

```
POST http://localhost:8080/api/sigmav2/labels/print
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "periodId": 8,
  "warehouseId": 254
}
```

La respuesta sera un PDF con todos los marbetes pendientes de impresion del almacen.

---

## PASO 8 - Registrar Conteos

### 8.1 Ver marbetes disponibles para contar

```
POST http://localhost:8080/api/sigmav2/labels/for-count/list
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "periodId": 8,
  "warehouseId": 254
}
```

### 8.2 Consultar un marbete especifico

```
GET http://localhost:8080/api/sigmav2/labels/for-count?folio=44&periodId=8&warehouseId=254
Authorization: Bearer {TOKEN}
```

Respuesta de ejemplo:
```json
{
  "folio": 44,
  "claveProducto": "GM17CRTB8",
  "descripcionProducto": "CARTUCHO P/ANT. GM17",
  "claveAlmacen": "1",
  "nombreAlmacen": "Almacen 1",
  "estado": "IMPRESO",
  "conteo1": null,
  "conteo2": null,
  "mensaje": "Pendiente de primer conteo"
}
```

### 8.3 Registrar Primer Conteo (C1)

```
POST http://localhost:8080/api/sigmav2/labels/counts/c1
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "folio": 44,
  "periodId": 8,
  "warehouseId": 254,
  "countedValue": 125
}
```

### 8.4 Registrar Segundo Conteo (C2)

```
POST http://localhost:8080/api/sigmav2/labels/counts/c2
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "folio": 44,
  "periodId": 8,
  "warehouseId": 254,
  "countedValue": 125
}
```

### 8.5 Actualizar un conteo ya registrado

```
PUT http://localhost:8080/api/sigmav2/labels/counts/c1
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "folio": 44,
  "periodId": 8,
  "warehouseId": 254,
  "countedValue": 130
}
```

---

## PASO 9 - Cancelar un Marbete (si es necesario)

```
POST http://localhost:8080/api/sigmav2/labels/cancel
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "folio": 1,
  "periodId": 8,
  "warehouseId": 254,
  "motivo": "Marbete incorrecto - producto duplicado"
}
```

---

## PASO 10 - Consultar Estado de Marbetes

### Ver estado de un marbete especifico

```
GET http://localhost:8080/api/sigmav2/labels/status?folio=44&periodId=8&warehouseId=254
Authorization: Bearer {TOKEN}
```

### Ver marbetes cancelados del almacen

```
GET http://localhost:8080/api/sigmav2/labels/cancelled?periodId=8&warehouseId=254
Authorization: Bearer {TOKEN}
```

---

## PASO 11 - Reportes JSON (datos)

Todos los reportes reciben el mismo body base:

```json
{
  "periodId": 8,
  "warehouseId": 254
}
```

Si omites `warehouseId` (o pones `null`), obtiene todos los almacenes accesibles.

| Reporte | Endpoint |
|---------|----------|
| Distribucion de marbetes | `POST /api/sigmav2/labels/reports/distribution` |
| Listado completo | `POST /api/sigmav2/labels/reports/list` |
| Marbetes pendientes | `POST /api/sigmav2/labels/reports/pending` |
| Marbetes con diferencias | `POST /api/sigmav2/labels/reports/with-differences` |
| Marbetes cancelados | `POST /api/sigmav2/labels/reports/cancelled` |
| Comparativo fisico vs teorico | `POST /api/sigmav2/labels/reports/comparative` |
| Inventario por almacen con detalle | `POST /api/sigmav2/labels/reports/warehouse-detail` |
| Inventario por producto con detalle | `POST /api/sigmav2/labels/reports/product-detail` |

### Ejemplo - Inventario por almacen con detalle:

```
POST http://localhost:8080/api/sigmav2/labels/reports/warehouse-detail
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "periodId": 8,
  "warehouseId": 254
}
```

### Ejemplo - Inventario por producto (todos los almacenes):

```
POST http://localhost:8080/api/sigmav2/labels/reports/product-detail
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "periodId": 8
}
```

---

## PASO 12 - Reportes en PDF

Los mismos reportes pero en formato PDF. Solo agrega `/pdf` al final del endpoint.

| Reporte | Endpoint PDF |
|---------|-------------|
| Distribucion | `POST /api/sigmav2/labels/reports/distribution/pdf` |
| Listado | `POST /api/sigmav2/labels/reports/list/pdf` |
| Pendientes | `POST /api/sigmav2/labels/reports/pending/pdf` |
| Diferencias | `POST /api/sigmav2/labels/reports/with-differences/pdf` |
| Cancelados | `POST /api/sigmav2/labels/reports/cancelled/pdf` |
| Comparativo | `POST /api/sigmav2/labels/reports/comparative/pdf` |
| Almacen con detalle | `POST /api/sigmav2/labels/reports/warehouse-detail/pdf` |
| Todos los almacenes | `POST /api/sigmav2/labels/reports/warehouse-detail/all/pdf` |
| Producto con detalle | `POST /api/sigmav2/labels/reports/product-detail/pdf` |

### Ejemplo - PDF de todos los almacenes:

```
POST http://localhost:8080/api/sigmav2/labels/reports/warehouse-detail/all/pdf
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "periodId": 8
}
```

La respuesta es un archivo PDF descargable.

---

## PASO 13 - Generar Archivo TXT de Existencias

Genera un archivo de texto con las existencias finales para exportar al sistema ERP.

```
POST http://localhost:8080/api/sigmav2/labels/generate-file
Authorization: Bearer {TOKEN}
Content-Type: application/json

{
  "periodId": 8
}
```

La respuesta es un archivo `.txt` descargable con formato:
```
CLAVE_PRODUCTO    DESCRIPCION                        EXISTENCIAS
GM17CRTB8         CARTUCHO P/ANT. GM17               125
GM17CRTC1         CARTUCHO EMP. P/ANT. GM17 1CJA     659
```

---

## PASO 14 - Cerrar el Periodo (solo ADMINISTRADOR)

Cuando el inventario fisico este completo:

### Cerrar periodo

```
PUT http://localhost:8080/api/sigmav2/periods/8/close
Authorization: Bearer {TOKEN}
```

### Bloquear periodo (impide modificaciones)

```
PUT http://localhost:8080/api/sigmav2/periods/8/lock
Authorization: Bearer {TOKEN}
```

---

## PASO 15 - Logout

```
POST http://localhost:8080/api/auth/logout
Authorization: Bearer {TOKEN}
```

---

## Flujo completo resumido

```
1. Login                     → Obtener TOKEN
2. Crear periodo             → Obtener periodId
3. Importar inventario       → Cargar existencias teoricas
4. Generar marbetes          → Crear marbetes por almacen
5. Imprimir marbetes         → PDF para el equipo de conteo
6. Registrar C1              → Primer conteo fisico
7. Registrar C2              → Segundo conteo fisico (confirmacion)
8. Revisar diferencias       → Reporte comparativo C1 vs C2
9. Generar reportes          → PDF o JSON de los resultados
10. Generar archivo TXT      → Exportar existencias al ERP
11. Cerrar periodo           → Finalizar inventario
```

---

## Roles del sistema

| Rol | Permisos |
|-----|----------|
| ADMINISTRADOR | Acceso total |
| AUXILIAR | Marbetes, conteos, reportes de todos los almacenes |
| ALMACENISTA | Marbetes, conteos, reportes de su almacen |
| AUXILIAR_DE_CONTEO | Solo registrar conteos |

---

## Notas importantes

- El `periodId` debe corresponder a un periodo en estado `OPEN`
- El `warehouseId` es el ID numerico del almacen en la base de datos (no la clave)
- Los conteos deben registrarse en orden: C1 primero, C2 despues
- Un marbete cancelado no se incluye en el calculo de existencias
- Los reportes PDF pueden tardarse unos segundos si hay muchos marbetes

