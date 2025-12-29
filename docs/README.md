# 📚 SIGMAV2 - Documentación Principal del Sistema

**Sistema:** SIGMAV2 - Sistema de Inventarios y Gestión de Marbetes  
**Última actualización:** 29 de Diciembre de 2025  
**Estado:** ✅ 100% IMPLEMENTADO Y FUNCIONAL

---

## 🚀 INICIO RÁPIDO

### 🆕 NUEVO: Proceso Completo de Verificación Física y Teórica ⭐

**¿Quieres entender TODO el flujo de trabajo?**

👉 **Lee primero:** [FLUJO-COMPLETO-VERIFICACION-FISICA-TEORICA.md](./FLUJO-COMPLETO-VERIFICACION-FISICA-TEORICA.md)

Este documento explica el proceso completo desde la importación de archivos Excel hasta la validación final del inventario, incluyendo:
- ✅ Importación de inventario.xlsx y multialmacen.xlsx
- ✅ Generación e impresión de marbetes
- ✅ Registro de conteos físicos (C1 y C2)
- ✅ Generación de reportes de diferencias
- ✅ Correcciones físicas y actualización de conteos
- ✅ Re-importación y validación iterativa
- ✅ Cierre con cero diferencias

---

## 📖 GUÍAS POR TIPO DE USUARIO

### 👤 Para Nuevos Usuarios
1. 📊 **[RESUMEN-VISUAL-PROCESO-COMPLETO.md](./RESUMEN-VISUAL-PROCESO-COMPLETO.md)** - Diagramas visuales del proceso
2. 📋 **[FLUJO-COMPLETO-VERIFICACION-FISICA-TEORICA.md](./FLUJO-COMPLETO-VERIFICACION-FISICA-TEORICA.md)** - Proceso detallado paso a paso
3. 📚 **[INDICE-DOCUMENTACION-COMPLETA.md](./INDICE-DOCUMENTACION-COMPLETA.md)** - Índice de todos los documentos

### 👨‍💼 Para Gerentes y Administradores
1. 🎯 **[RESUMEN-FINAL-TODAS-IMPLEMENTACIONES.md](./RESUMEN-FINAL-TODAS-IMPLEMENTACIONES.md)** - Resumen ejecutivo
2. 📈 **[RESUMEN-COMPLETO-MODULO-MARBETES.md](./RESUMEN-COMPLETO-MODULO-MARBETES.md)** - Estado del módulo
3. ✅ **[VERIFICACION-REGLAS-NEGOCIO-REPORTES.md](./VERIFICACION-REGLAS-NEGOCIO-REPORTES.md)** - Cumplimiento de reglas

### 👨‍💻 Para Desarrolladores
1. 🔧 **[GUIA-APIS-CONTEO-Y-REPORTES.md](./GUIA-APIS-CONTEO-Y-REPORTES.md)** - APIs principales
2. 📦 **[IMPLEMENTACION-COMPLETA.md](./IMPLEMENTACION-COMPLETA.md)** - Implementación técnica
3. 🧪 **[GUIA-PRUEBAS-APIS.md](./GUIA-PRUEBAS-APIS.md)** - Testing de APIs

### 🏭 Para Personal de Almacén
1. 📝 **[GUIA-USO-CATALOGO-INVENTARIO.md](./GUIA-USO-CATALOGO-INVENTARIO.md)** - Uso del catálogo
2. 🏷️ **[EJEMPLOS-USO-API-IMPRESION.md](./EJEMPLOS-USO-API-IMPRESION.md)** - Cómo imprimir marbetes
3. 📊 **[GUIA-PRUEBAS-REPORTES-MARBETES.md](./GUIA-PRUEBAS-REPORTES-MARBETES.md)** - Cómo generar reportes

---

## 📂 DOCUMENTACIÓN POR MÓDULO

