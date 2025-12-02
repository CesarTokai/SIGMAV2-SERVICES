# 📚 Índice de Documentación - Impresión de Marbetes

Este documento sirve como índice maestro para toda la documentación relacionada con la implementación del módulo de Impresión de Marbetes.

---

## 📋 Documentos Disponibles

### 1. Resumen Ejecutivo
**Archivo:** `RESUMEN-IMPLEMENTACION-IMPRESION-MARBETES.md`
**Audiencia:** Project Managers, Product Owners, Stakeholders
**Contenido:**
- Vista general de la implementación
- Métricas y cobertura
- Estado del proyecto
- Próximos pasos
- Criterios de aceptación

**🔗 Cuándo usarlo:**
- Para reportes a gerencia
- Para presentaciones ejecutivas
- Para revisión de progreso del proyecto

---

### 2. Documentación Técnica Completa
**Archivo:** `IMPLEMENTACION-IMPRESION-MARBETES.md`
**Audiencia:** Desarrolladores, Arquitectos de Software
**Contenido:**
- Detalle técnico de cada regla de negocio
- Código fuente implementado
- Flujos de validación
- Mensajes de error
- Archivos modificados
- Próximos pasos técnicos

**🔗 Cuándo usarlo:**
- Para entender la implementación técnica
- Para mantenimiento del código
- Para debugging de issues
- Para onboarding de nuevos desarrolladores

---

### 3. Ejemplos de Uso de la API
**Archivo:** `EJEMPLOS-USO-API-IMPRESION.md`
**Audiencia:** Desarrolladores Frontend, Integradores, Testers
**Contenido:**
- Ejemplos de requests/responses HTTP
- Casos de uso comunes (11 escenarios)
- Scripts de testing (cURL, PowerShell)
- Errores comunes y soluciones (7 tipos)
- Flujo completo de trabajo
- Testing automatizado

**🔗 Cuándo usarlo:**
- Para integrar el frontend con el backend
- Para crear scripts de prueba
- Para debugging de requests HTTP
- Para documentar la API externamente

---

### 4. Checklist de Verificación
**Archivo:** `CHECKLIST-VERIFICACION-IMPRESION.md`
**Audiencia:** QA Engineers, Testers, Product Owners
**Contenido:**
- 15 categorías de pruebas
- 100+ casos de prueba específicos
- Criterios de aceptación
- Configuración de ambiente de prueba
- Datos de prueba necesarios
- Resumen de verificación

**🔗 Cuándo usarlo:**
- Para planear el testing
- Para ejecutar pruebas de QA
- Para validar reglas de negocio
- Para UAT (User Acceptance Testing)

---

### 5. Índice Maestro (Este Documento)
**Archivo:** `INDICE-DOCUMENTACION-IMPRESION.md`
**Audiencia:** Todos
**Contenido:**
- Índice de todos los documentos
- Descripción de cada documento
- Referencias rápidas
- Guía de navegación

**🔗 Cuándo usarlo:**
- Como punto de entrada a la documentación
- Para encontrar el documento correcto según necesidad
- Para navegar entre documentos relacionados

---

## 🗺️ Mapa de Navegación

### Si eres... deberías leer:

#### 📊 **Project Manager / Product Owner**
1. `RESUMEN-IMPLEMENTACION-IMPRESION-MARBETES.md` (vista general)
2. `CHECKLIST-VERIFICACION-IMPRESION.md` (criterios de aceptación)

#### 💻 **Desarrollador Backend**
1. `IMPLEMENTACION-IMPRESION-MARBETES.md` (documentación técnica)
2. `EJEMPLOS-USO-API-IMPRESION.md` (ejemplos de uso)

#### 🎨 **Desarrollador Frontend**
1. `EJEMPLOS-USO-API-IMPRESION.md` (ejemplos de requests)
2. `IMPLEMENTACION-IMPRESION-MARBETES.md` (mensajes de error)

#### 🧪 **QA Engineer / Tester**
1. `CHECKLIST-VERIFICACION-IMPRESION.md` (casos de prueba)
2. `EJEMPLOS-USO-API-IMPRESION.md` (scripts de testing)

#### 🏗️ **Arquitecto de Software**
1. `IMPLEMENTACION-IMPRESION-MARBETES.md` (decisiones técnicas)
2. `RESUMEN-IMPLEMENTACION-IMPRESION-MARBETES.md` (impacto del sistema)

#### 🆕 **Nuevo en el Proyecto**
1. `RESUMEN-IMPLEMENTACION-IMPRESION-MARBETES.md` (contexto general)
2. `EJEMPLOS-USO-API-IMPRESION.md` (ejemplos prácticos)
3. `IMPLEMENTACION-IMPRESION-MARBETES.md` (detalles técnicos)

---

## 📂 Estructura de Archivos

```
docs/
├── INDICE-DOCUMENTACION-IMPRESION.md              (Este archivo)
├── RESUMEN-IMPLEMENTACION-IMPRESION-MARBETES.md   (Resumen ejecutivo)
├── IMPLEMENTACION-IMPRESION-MARBETES.md           (Documentación técnica)
├── EJEMPLOS-USO-API-IMPRESION.md                  (Ejemplos y scripts)
└── CHECKLIST-VERIFICACION-IMPRESION.md            (QA y testing)

src/main/java/tokai/com/mx/SIGMAV2/modules/labels/
├── application/
│   ├── service/
│   │   └── impl/
│   │       └── LabelServiceImpl.java              (Implementación principal)
│   └── exception/
│       └── CatalogNotLoadedException.java         (Nueva excepción)
└── infrastructure/
    └── adapter/
        └── LabelsPersistenceAdapter.java          (Lógica de persistencia)

src/main/java/tokai/com/mx/SIGMAV2/modules/inventory/
└── infrastructure/
    └── persistence/
        └── JpaInventoryStockRepository.java       (Método agregado)
```

