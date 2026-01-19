# Manual de Usuario - Módulo Generar Archivo

**Versión:** 2.0  
**Fecha:** 16 de enero de 2026  
**Sistema:** SIGMA V2 - Sistema de Gestión de Inventarios  
**Módulo:** Generar Archivo de Existencias

---

## 📋 Descripción General

El módulo **Generar Archivo** permite obtener un archivo de texto con el contenido del inventario de productos de la organización y sus respectivas existencias físicas.

Es decir, un listado de todos los productos ordenados alfabéticamente por clave de producto, con sus respectivas existencias físicas, resultado de todo el proceso de gestión de marbetes.

---

## 🎯 Objetivo

Generar un archivo de texto plano (TXT) que contenga:
- **Clave del Producto:** Identificador único del producto
- **Descripción:** Nombre del producto
- **Existencias:** Total de existencias físicas registradas durante el inventario

Este archivo puede ser utilizado para:
- Actualizar sistemas de inventario externos
- Realizar auditorías
- Generar reportes consolidados
- Integración con otros sistemas de la organización

---

## 🚀 Procedimiento para Generar el Archivo

### Paso 1: Acceder al Módulo

En el **menú principal** (Menú lateral), presione sobre la opción **"Generar archivo"**.

```
📁 Inicio
📋 Catálogos
🏷️ Marbetes
📊 Reportes
📄 Generar archivo  ← AQUÍ
```

### Paso 2: Seleccionar Periodo

Enseguida se visualizará la interfaz de **selección de periodo**.

- Por defecto aparece seleccionado el **último periodo registrado**
- Puede cambiar la selección a través de la **lista desplegable**
- Seleccione el periodo del cual desea generar el archivo

```
┌─────────────────────────────────────┐
│   Selección de Periodo              │
├─────────────────────────────────────┤
│                                     │
│   Periodo:  [Diciembre2015 ▼]      │
│                                     │
│          [ Generar Archivo ]        │
│                                     │
└─────────────────────────────────────┘
```

### Paso 3: Generar el Archivo

Presione el botón **"Generar archivo"**.

Se desplegará una ventana indicando que el reporte se está generando y que espere a que este proceso finalice.

```
┌─────────────────────────────────────┐
│           ⏳ Aviso                   │
├─────────────────────────────────────┤
│                                     │
│  Espere a que se genere el          │
│  archivo TXT...                     │
│                                     │
└─────────────────────────────────────┘
```

### Paso 4: Confirmación de Generación

Una vez que el proceso de generación del archivo TXT haya finalizado, se mostrará una ventana que notificará el hecho:

```
┌─────────────────────────────────────┐
│           ✅ Éxito                   │
├─────────────────────────────────────┤
│                                     │
│  El archivo se generó/actualizó     │
│  correctamente.                     │
│                                     │
│            [ Aceptar ]              │
│                                     │
└─────────────────────────────────────┘
```

### Paso 5: Ubicación del Archivo

El archivo generado estará en la ubicación siguiente:

```
C:\Sistemas\SIGMA\Documentos\
```

---

## 📁 Nomenclatura del Archivo

El nombre del archivo generado estará basado en la nomenclatura siguiente:

```
"Existencias" + Nombre del periodo + ".txt"
```

### Ejemplos:
- `Existencias_Diciembre2016.txt`
- `Existencias_Enero2026.txt`
- `Existencias_Marzo2025.txt`

---

## 📄 Estructura del Archivo

El archivo TXT generado tiene la siguiente estructura:

```
CLAVE_PRODUCTO    DESCRIPCION              EXISTENCIAS
========================================
PROD001           Tornillo M8 x 20mm       1500
PROD002           Tuerca M8                2000
PROD003           Arandela plana M8        3500
PROD004           Pintura azul 1L          125
PROD005           Aceite motor SAE 10W-40  450
```

### Columnas del Archivo:

| Columna | Descripción | Ejemplo |
|---------|-------------|---------|
| **Clave_Producto** | Identificador del producto | PROD001 |
| **Descripción** | Nombre del producto | Tornillo M8 x 20mm |
| **Existencias** | Total de existencias físicas registradas | 1500 |

### Características del Formato:
- **Delimitador:** Tabulador (`\t`) entre columnas
- **Codificación:** UTF-8 (soporta caracteres especiales como ñ, á, é, í, ó, ú)
- **Orden:** Alfabético por clave de producto
- **Decimales:** Los valores decimales se muestran sin ceros innecesarios (150.00 → 150)

---

## ⚠️ Notas Importantes

### ✅ Actualización de Archivo Existente

> **NOTA:** Si usted ya generó un archivo de un determinado periodo y repite el proceso de generación del mismo, el archivo será **sustituido** y contendrá la información más reciente.

Es decir, el archivo para un periodo determinado será **actualizado** con los datos más recientes del inventario.

### 📊 Datos Considerados

El archivo incluye:
- ✅ Todos los productos del periodo seleccionado
- ✅ Solo marbetes NO cancelados
- ✅ Conteos finales (C2 si existe, sino C1)
- ✅ Suma de existencias de todos los almacenes
- ❌ NO incluye marbetes cancelados

### 🔒 Permisos Requeridos

Para generar archivos, debe tener uno de los siguientes roles:
- **ADMINISTRADOR**
- **AUXILIAR**
- **ALMACENISTA**

---

## 🛠️ Solución de Problemas

### Problema: No aparece el botón "Generar archivo"
**Solución:** Verifique que su usuario tenga los permisos necesarios (ADMINISTRADOR, AUXILIAR o ALMACENISTA).

### Problema: El archivo no se genera
**Solución:**
1. Verifique que el periodo seleccionado tenga marbetes registrados
2. Asegúrese de tener permisos de escritura en `C:\Sistemas\SIGMA\Documentos\`
3. Verifique que no haya otro programa usando el archivo

### Problema: El archivo está vacío
**Solución:**
- El periodo seleccionado no tiene conteos registrados
- Verifique que existan marbetes con conteos C1 o C2 en ese periodo

### Problema: Las existencias no coinciden
**Solución:**
- El sistema usa el conteo más reciente (C2 preferido, C1 si no existe C2)
- Los marbetes cancelados NO se incluyen en el cálculo
- Las existencias se suman de TODOS los almacenes

---

## 📞 Soporte Técnico

Si tiene problemas con el módulo de generación de archivos, contacte a:

- **Soporte Técnico:** soporte@tokai.com.mx
- **Documentación Técnica:** Ver `DOCUMENTACION-GENERAR-ARCHIVO-TXT.md`

---

## 🔄 Historial de Versiones

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 2.0 | 16/01/2026 | Manual de usuario completo basado en SIGMA V2 |
| 1.0 | 10/12/2025 | Implementación inicial de generación de archivos |

---

**© 2026 Tokai - Sistema SIGMA V2**
