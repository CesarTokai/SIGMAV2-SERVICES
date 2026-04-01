# APIs REST para Aplicación Móvil Flutter - SIGMAV2

**Versión:** 1.0.0  
**Fecha:** 2026-03-24  
**Base URL:** `http://[TU_IP_SERVIDOR]:8080/api/sigmav2`  
**Documentación Base:** Consultar `SETUP-FLUTTER-APP-ESCANEO-MARBETES.md`

---

## 📋 Índice de Endpoints

1. [Autenticación (Auth)](#autenticación-auth)
2. [Consulta de Marbetes (Labels)](#consulta-de-marbetes-labels)
3. [Registrar Conteos (Counts)](#registrar-conteos-counts)
4. [Almacenes (Warehouses)](#almacenes-warehouses)
5. [Usuarios (Users)](#usuarios-users)
6. [Períodos (Periods)](#períodos-periods)
7. [Códigos de Error](#códigos-de-error)

---

## 🔐 Autenticación (Auth)

### **POST /auth/login**
Iniciar sesión y obtener JWT token.

**Request:**
```json
{
  "email": "usuario@tokai.com",
  "password": "Tu_Contraseña_123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "email": "usuario@tokai.com",
      "nombreCompleto": "Juan Pérez",
      "role": "ALMACENISTA",
      "almacen": "ALMACEN_A"
    },
    "expiresIn": 3600
  },
  "message": "Inicio de sesión exitoso"
}
```

**Response (401 Unauthorized):**
```json
{
  "success": false,
  "error": "LOGIN_FAILED",
  "message": "Error de autenticación",
  "detail": "Email o contraseña incorrectos"
}
```

**Headers necesarios:**
```
Content-Type: application/json
```

**Guardar token en Flutter:**
```dart
// Usar SharedPreferences
final prefs = await SharedPreferences.getInstance();
await prefs.setString('jwt_token', response['data']['token']);
```

---

### **POST /auth/logout**
Cerrar sesión e invalidar token.

**Request Headers:**
```
Authorization: Bearer <TU_JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Sesión cerrada exitosamente"
}
```

---

## 📦 Consulta de Marbetes (Labels)

> **Importante:** El escaneo QR devuelve el `folio` (número del marbete). Este es tu identificador principal.

### **GET /labels/by-folio/{folio}**
Obtener información completa de un marbete por su número (lo que escanea el QR).

**Path Parameters:**
- `folio` (Long): Número del marbete (ej: 42)

**Request Headers:**
```
Authorization: Bearer <TU_JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 105,
    "folio": 42,
    "numeroMarbete": 42,
    "producto": "Widget Azul Premium",
    "claveProducto": "WDGT-001",
    "descripcionProducto": "Widget de color azul con acabado premium",
    "stockTeorico": 150,
    "almacen": "ALMACEN_A",
    "claveAlmacen": "ALM001",
    "nombreAlmacen": "Almacén Central",
    "estado": "ACTIVO",
    "periodo": {
      "id": 1,
      "nombre": "Inventario Marzo 2026",
      "fechaInicio": "2026-03-01",
      "fechaFin": "2026-03-31"
    },
    "qrData": "42",
    "fechaCreacion": "2026-03-24T10:30:00",
    "countC1": null,
    "countC2": null,
    "diasDesdeCreacion": 0
  },
  "message": "Marbete encontrado"
}
```

**Response (404 Not Found):**
```json
{
  "success": false,
  "error": "NOT_FOUND",
  "message": "Marbete no encontrado",
  "detail": "No existe marbete con folio: 42"
}
```

---

### **GET /labels/for-count**
Obtener marbete específico para registrar conteo (incluye validaciones).

**Query Parameters:**
- `folio` (Long): Número del marbete
- `periodId` (Long): ID del período
- `warehouseId` (Long): ID del almacén

**Request Headers:**
```
Authorization: Bearer <TU_JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "folio": 42,
    "producto": "Widget Azul Premium",
    "claveProducto": "WDGT-001",
    "almacen": "ALMACEN_A",
    "stockTeorico": 150,
    "countC1": null,
    "countC2": null,
    "estado": "ACTIVO",
    "mensaje": "Listo para conteo C1"
  },
  "message": "Marbete válido para conteo"
}
```

---

### **POST /labels/for-count/list**
Obtener listado de marbetes listos para contar (por almacén y período).

**Request Body:**
```json
{
  "periodId": 1,
  "warehouseId": 5
}
```

**Request Headers:**
```
Authorization: Bearer <TU_JWT_TOKEN>
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "folio": 40,
      "producto": "Widget Rojo",
      "claveProducto": "WDGT-002",
      "almacen": "ALMACEN_A",
      "stockTeorico": 200,
      "countC1": null,
      "countC2": null,
      "estado": "ACTIVO"
    },
    {
      "folio": 42,
      "producto": "Widget Azul Premium",
      "claveProducto": "WDGT-001",
      "almacen": "ALMACEN_A",
      "stockTeorico": 150,
      "countC1": 148,
      "countC2": null,
      "estado": "ACTIVO"
    }
  ],
  "message": "Listado de marbetes para conteo"
}
```

---

## 📊 Registrar Conteos (Counts)

> **Flujo de Conteo:**
> 1. Escanear QR → Obtienes `folio`
> 2. GET `/labels/by-folio/{folio}` → Validas el marbete
> 3. POST `/labels/counts/c1` → Registras primer conteo
> 4. POST `/labels/counts/c2` → Registras segundo conteo (después que C1 esté completo)

### **POST /labels/counts/c1**
Registrar primer conteo físico (C1) de un marbete.

**Request Body:**
```json
{
  "folio": 42,
  "countValue": 148,
  "periodId": 1,
  "warehouseId": 5
}
```

**Request Headers:**
```
Authorization: Bearer <TU_JWT_TOKEN>
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 501,
    "folio": 42,
    "countC1": 148,
    "countC2": null,
    "estado": "C1_REGISTRADO",
    "mensaje": "Conteo C1 registrado. Stock teórico: 150, Contado: 148, Diferencia: -2",
    "diferencia": -2,
    "porcentajeDiferencia": -1.33,
    "timestampConteo": "2026-03-24T15:45:30"
  },
  "message": "Conteo C1 registrado exitosamente"
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "error": "INVALID_REQUEST",
  "message": "Error en validación",
  "detail": "El conteo debe ser un número mayor o igual a 0"
}
```

**Response (409 Conflict):**
```json
{
  "success": false,
  "error": "CONFLICT",
  "message": "Error de estado",
  "detail": "El marbete ya tiene C1 registrado. Use PUT para actualizar."
}
```

---

### **POST /labels/counts/c2**
Registrar segundo conteo físico (C2) de un marbete.

> **Requisito:** El marbete debe tener C1 ya registrado antes de registrar C2.

**Request Body:**
```json
{
  "folio": 42,
  "countValue": 149,
  "periodId": 1,
  "warehouseId": 5
}
```

**Request Headers:**
```
Authorization: Bearer <TU_JWT_TOKEN>
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 502,
    "folio": 42,
    "countC1": 148,
    "countC2": 149,
    "estado": "C1_C2_REGISTRADOS",
    "mensaje": "Conteo C2 registrado. C1: 148, C2: 149, Diferencia C1-C2: 1",
    "diferenciaC1C2": 1,
    "diferenciaTeorica": -1,
    "timestampConteo": "2026-03-24T16:10:30"
  },
  "message": "Conteo C2 registrado exitosamente"
}
```

---

### **PUT /labels/counts/c1**
Actualizar (corregir) un conteo C1 ya registrado.

**Request Body:**
```json
{
  "folio": 42,
  "countValue": 150,
  "periodId": 1,
  "warehouseId": 5
}
```

**Request Headers:**
```
Authorization: Bearer <TU_JWT_TOKEN>
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 501,
    "folio": 42,
    "countC1": 150,
    "countC2": null,
    "estado": "C1_ACTUALIZADO",
    "mensaje": "Conteo C1 actualizado exitosamente"
  },
  "message": "Conteo C1 actualizado"
}
```

---

### **PUT /labels/counts/c2**
Actualizar (corregir) un conteo C2 ya registrado.

**Request Body:**
```json
{
  "folio": 42,
  "countValue": 148,
  "periodId": 1,
  "warehouseId": 5
}
```

**Response (200 OK):** Similar a C1 con estado `C2_ACTUALIZADO`

---

## 🏢 Almacenes (Warehouses)

### **GET /warehouse**
Listar todos los almacenes disponibles.

**Request Headers:**
```
Authorization: Bearer <TU_JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "nombre": "Almacén Central",
      "clave": "ALM001",
      "ubicacion": "México DF",
      "activo": true
    },
    {
      "id": 5,
      "nombre": "Almacén Regional",
      "clave": "ALM005",
      "ubicacion": "Guadalajara",
      "activo": true
    }
  ],
  "message": "Almacenes obtenidos"
}
```

---

### **GET /warehouse/{warehouseId}**
Obtener detalles de un almacén específico.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 5,
    "nombre": "Almacén Regional",
    "clave": "ALM005",
    "ubicacion": "Guadalajara",
    "activo": true,
    "responsable": "Carlos López",
    "telefono": "+52 33 3333 3333"
  },
  "message": "Almacén encontrado"
}
```

---

## 👤 Usuarios (Users)

### **GET /users/me**
Obtener información del usuario autenticado.

**Request Headers:**
```
Authorization: Bearer <TU_JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "usuario@tokai.com",
    "nombreCompleto": "Juan Pérez",
    "role": "ALMACENISTA",
    "almacen": "ALMACEN_A",
    "almacenId": 5,
    "activo": true,
    "lastLoginAt": "2026-03-24T10:00:00",
    "lastActivityAt": "2026-03-24T15:45:30"
  },
  "message": "Información de usuario obtenida"
}
```

---

## 📅 Períodos (Periods)

### **GET /periods**
Listar todos los períodos disponibles.

**Request Headers:**
```
Authorization: Bearer <TU_JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "nombre": "Inventario Marzo 2026",
      "fechaInicio": "2026-03-01",
      "fechaFin": "2026-03-31",
      "estado": "ACTIVO",
      "activo": true
    },
    {
      "id": 2,
      "nombre": "Inventario Abril 2026",
      "fechaInicio": "2026-04-01",
      "fechaFin": "2026-04-30",
      "estado": "PLANIFICADO",
      "activo": false
    }
  ],
  "message": "Períodos obtenidos"
}
```

---

### **GET /periods/active**
Obtener el período activo actualmente.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "nombre": "Inventario Marzo 2026",
    "fechaInicio": "2026-03-01",
    "fechaFin": "2026-03-31",
    "estado": "ACTIVO"
  },
  "message": "Período activo obtenido"
}
```

