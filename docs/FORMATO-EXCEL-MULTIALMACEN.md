# Formato del Archivo multialmacen.xlsx

## Ubicación del Archivo
El archivo debe estar en la ruta:
```
C:\Sistemas\SIGMA\Documentos\multialmacen.xlsx
```

## Estructura del Archivo Excel

### Hoja 1 (Primera hoja del workbook)

| CVE_ALM | CVE_ART | DESCR | STATUS | EXIST |
|---------|---------|-------|--------|-------|
| ALM_01 | PROD_001 | Producto ejemplo 1 | A | 100.50 |
| ALM_01 | PROD_002 | Producto ejemplo 2 | A | 250.00 |
| ALM_02 | PROD_001 | Producto ejemplo 1 | A | 75.25 |
| ALM_02 | PROD_003 | Producto ejemplo 3 | B | 0.00 |
| ALM_03 | PROD_004 | Producto ejemplo 4 | A | 1500.75 |

## Descripción de Columnas

### CVE_ALM (OBLIGATORIO)
- **Tipo:** Texto
- **Descripción:** Clave única del almacén
- **Ejemplos:** "ALM_01", "CEDIS", "TIENDA_MTY"
- **Longitud máxima:** 50 caracteres
- **Notas:**
  - Si el almacén no existe en el sistema, será creado automáticamente
  - Esta es la clave primaria para identificar el almacén
  - Se almacena en el campo `warehouse_key` de la tabla `warehouse`

### CVE_ART (OBLIGATORIO)
- **Tipo:** Texto
- **Descripción:** Clave única del producto
- **Ejemplos:** "PROD_001", "ART-12345", "SKU-9876"
- **Longitud máxima:** 50 caracteres
- **Notas:**
  - Si el producto no existe en el inventario, será creado automáticamente con estado "A"
  - Esta es la clave primaria para identificar el producto

### DESCR (OPCIONAL)
- **Tipo:** Texto
- **Descripción:** Descripción del producto
- **Ejemplos:** "Laptop Dell Inspiron 15", "Mouse óptico USB"
- **Longitud máxima:** 255 caracteres
- **Notas:**
  - **IMPORTANTE:** Si el producto ya existe en el catálogo de inventario, esta columna será IGNORADA
  - La descripción siempre se obtiene del catálogo de inventario (tabla `products`, campo `descr`)
  - Solo se usa esta columna si el producto es nuevo y no existe en el inventario

### STATUS (OBLIGATORIO)
- **Tipo:** Texto
- **Descripción:** Estado del producto
- **Valores permitidos:**
  - `A` = Alta (producto activo)
  - `B` = Baja (producto inactivo/discontinuado)
- **Longitud:** 1 carácter
- **Notas:**
  - El sistema acepta "A", "a", "ALTA", "Alta" (normaliza a "A")
  - El sistema acepta "B", "b", "BAJA", "Baja" (normaliza a "B")

### EXIST (OBLIGATORIO)
- **Tipo:** Número decimal
- **Descripción:** Cantidad de existencias del producto en el almacén
- **Ejemplos:** 100, 250.50, 0, 1500.75
- **Formato:** Decimal con hasta 2 decimales
- **Rango:** 0.00 a 999999999999.99
- **Notas:**
  - Puede ser cero (para productos sin stock)
  - Se almacena con precisión de 2 decimales

## Reglas de Importación

### 1. Creación Automática de Almacenes
Si en el archivo aparece un `CVE_ALM` que no existe en la tabla `warehouse`:
- ✅ Se crea el almacén automáticamente
- ✅ `warehouse_key` = valor de CVE_ALM
- ✅ `name_warehouse` = valor de CVE_ALM (si no se proporciona nombre)
- ✅ `observations` = "Este almacén no existía y fue creado en la importación"

### 2. Creación Automática de Productos
Si en el archivo aparece un `CVE_ART` que no existe en la tabla `products`:
- ✅ Se crea el producto automáticamente
- ✅ `cve_art` = valor de CVE_ART
- ✅ `descr` = valor de DESCR del Excel (o CVE_ART si DESCR está vacío)
- ✅ `status` = "A" (Alta)
- ✅ `uni_med` = "PZA" (por defecto)

### 3. Productos Nuevos en MultiAlmacén
Si el producto existe en inventario pero NO existe en multialmacén para el periodo:
- ✅ Se importa el registro
- ✅ Se crea nueva entrada en `multiwarehouse_existences`

### 4. Actualización de Productos Existentes
Si el producto ya existe en multialmacén para el periodo:
- ✅ Se actualizan las existencias (EXIST)
- ✅ Se actualiza el estado (STATUS)
- ✅ Se actualiza la descripción desde el inventario (no del Excel)