---

## 🔍 Referencias Rápidas

### Código Fuente Principal
- **Servicio:** `LabelServiceImpl.printLabels()` (líneas 183-238)
- **Adapter:** `LabelsPersistenceAdapter.printLabelsRange()` (líneas 137-193)
- **Repositorio:** `JpaInventoryStockRepository.existsByWarehouseIdWarehouseAndPeriodId()`

### Reglas de Negocio Implementadas
1. Control de acceso por rol → `LabelServiceImpl.java:183-195`
2. Validación de catálogos → `LabelServiceImpl.java:197-207`
3. Validación de rango → `LabelServiceImpl.java:209-213`
4. Impresión normal/extraordinaria → `LabelsPersistenceAdapter.java:171-173`
5. No imprimir cancelados → `LabelsPersistenceAdapter.java:171-173`
6. Registro de auditoría → `LabelsPersistenceAdapter.java:181-191`
7. Logging detallado → `LabelServiceImpl.java:183, 215-216, 226-238`

### Endpoints de API
- **POST** `/api/labels/print` - Imprimir marbetes

### Excepciones
- `CatalogNotLoadedException` - Catálogos no cargados
- `InvalidLabelStateException` - Estado de marbete inválido
- `PermissionDeniedException` - Sin permisos de acceso
- `LabelNotFoundException` - Marbete no encontrado

### Tablas de Base de Datos
- `label` - Marbetes individuales
- `label_prints` - Registro de impresiones
- `label_requests` - Solicitudes de folios
- `inventory_stock` - Catálogo de inventario

---

## 🎯 Casos de Uso Documentados

### En `EJEMPLOS-USO-API-IMPRESION.md`
1. ✅ Impresión normal (marbetes recién generados)
2. ✅ Impresión extraordinaria (reimpresión)
3. ✅ Impresión de un solo folio
4. ✅ Administrador imprime en cualquier almacén
5. ✅ Error: Catálogos no cargados
6. ✅ Error: Rango de folios inválido
7. ✅ Error: Folios faltantes
8. ✅ Error: Marbete cancelado
9. ✅ Error: Folio no pertenece al periodo/almacén
10. ✅ Error: Usuario sin acceso
11. ✅ Error: Rango muy grande (>500 folios)

---

## 📊 Métricas de Documentación

| Métrica | Valor |
|---------|-------|
| Documentos creados | 5 |
| Páginas totales | ~40 |
| Ejemplos de código | 20+ |
| Casos de prueba documentados | 100+ |
| Casos de uso documentados | 11 |
| Reglas de negocio documentadas | 7 |
| Scripts de testing | 4 (cURL, PowerShell) |

---

## 🔄 Flujo de Lectura Recomendado

### Para Implementación Nueva
```
1. RESUMEN-IMPLEMENTACION-IMPRESION-MARBETES.md
   ↓
2. IMPLEMENTACION-IMPRESION-MARBETES.md
   ↓
3. EJEMPLOS-USO-API-IMPRESION.md
   ↓
4. CHECKLIST-VERIFICACION-IMPRESION.md
```

### Para Debugging
```
1. EJEMPLOS-USO-API-IMPRESION.md (sección de errores)
   ↓
2. IMPLEMENTACION-IMPRESION-MARBETES.md (mensajes de error)
   ↓
3. Código fuente: LabelServiceImpl.java
```

### Para Testing
```
1. CHECKLIST-VERIFICACION-IMPRESION.md
   ↓
2. EJEMPLOS-USO-API-IMPRESION.md
   ↓
3. Scripts de testing (cURL/PowerShell)
```

---

## 📞 Contacto y Soporte

### Para Preguntas Técnicas
- Revisar: `IMPLEMENTACION-IMPRESION-MARBETES.md`
- Código: `LabelServiceImpl.java`

### Para Preguntas de Negocio
- Revisar: `RESUMEN-IMPLEMENTACION-IMPRESION-MARBETES.md`
- Contactar: Product Owner

### Para Preguntas de Testing
- Revisar: `CHECKLIST-VERIFICACION-IMPRESION.md`
- Contactar: QA Lead

---

## 🔄 Actualizaciones

| Fecha | Documento | Cambios |
|-------|-----------|---------|
| 2025-12-02 | Todos | Creación inicial de documentación completa |

---

## ✅ Checklist de Documentación

- [x] Resumen ejecutivo creado
- [x] Documentación técnica completa
- [x] Ejemplos de uso documentados
- [x] Checklist de QA creado
- [x] Índice maestro creado
- [x] Referencias cruzadas implementadas
- [x] Casos de uso documentados
- [x] Scripts de testing incluidos
- [x] Flujos de trabajo documentados
- [x] Mensajes de error documentados

---

## 📌 Nota Final

Esta documentación está **completa y lista para uso**. Todos los documentos están sincronizados con el código implementado en la versión 1.0.0 del módulo de Impresión de Marbetes.

**Fecha de Creación:** 2 de diciembre de 2025
**Versión:** 1.0.0
**Estado:** ✅ Completo

