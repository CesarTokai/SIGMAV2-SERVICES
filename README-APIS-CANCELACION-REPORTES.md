# 📚 Documentación de APIs - Cancelación y Reportes de Marbetes

## 🎯 Resumen

Se han implementado exitosamente:
- ✅ **1 endpoint** de cancelación de marbetes
- ✅ **8 endpoints** de reportes de marbetes
- ✅ **9 endpoints** en total
- ✅ Documentación completa de APIs
- ✅ Colección de Postman
- ✅ Scripts de prueba automatizados

---

## 📁 Archivos de Documentación Disponibles

### 1. **Documentación Principal**

#### `docs/API-CANCELACION-REPORTES-MARBETES.md`
📖 **Documentación completa y detallada de todas las APIs**

**Contenido**:
- Descripción detallada de cada endpoint
- Request/Response bodies con ejemplos completos
- Códigos de error y manejo
- Ejemplos con cURL, JavaScript/Fetch y Axios
- Ejemplos de integración para React/Vue
- Mejores prácticas y consideraciones de performance
- Reglas de negocio y permisos por rol

**Cuándo usar**: Referencia completa para desarrolladores frontend y backend

---

#### `QUICK-API-REFERENCE.md`
⚡ **Referencia rápida de todos los endpoints**

**Contenido**:
- Lista compacta de todos los endpoints
- Request/Response simplificados
- Ejemplos rápidos con cURL
- Tabla de casos de uso
- Tabla de permisos por rol

**Cuándo usar**: Consulta rápida durante el desarrollo

---

#### `IMPLEMENTACION-CANCELACION-REPORTES-MARBETES.md`
🔧 **Documentación técnica de la implementación**

**Contenido**:
- Arquitectura de la implementación
- Clases y métodos creados
- Reglas de negocio cumplidas
- Estructura de DTOs
- Estado de la compilación
- Próximos pasos recomendados

**Cuándo usar**: Para entender la implementación técnica

---

### 2. **Herramientas de Prueba**

#### `postman/SIGMAV2-Cancelacion-Reportes-Marbetes.postman_collection.json`
📮 **Colección de Postman completa**

**Contenido**:
- 18 requests pre-configuradas
- Variables de entorno configurables
- Autenticación con Bearer Token
- Organizado por categorías (Cancelación, Reportes)
- Variantes para "todos los almacenes" y "almacén específico"

**Cómo importar**:
1. Abrir Postman
2. Click en "Import"
3. Seleccionar el archivo JSON
4. Configurar las variables: `token`, `periodId`, `warehouseId`

---

#### `test-api-cancelacion-reportes.ps1`
🔬 **Script interactivo de pruebas en PowerShell**

**Características**:
- Menú interactivo para seleccionar pruebas
- Configuración de variables (token, periodo, almacén)
- Ejecución individual o de todas las pruebas
- Resúmenes estadísticos de resultados
- Colores para facilitar lectura
- Manejo de errores

**Cómo usar**:
```powershell
# 1. Editar el script y colocar el token
$token = "tu_token_jwt_aqui"

# 2. Ejecutar
.\test-api-cancelacion-reportes.ps1

# 3. Seleccionar opciones del menú
```

---

## 🚀 Inicio Rápido

### Opción 1: Usar Postman (Recomendado para testing manual)

1. Importar la colección:
   ```
   postman/SIGMAV2-Cancelacion-Reportes-Marbetes.postman_collection.json
   ```

2. Configurar variables:
   - `baseUrl`: http://localhost:8080
   - `token`: tu_token_jwt
   - `periodId`: 1
   - `warehouseId`: 5

3. Ejecutar las peticiones

### Opción 2: Usar el script de PowerShell (Recomendado para testing automatizado)

1. Abrir el script:
   ```powershell
   notepad test-api-cancelacion-reportes.ps1
   ```

2. Configurar el token en la línea 10:
   ```powershell
   $token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   ```

3. Ejecutar:
   ```powershell
   .\test-api-cancelacion-reportes.ps1
   ```

4. Seleccionar "A" para ejecutar todas las pruebas

### Opción 3: Usar cURL (Para pruebas rápidas)

```bash
# Cancelar un marbete
curl -X POST http://localhost:8080/api/sigmav2/labels/cancel \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"folio":1001,"periodId":1,"motivoCancelacion":"Prueba"}'

# Obtener marbetes pendientes
curl -X POST http://localhost:8080/api/sigmav2/labels/reports/pending \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"periodId":1,"warehouseId":null}'
```

---

## 📊 Endpoints Implementados

