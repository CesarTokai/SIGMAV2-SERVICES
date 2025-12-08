# Guía de Compilación y Ejecución - Módulo de Marbetes

## 🔧 Compilación del Proyecto

### Compilar Todo el Proyecto
```powershell
cd C:\Users\cesarg\Desktop\SIGMAV2\SIGMAV2
mvn clean compile
```

### Compilar sin Tests
```powershell
mvn clean compile -DskipTests
```

### Compilar en Modo Silencioso
```powershell
mvn clean compile -DskipTests -q
```

### Verificar Solo Errores
```powershell
mvn clean compile -DskipTests 2>&1 | Select-String -Pattern "ERROR"
```

---

## 🏃 Ejecutar la Aplicación

### Iniciar el Servidor Spring Boot
```powershell
mvn spring-boot:run
```

### Iniciar en Modo Debug
```powershell
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### Verificar que el Servidor Está Corriendo
```powershell
curl http://localhost:8080/actuator/health
```

---

## 🧪 Ejecutar Tests

### Ejecutar Script de Pruebas PowerShell
```powershell
# Primero, obtener un token JWT válido
$token = "TU_TOKEN_JWT_AQUI"

# Ejecutar el script de pruebas
.\test-reportes-marbetes.ps1
```

### Modificar el Script para tu Token
Editar `test-reportes-marbetes.ps1` y cambiar:
```powershell
$token = "YOUR_JWT_TOKEN_HERE"  # Reemplazar con token real
```

### Ejecutar Tests Unitarios (cuando estén disponibles)
```powershell
mvn test
```

### Ejecutar Tests de Integración (cuando estén disponibles)
```powershell
mvn verify
```

---

## 📝 Obtener Token JWT

### 1. Login con Usuario
```powershell
$loginUrl = "http://localhost:8080/api/sigmav2/auth/login"
$loginBody = @{
    email = "tu_email@example.com"
    password = "tu_password"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
$token = $response.token

Write-Host "Token obtenido: $token"
```

### 2. Usar el Token en Requests
```powershell
$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}