---

## ⚠️ Códigos de Error

| Código HTTP | Código Error | Descripción |
|-------------|-------------|-------------|
| **200** | - | Solicitud exitosa |
| **201** | - | Recurso creado exitosamente |
| **400** | `INVALID_REQUEST` | Datos de entrada inválidos |
| **401** | `UNAUTHORIZED` | Token expirado o no proporcionado |
| **403** | `FORBIDDEN` | Usuario no tiene permisos |
| **404** | `NOT_FOUND` | Recurso no encontrado |
| **409** | `CONFLICT` | Conflicto de estado (ej: C1 ya existe) |
| **500** | `INTERNAL_ERROR` | Error del servidor |

---

## 🔄 Flujo Completo en Flutter

### 1. **Login**
```dart
final response = await apiService.login('usuario@tokai.com', 'contraseña');
// Guardar token
await storageService.saveToken(response['token']);
```

### 2. **Escanear QR**
```dart
// El QR contiene solo el número: "42"
String folioEscaneado = "42";
```

### 3. **Obtener Detalles del Marbete**
```dart
final marbete = await apiService.getMarbete(int.parse(folioEscaneado));
// Mostrar detalles: producto, stock teórico, almacén, etc.
```

### 4. **Registrar Conteo C1**
```dart
await apiService.registerCountC1(
  labelId: marbete['data']['id'],
  countValue: 148,
  periodId: 1,
  warehouseId: 5,
);
```