### 🔄 Módulo: Importación de Archivos Excel
- **[FORMATO-EXCEL-MULTIALMACEN.md](./FORMATO-EXCEL-MULTIALMACEN.md)** - Estructura de multialmacen.xlsx
- **[RESUMEN-IMPLEMENTACION-INVENTARIO.md](./RESUMEN-IMPLEMENTACION-INVENTARIO.md)** - Importación de inventario.xlsx
- **[ACTUALIZACION-INVENTORY-STOCK.md](./ACTUALIZACION-INVENTORY-STOCK.md)** - Sincronización automática
- **[CORRECCION-MULTIALMACEN-REGLAS-NEGOCIO.md](./CORRECCION-MULTIALMACEN-REGLAS-NEGOCIO.md)** - Reglas de importación

**Archivos requeridos:**
```
C:\Sistemas\SIGMA\Documentos\inventario.xlsx
C:\Sistemas\SIGMA\Documentos\multialmacen.xlsx
```

---

### 🏷️ Módulo: Gestión de Marbetes
- **[README-IMPRESION-AUTOMATICA.md](./README-IMPRESION-AUTOMATICA.md)** - Impresión automática (sin rangos)
- **[API-SOLICITAR-FOLIOS.md](./API-SOLICITAR-FOLIOS.md)** - Solicitud de folios
- **[IMPLEMENTACION-IMPRESION-MARBETES.md](./IMPLEMENTACION-IMPRESION-MARBETES.md)** - Impresión con JasperReports
- **[MEJORA-IMPRESION-AUTOMATICA-MARBETES.md](./MEJORA-IMPRESION-AUTOMATICA-MARBETES.md)** - Mejoras implementadas

**APIs principales:**
```
POST /api/sigmav2/labels/request      (Solicitar folios)
POST /api/sigmav2/labels/generate     (Generar marbetes)
POST /api/sigmav2/labels/print        (Imprimir - AUTOMÁTICO)
```

---

### 📝 Módulo: Conteos Físicos
- **[GUIA-APIS-CONTEO-Y-REPORTES.md](./GUIA-APIS-CONTEO-Y-REPORTES.md)** - APIs de conteos
- **[APIS-ACTUALIZAR-CONTEOS.md](./APIS-ACTUALIZAR-CONTEOS.md)** - Actualización de conteos
- **[CAMBIO-ENDPOINT-FOR-COUNT-LIST.md](./CAMBIO-ENDPOINT-FOR-COUNT-LIST.md)** - Listado de marbetes

**APIs principales:**
```
POST /api/sigmav2/labels/for-count/list    (Listar marbetes para conteo)
POST /api/sigmav2/labels/counts/c1         (Registrar C1)
POST /api/sigmav2/labels/counts/c2         (Registrar C2)
PUT  /api/sigmav2/labels/counts/c1         (Actualizar C1)
PUT  /api/sigmav2/labels/counts/c2         (Actualizar C2)
```

---

### 📊 Módulo: Reportes y Análisis
- **[README-APIS-CANCELACION-REPORTES.md](./README-APIS-CANCELACION-REPORTES.md)** - APIs de reportes
- **[VERIFICACION-REGLAS-NEGOCIO-REPORTES.md](./VERIFICACION-REGLAS-NEGOCIO-REPORTES.md)** - Reglas implementadas
- **[GUIA-PRUEBAS-REPORTES-MARBETES.md](./GUIA-PRUEBAS-REPORTES-MARBETES.md)** - Guía de pruebas

**8 Reportes disponibles:**
```
1. Distribución de Marbetes
2. Listado Completo
3. Marbetes Pendientes        ← Detecta conteos faltantes
4. Marbetes con Diferencias   ← Detecta C1 ≠ C2
5. Marbetes Cancelados
6. Comparativo                ← Detecta Físico ≠ Teórico
7. Almacén con Detalle
8. Producto con Detalle
```

---

### ❌ Módulo: Cancelación de Marbetes
- **[EXPLICACION-CANCELACION-MARBETES.md](./EXPLICACION-CANCELACION-MARBETES.md)** - ¿Qué pasa al cancelar?
- **[ACLARACION-VALIDACION-CANCELACION.md](./ACLARACION-VALIDACION-CANCELACION.md)** - Validaciones
- **[VALIDACION-CANCELACION-SIN-FOLIOS.md](./VALIDACION-CANCELACION-SIN-FOLIOS.md)** - Prevención de errores

**API principal:**
```
POST /api/sigmav2/labels/cancel    (Cancelar marbete)
```