# Ejemplo: Cancelar un marbete
$cancelBody = @{
    folio = 1001
    periodId = 1
    warehouseId = 2
    motivoCancelacion = "Prueba"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/cancel" -Method Post -Headers $headers -Body $cancelBody
```

---

## 🔍 Verificar Implementación

### 1. Verificar que los Endpoints Existen
```powershell
# Debe retornar 401 (no autorizado) si los endpoints existen
Invoke-WebRequest -Uri "http://localhost:8080/api/sigmav2/labels/cancel" -Method Post
Invoke-WebRequest -Uri "http://localhost:8080/api/sigmav2/labels/reports/distribution" -Method Post
```

### 2. Ver Logs del Servidor
Los logs mostrarán las llamadas a los endpoints:
```
INFO  c.m.S.m.l.a.c.LabelsController - Cancelando marbete folio 1001...
INFO  c.m.S.m.l.a.s.i.LabelServiceImpl - Marbete 1001 cancelado exitosamente
```

### 3. Verificar en Base de Datos

#### Verificar Cancelación
```sql
-- Ver marbete cancelado
SELECT * FROM labels WHERE folio = 1001;

-- Ver registro de cancelación
SELECT * FROM labels_cancelled WHERE folio = 1001;
```

#### Verificar Datos para Reportes
```sql
-- Contar marbetes por periodo
SELECT COUNT(*) FROM labels WHERE id_period = 1;

-- Ver marbetes con conteos
SELECT l.folio, lc.one_count, lc.second_count
FROM labels l
LEFT JOIN label_counts lc ON l.folio = lc.folio
WHERE l.id_period = 1;
```

---

## 🐛 Debugging

### Ver Logs Detallados
Agregar en `application.properties`:
```properties
logging.level.tokai.com.mx.SIGMAV2.modules.labels=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

### Verificar Errores de Compilación
```powershell
mvn clean compile 2>&1 | Select-String -Pattern "ERROR" | Format-Table
```

### Ver Stack Traces Completos
Revisar el archivo de logs o la consola cuando hay excepciones.

---

## 📊 Probar Cada Endpoint

### Cancelar Marbete
```powershell
$body = @{
    folio = 1001
    periodId = 1
    warehouseId = 2
    motivoCancelacion = "Prueba de cancelación"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/cancel" `
    -Method Post -Headers $headers -Body $body
```

### Reporte de Distribución
```powershell
$filter = @{
    periodId = 1
    warehouseId = $null
} | ConvertTo-Json

$result = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/reports/distribution" `
    -Method Post -Headers $headers -Body $filter

$result | Format-Table
```

### Reporte de Listado
```powershell
$filter = @{
    periodId = 1
    warehouseId = 2
} | ConvertTo-Json

$result = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/reports/list" `
    -Method Post -Headers $headers -Body $filter

$result | Format-Table -Property numeroMarbete, claveProducto, conteo1, conteo2, estado
```

### Reporte de Pendientes
```powershell
$result = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/reports/pending" `
    -Method Post -Headers $headers -Body $filter

Write-Host "Marbetes pendientes: $($result.Count)"
$result | Format-Table
```

### Reporte de Diferencias
```powershell
$result = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/reports/with-differences" `
    -Method Post -Headers $headers -Body $filter

Write-Host "Marbetes con diferencias: $($result.Count)"
$result | Format-Table -Property numeroMarbete, conteo1, conteo2, diferencia
```

### Reporte de Cancelados
```powershell
$result = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/reports/cancelled" `
    -Method Post -Headers $headers -Body $filter

Write-Host "Marbetes cancelados: $($result.Count)"
$result | Format-Table -Property numeroMarbete, motivoCancelacion, canceladoPor
```

### Reporte Comparativo
```powershell
$result = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/reports/comparative" `
    -Method Post -Headers $headers -Body $filter

$result | Format-Table -Property claveProducto, existenciasFisicas, existenciasTeoricas, diferencia, porcentajeDiferencia
```

### Reporte Almacén con Detalle
```powershell
$result = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/reports/warehouse-detail" `
    -Method Post -Headers $headers -Body $filter

Write-Host "Total de registros: $($result.Count)"
$result | Select-Object -First 10 | Format-Table
```

### Reporte Producto con Detalle
```powershell
$filterAll = @{
    periodId = 1
    warehouseId = $null
} | ConvertTo-Json

$result = Invoke-RestMethod -Uri "http://localhost:8080/api/sigmav2/labels/reports/product-detail" `
    -Method Post -Headers $headers -Body $filterAll

Write-Host "Total de registros: $($result.Count)"
$result | Select-Object -First 10 | Format-Table
```

---

## 📦 Generar JAR para Producción

### Compilar y Empaquetar
```powershell
mvn clean package -DskipTests
```

### Ejecutar JAR
```powershell
java -jar target\SIGMAV2-0.0.1-SNAPSHOT.jar
```

### Ejecutar con Perfil de Producción
```powershell
java -jar target\SIGMAV2-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 🔄 Recargar Cambios sin Reiniciar

### Usar Spring DevTools (si está configurado)
Los cambios se recargarán automáticamente al guardar archivos.

### Reinicio Rápido
```powershell
# Detener el servidor (Ctrl+C)
# Reiniciar
mvn spring-boot:run
```

---

## 📈 Monitoreo y Métricas

### Ver Métricas de Actuator (si está habilitado)
```powershell
# Health check
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# Info
curl http://localhost:8080/actuator/info
```

---

## ⚡ Tips de Performance

### Compilación Rápida
```powershell
# Compilar solo módulos cambiados
mvn compile -pl :SIGMAV2 -am
```

### Ejecutar con Más Memoria
```powershell
set MAVEN_OPTS=-Xmx2048m -XX:MaxPermSize=512m
mvn clean install
```

---

## 🚨 Solución de Problemas Comunes

### Error: "Cannot resolve symbol"
```powershell
# Limpiar e importar dependencias
mvn clean
mvn dependency:resolve
# Recompilar
mvn compile
```

### Error: "Port already in use"
```powershell
# Encontrar proceso usando el puerto 8080
netstat -ano | findstr :8080

# Matar el proceso (reemplazar PID)
taskkill /PID <PID> /F

# O cambiar el puerto en application.properties
# server.port=8081
```

### Error: "JpaUserRepository could not be autowired"
Este es un warning del IDE, no afecta la compilación. El bean existe en tiempo de ejecución.

### Tests Fallan
```powershell
# Ejecutar sin tests
mvn clean package -DskipTests
```

---

## 📚 Recursos Adicionales

### Documentación Creada
- `README-CANCELACION-Y-REPORTES-MARBETES.md` - Documentación completa de APIs
- `RESUMEN-IMPLEMENTACION-CANCELACION-REPORTES.md` - Resumen ejecutivo
- `CHECKLIST-IMPLEMENTACION-MARBETES.md` - Checklist de implementación
- `test-reportes-marbetes.ps1` - Script de pruebas automatizado

### Logs Importantes
- Logs de aplicación: `logs/spring-boot-logger.log` (si está configurado)
- Logs de compilación: En la consola de Maven
- Logs de servidor: En la consola donde se ejecuta `mvn spring-boot:run`

---

## ✅ Verificación Final

### Checklist Antes de Deployment
- [ ] Compilación exitosa sin errores
- [ ] Todos los tests pasan
- [ ] Endpoints responden correctamente
- [ ] Autenticación y autorización funcionan
- [ ] Logs no muestran errores
- [ ] Base de datos está actualizada
- [ ] Documentación está completa
- [ ] Scripts de prueba funcionan

---

**Última Actualización:** 8 de Diciembre de 2025
**Versión:** 1.0
**Estado:** ✅ Listo para Testing