### 5. Productos Marcados como Baja
Si un producto existe en multialmacén pero NO aparece en el Excel:
- ✅ Su estado cambia automáticamente a "B" (Baja)
- ⚠️ Las existencias NO se modifican

## Ejemplo de Archivo Real

### Escenario: Empresa con 3 almacenes

```excel
CVE_ALM     CVE_ART      DESCR                          STATUS  EXIST
CEDIS       LAP-001      Laptop Dell Inspiron 15        A       50
CEDIS       MOU-001      Mouse Logitech M185            A       200
CEDIS       TEC-001      Teclado HP K200                A       150
TIENDA_MTY  LAP-001      Laptop Dell Inspiron 15        A       10
TIENDA_MTY  MOU-001      Mouse Logitech M185            A       25
TIENDA_GDL  LAP-001      Laptop Dell Inspiron 15        A       8
TIENDA_GDL  MOU-001      Mouse Logitech M185            B       0
```

### Resultado de la Importación:

1. **Almacenes creados** (si no existen):
   - CEDIS
   - TIENDA_MTY
   - TIENDA_GDL

2. **Productos creados** (si no existen):
   - LAP-001
   - MOU-001
   - TEC-001

3. **Registros en multiwarehouse_existences:**
   - 7 registros creados/actualizados
   - CEDIS tiene 3 productos
   - TIENDA_MTY tiene 2 productos
   - TIENDA_GDL tiene 2 productos (uno en estado Baja)

## Validaciones del Sistema

### Validaciones Obligatorias
El archivo será RECHAZADO si:
- ❌ Falta la columna CVE_ALM
- ❌ Falta la columna CVE_ART
- ❌ Falta la columna STATUS
- ❌ Falta la columna EXIST
- ❌ El formato del archivo es inválido
- ❌ El periodo está CLOSED o LOCKED

### Validaciones por Fila
Una fila será IGNORADA si:
- ⚠️ CVE_ALM está vacío
- ⚠️ CVE_ART está vacío
- ⚠️ EXIST no es un número válido
- ⚠️ STATUS no es "A" o "B"

## Formatos Alternativos de Columnas

El sistema acepta nombres alternativos para las columnas:

### Para CVE_ALM:
- `CVE_ALM`
- `cve_alm`
- `almacen_clave`
- `warehouse_key`

### Para CVE_ART:
- `CVE_ART`
- `cve_art`
- `producto`
- `product`
- `codigo`
- `product_code`

### Para DESCR:
- `DESCR`
- `descr`
- `descripcion`
- `description`
- `producto_nombre`
- `product_name`

### Para STATUS:
- `STATUS`
- `status`
- `estado`

### Para EXIST:
- `EXIST`
- `exist`
- `existencias`
- `stock`
- `cantidad`

## Conversión desde CSV

Si prefiere usar CSV en lugar de Excel:

### Formato CSV con punto y coma (;)
```csv
CVE_ALM;CVE_ART;DESCR;STATUS;EXIST
CEDIS;LAP-001;Laptop Dell Inspiron 15;A;50
CEDIS;MOU-001;Mouse Logitech M185;A;200
```

### Formato CSV con coma (,)
```csv
CVE_ALM,CVE_ART,DESCR,STATUS,EXIST
CEDIS,LAP-001,"Laptop Dell Inspiron 15",A,50
CEDIS,MOU-001,"Mouse Logitech M185",A,200
```

**Nota:** Si la descripción contiene comas, debe estar entre comillas.

## Codificación de Caracteres

- **Recomendado:** UTF-8
- **Alternativo:** ISO-8859-1 (Latin-1)
- **Soporta:** Caracteres especiales en español (á, é, í, ó, ú, ñ, ¿, ¡)

## Tamaño Máximo del Archivo

- **Recomendado:** Hasta 10,000 registros por archivo
- **Máximo:** 100,000 registros (puede afectar el rendimiento)
- **Tamaño en disco:** Hasta 50 MB

## Frecuencia de Importación

- **Recomendado:** Una vez al mes por periodo
- **Permitido:** Múltiples importaciones (actualiza datos)
- **Restricción:** No se puede importar el mismo archivo dos veces (hash duplicado)

## Soporte

Para más información, consulte:
- `/docs/CORRECCION-MULTIALMACEN-REGLAS-NEGOCIO.md`
- `/docs/TESTING-MULTIALMACEN.md`
- `/docs/GUIA-USO-CATALOGO-INVENTARIO.md`

## Contacto

Para soporte técnico, contacte a:
- TOKAI de México S.A. de C.V.
- Departamento de Sistemas