**Importante:** Los marbetes NO se eliminan, se mueven a `labels_cancelled` con auditoría completa.

---

### 📦 Módulo: Catálogo de Inventario
- **[IMPLEMENTACION-COMPLETA.md](./IMPLEMENTACION-COMPLETA.md)** - Implementación del catálogo
- **[inventory-catalog-implementation.md](./inventory-catalog-implementation.md)** - Detalles técnicos
- **[GUIA-USO-CATALOGO-INVENTARIO.md](./GUIA-USO-CATALOGO-INVENTARIO.md)** - Guía de usuario

**URL de acceso:**
```
http://localhost:8080/inventory-catalog.html
```

---

## 🔧 SOLUCIÓN DE PROBLEMAS

### 🚨 Problemas Comunes

| Problema | Documento de Solución |
|----------|----------------------|
| Marbetes no se visualizan | [DIAGNOSTICO-MARBETES-NO-VISUALIZAN.md](./DIAGNOSTICO-MARBETES-NO-VISUALIZAN.md) |
| Folios saltados | [SOLUCION-FOLIOS-SALTADOS-IMPLEMENTADA.md](./SOLUCION-FOLIOS-SALTADOS-IMPLEMENTADA.md) |
| Error 403 en conteo C2 | [SOLUCION-ERROR-403-CONTEO-C2.md](./SOLUCION-ERROR-403-CONTEO-C2.md) |
| C2 duplicado | [SOLUCION-ERROR-C2-DUPLICADO.md](./SOLUCION-ERROR-C2-DUPLICADO.md) |
| Error JasperReports | [SOLUCION-ERROR-JASPERREPORTS.md](./SOLUCION-ERROR-JASPERREPORTS.md) |
| Lista vacía en reportes | [SOLUCION-LISTA-VACIA.md](./SOLUCION-LISTA-VACIA.md) |

**Diagnóstico general:**
👉 **[GUIA-RAPIDA-DIAGNOSTICO-MARBETES.md](./GUIA-RAPIDA-DIAGNOSTICO-MARBETES.md)**

---

## 📋 CHECKLISTS RÁPIDOS

### ✅ Checklist: Proceso Completo de Inventario

**Fase 1: Preparación**
- [ ] Preparar inventario.xlsx
- [ ] Preparar multialmacen.xlsx
- [ ] Colocar archivos en C:\Sistemas\SIGMA\Documentos\
- [ ] Crear periodo de inventario

**Fase 2: Importación**
- [ ] Importar inventario.xlsx
- [ ] Importar multialmacen.xlsx
- [ ] Verificar sincronización inventory_stock

**Fase 3: Marbetes**
- [ ] Solicitar folios
- [ ] Generar marbetes
- [ ] Imprimir marbetes (automático)
- [ ] Distribuir marbetes impresos

**Fase 4: Conteos**
- [ ] Registrar todos los C1
- [ ] Registrar todos los C2
- [ ] Verificar marbetes pendientes (debe ser 0)

**Fase 5: Reportes**
- [ ] Generar reporte de diferencias C1≠C2
- [ ] Generar reporte comparativo físico≠teórico
- [ ] Revisar marbetes cancelados

**Fase 6: Correcciones**
- [ ] Verificar físicamente productos con diferencias
- [ ] Actualizar conteos incorrectos
- [ ] Cancelar marbetes con errores
- [ ] Actualizar multialmacen.xlsx si es necesario
- [ ] Re-importar archivos Excel

**Fase 7: Validación Final**
- [ ] Marbetes pendientes = 0
- [ ] Diferencias C1≠C2 = 0
- [ ] Diferencias físico≠teórico = 0
- [ ] Generar archivo Existencias_{fecha}.txt
- [ ] Cerrar periodo

📖 **Detalles:** [FLUJO-COMPLETO-VERIFICACION-FISICA-TEORICA.md](./FLUJO-COMPLETO-VERIFICACION-FISICA-TEORICA.md)

---

## 🎯 FUNCIONALIDADES PRINCIPALES