### Cancelación
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/sigmav2/labels/cancel` | Cancela un marbete |

### Reportes
| # | Endpoint | Descripción |
|---|----------|-------------|
| 1 | `/api/sigmav2/labels/reports/distribution` | Distribución de marbetes |
| 2 | `/api/sigmav2/labels/reports/list` | Listado completo |
| 3 | `/api/sigmav2/labels/reports/pending` | Marbetes pendientes |
| 4 | `/api/sigmav2/labels/reports/with-differences` | Con diferencias |
| 5 | `/api/sigmav2/labels/reports/cancelled-report` | Cancelados |
| 6 | `/api/sigmav2/labels/reports/comparative` | Comparativo |
| 7 | `/api/sigmav2/labels/reports/warehouse-detail` | Almacén detallado |
| 8 | `/api/sigmav2/labels/reports/product-detail` | Producto detallado |

---

## 🔐 Autenticación

Todos los endpoints requieren autenticación JWT:

```
Authorization: Bearer {token}
```

Para obtener un token, usar el endpoint de login:
```bash
POST /api/sigmav2/auth/login
{
  "email": "usuario@empresa.com",
  "password": "password"
}
```

---

## 🎭 Roles y Permisos

| Rol | Cancelar | Reportes | Almacenes |
|-----|----------|----------|-----------|
| ADMINISTRADOR | ✅ | ✅ | Todos |
| AUXILIAR | ✅ | ✅ | Todos |
| ALMACENISTA | ✅ | ✅ | Asignados |
| AUXILIAR_DE_CONTEO | ✅ | ✅ | Asignados |

---

## 📝 Estructura de Archivos Creados

```
SIGMAV2/
├── docs/
│   └── API-CANCELACION-REPORTES-MARBETES.md      # Documentación completa
├── postman/
│   └── SIGMAV2-Cancelacion-Reportes...json       # Colección Postman
├── src/main/java/.../labels/
│   ├── application/
│   │   ├── dto/
│   │   │   ├── CancelLabelDTO.java               # DTO cancelación
│   │   │   └── reports/
│   │   │       ├── ReportRequestDTO.java         # DTO request común
│   │   │       ├── LabelDistributionReportDTO.java
│   │   │       ├── LabelListReportDTO.java
│   │   │       ├── ComparativeReportDTO.java
│   │   │       ├── WarehouseDetailReportDTO.java
│   │   │       └── ProductDetailReportDTO.java
│   │   ├── service/
│   │   │   ├── LabelService.java                 # Interface (modificada)
│   │   │   └── impl/
│   │   │       └── LabelServiceImpl.java         # Implementación
│   └── adapter/
│       └── controller/
│           └── LabelsController.java             # Endpoints REST
├── IMPLEMENTACION-CANCELACION-REPORTES-MARBETES.md
├── QUICK-API-REFERENCE.md
├── README-APIS-CANCELACION-REPORTES.md           # Este archivo
└── test-api-cancelacion-reportes.ps1             # Script de pruebas
```

---

## 🧪 Testing

### Pruebas Manuales
1. Usar Postman con la colección incluida
2. Verificar cada endpoint individualmente
3. Probar con diferentes roles de usuario
4. Probar con diferentes almacenes

### Pruebas Automatizadas
1. Ejecutar el script de PowerShell
2. Revisar los resúmenes estadísticos
3. Verificar los códigos de respuesta
4. Validar la estructura de datos devueltos

### Checklist de Pruebas
- [ ] Cancelar marbete exitosamente
- [ ] Intentar cancelar marbete no existente (debe fallar)
- [ ] Obtener distribución para todos los almacenes
- [ ] Obtener distribución para almacén específico
- [ ] Obtener listado completo
- [ ] Obtener marbetes pendientes
- [ ] Obtener marbetes con diferencias
- [ ] Obtener marbetes cancelados
- [ ] Obtener reporte comparativo
- [ ] Obtener detalle por almacén
- [ ] Obtener detalle por producto
- [ ] Probar sin token (debe fallar con 401)
- [ ] Probar con token expirado (debe fallar con 401)
- [ ] Probar acceso a almacén no autorizado (debe fallar con 403)

---

## 🐛 Solución de Problemas

### Error 401 Unauthorized
- **Causa**: Token no válido o expirado
- **Solución**: Obtener un nuevo token con el endpoint de login

### Error 403 Forbidden
- **Causa**: Usuario sin permisos para el almacén
- **Solución**: Verificar permisos del usuario o usar almacén asignado

### Error 404 Not Found
- **Causa**: Marbete, periodo o almacén no existe
- **Solución**: Verificar IDs correctos

### Error 500 Internal Server Error
- **Causa**: Error en el servidor
- **Solución**: Revisar logs del servidor, verificar base de datos

### Lista vacía en reportes
- **Causa**: No hay datos para el periodo/almacén especificado
- **Solución**: Verificar que existan marbetes generados

---

## 📞 Soporte

Para preguntas o problemas:
1. Revisar la documentación completa en `docs/API-CANCELACION-REPORTES-MARBETES.md`
2. Verificar el checklist de pruebas
3. Revisar logs del servidor
4. Contactar al equipo de desarrollo

---

## 📅 Historial de Versiones

### v1.0.0 (8 de diciembre de 2025)
- ✅ Implementación inicial de cancelación de marbetes
- ✅ Implementación de 8 reportes completos
- ✅ Documentación completa de APIs
- ✅ Colección de Postman
- ✅ Script de pruebas automatizado
- ✅ Compilación exitosa sin errores

---

## 🔮 Próximos Pasos

1. **Integración Frontend**
   - Crear interfaces de usuario
   - Implementar exportación a Excel/PDF
   - Agregar gráficos y visualizaciones

2. **Optimizaciones**
   - Implementar caché para reportes
   - Agregar paginación para reportes grandes
   - Optimizar queries SQL

3. **Testing**
   - Crear pruebas unitarias
   - Crear pruebas de integración
   - Pruebas de carga y performance

4. **Documentación**
   - Actualizar Swagger/OpenAPI
   - Crear video tutoriales
   - Documentar casos de uso reales

---

**¡Listo para usar!** 🚀

Todos los endpoints están implementados, documentados y listos para ser consumidos.