### 5. **Registrar Conteo C2**
```dart
await apiService.registerCountC2(
  labelId: marbete['data']['id'],
  countValue: 149,
  periodId: 1,
  warehouseId: 5,
);
```

---

## 🔐 Seguridad en Mobile

### **Guardar Token con Expiración**
```dart
// En StorageService
Future<void> saveToken(String token) async {
  final prefs = await SharedPreferences.getInstance();
  await prefs.setString('jwt_token', token);
  await prefs.setInt('token_timestamp', DateTime.now().millisecondsSinceEpoch);
}

Future<String?> getValidToken() async {
  final prefs = await SharedPreferences.getInstance();
  final token = prefs.getString('jwt_token');
  final timestamp = prefs.getInt('token_timestamp') ?? 0;
  
  final now = DateTime.now().millisecondsSinceEpoch;
  final elapsed = now - timestamp;
  final expiresIn = 3600 * 1000; // 1 hora
  
  if (elapsed > expiresIn) {
    // Token expirado, limpiar
    await prefs.remove('jwt_token');
    return null;
  }
  return token;
}
```

### **Interceptor JWT**
```dart
_dio.interceptors.add(
  InterceptorsWrapper(
    onRequest: (options, handler) async {
      final token = await storageService.getValidToken();
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      } else {
        // Redirigir a login si token expiró
        navigateToLogin();
      }
      return handler.next(options);
    },
  ),
);
```

---

## 📝 Notas Importantes

1. **Headers Obligatorios:** Todas las requests autenticadas requieren `Authorization: Bearer <token>`
2. **Content-Type:** Siempre usar `application/json` para requests con body
3. **Folio vs ID:** El `folio` es lo que escanea el QR. El `id` es el identificador interno de BD.
4. **Período y Almacén:** Son requeridos en muchos endpoints para validar acceso del usuario
5. **Manejo de Errores:** Verificar siempre `success: true/false` antes de usar `data`
6. **Rate Limiting:** El servidor puede limitar requests. Implementar retry con backoff exponencial
7. **Validación de QR:** El QR debe contener solo números (el folio)

---

## 📞 Contacto y Soporte

**Desarrollado por:** Cesar Uriel Gonzalez Saldaña  
**Empresa:** Tokai de México  
**Versión API:** 1.0.0  
**Última actualización:** 2026-03-24

Para actualizar esta documentación, consulta con el equipo backend de SIGMAV2.