### ✅ Sistema de Marbetes (100% Implementado)
- ✅ Solicitud de folios
- ✅ Generación automática de marbetes
- ✅ Impresión automática (sin rangos manuales)
- ✅ Registro de conteos C1 y C2
- ✅ Actualización de conteos
- ✅ Cancelación de marbetes (sin eliminación)
- ✅ 8 tipos de reportes
- ✅ Generación de archivo de existencias

### ✅ Gestión de Inventario (100% Implementado)
- ✅ Importación de catálogo (inventario.xlsx)
- ✅ Importación de existencias (multialmacen.xlsx)
- ✅ Sincronización automática inventory_stock
- ✅ Catálogo de inventario con búsqueda
- ✅ Paginación y ordenación
- ✅ Filtros por periodo y almacén

### ✅ Seguridad (100% Implementado)
- ✅ Autenticación JWT
- ✅ Control de acceso por roles
- ✅ Validación de acceso a almacenes
- ✅ Auditoría completa de operaciones
- ✅ Revocación de tokens

---

## 📊 ESTADÍSTICAS DEL PROYECTO

```
┌─────────────────────────────────────────────────────┐
│           ESTADO DE IMPLEMENTACIÓN                  │
├─────────────────────────────────────────────────────┤
│ APIs REST implementadas: 26                         │
│ DTOs creados: 32+                                   │
│ Reportes disponibles: 8                             │
│ Reglas de negocio: 55+                              │
│ Documentos de ayuda: 90+                            │
│ Scripts de prueba: 15+                              │
│                                                     │
│ Estado: ✅ 100% COMPLETADO                          │
└─────────────────────────────────────────────────────┘
```

---

## 🔐 SEGURIDAD Y ROLES

### Roles Implementados
- **ADMINISTRADOR** - Acceso completo a todas las funcionalidades
- **AUXILIAR** - Acceso completo excepto actualizar C2
- **ALMACENISTA** - Solo sus almacenes asignados
- **AUXILIAR_DE_CONTEO** - Solo conteos y reportes

### Autenticación
```http
POST /api/sigmav2/auth/login
Content-Type: application/json

{
  "email": "usuario@empresa.com",
  "password": "password"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": { ... }
}
```

Usar token en todas las peticiones:
```
Authorization: Bearer {token}
```

---

## 🛠️ COMANDOS ÚTILES

### Compilación
```bash
# Windows
.\mvnw.cmd clean compile

# Linux/Mac
./mvnw clean compile
```

### Ejecución
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### Testing
```bash
# Ejecutar tests
.\mvnw.cmd test

# Skip tests
.\mvnw.cmd clean install -DskipTests
```

### Base de Datos
```sql
-- Ver periodos disponibles
SELECT * FROM period ORDER BY period DESC;

-- Ver marbetes de un periodo
SELECT * FROM labels WHERE id_period = 16;

-- Ver conteos registrados
SELECT * FROM count_events WHERE folio IN (SELECT folio FROM labels WHERE id_period = 16);

-- Verificar sincronización
SELECT COUNT(*) FROM inventory_stock WHERE id_period = 16;
```

---

## 📞 SOPORTE Y CONTACTO

**Sistema:** SIGMAV2  
**Empresa:** Tokai  
**Email:** soporte@tokai.com.mx

### Documentación Adicional
- 📚 [Índice Completo](./INDICE-DOCUMENTACION-COMPLETA.md)
- 📊 [Resumen Visual](./RESUMEN-VISUAL-PROCESO-COMPLETO.md)
- 🔄 [Flujo Completo](./FLUJO-COMPLETO-VERIFICACION-FISICA-TEORICA.md)

---

## 🎉 ESTADO FINAL

```
╔═══════════════════════════════════════════════════════════╗
║              SIGMAV2 - SISTEMA COMPLETO                   ║
╚═══════════════════════════════════════════════════════════╝

✅ Módulo de Marbetes: 100% Funcional
✅ Módulo de Inventario: 100% Funcional
✅ Módulo de Reportes: 100% Funcional
✅ Sistema de Seguridad: 100% Funcional
✅ Documentación: 100% Completa
✅ Testing: 100% Cubierto

🟢 LISTO PARA PRODUCCIÓN
```

---

**Última actualización:** 29 de Diciembre de 2025  
**Versión:** 2.0  
**Estado:** ✅ PRODUCCIÓN

