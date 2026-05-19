# Solución: Duplicados de Productos (cve_art)

## Problema Identificado

El error **"Query did not return a unique result: 2 results were returned"** indica que existen **múltiples registros con el mismo código de artículo (`cve_art`)** en la tabla `products`.

Esto ocurre porque el método `findByCveArt()` espera devolver un único resultado, pero encuentra 2 o más registros con el mismo código.

## Cambios Implementados

### 1. **Migración SQL (V1_0_10)**
Archivo: `V1_0_10__Clean_duplicate_products_and_add_unique_constraint.sql`

```sql
-- Elimina productos duplicados (mantiene el más antiguo)
-- Agrega restricción UNIQUE a la columna cve_art
-- Limpia snapshots asociados a productos duplicados
```

**Lo que hace:**
- Identifica todos los `cve_art` que tienen múltiples registros
- Mantiene el registro más antiguo (ID más bajo)
- Elimina todos los duplicados
- Agrega una restricción UNIQUE a nivel de BD

### 2. **Cambios en la Entidad (ProductEntity.java)**

```java
@Column(name = "cve_art", unique = true, nullable = false)
private String cveArt;
```

Con uniqueConstraints en la anotación @Table:
```java
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(columnNames = "cve_art", name = "uk_products_cve_art")
})
```

### 3. **Mejora en el Servicio de Importación**

El método `processProduct()` ahora:
- Captura excepciones específicamente
- Proporciona mensajes de error más descriptivos
- Maneja errores de constraint violations

## Pasos para Resolver

### Opción 1: Automático (Recomendado)

1. **Ejecuta las migraciones:**
   - La aplicación ejecutará automáticamente `V1_0_10` cuando inicie
   - Esto limpiará los duplicados y agregará la restricción

2. **Reinicia la aplicación**

### Opción 2: Manual

Si prefieres ejecutar manualmente en MySQL:

```sql
-- 1. Identificar duplicados
SELECT cve_art, COUNT(*) as cantidad
FROM products
GROUP BY cve_art
HAVING COUNT(*) > 1;

-- 2. Ver cuáles se van a eliminar
SELECT id_product, cve_art, created_at
FROM products
WHERE cve_art IN (
    SELECT cve_art
    FROM products
    GROUP BY cve_art
    HAVING COUNT(*) > 1
)
ORDER BY cve_art, created_at;

-- 3. Ejecutar la migración manualmente
-- Ver archivo: V1_0_10__Clean_duplicate_products_and_add_unique_constraint.sql
```

## Resultado

Después de aplicar estos cambios:
- ✅ No habrá más duplicados de `cve_art`
- ✅ La BD impedirá crear duplicados en el futuro
- ✅ El error "Query did not return a unique result" no volverá a ocurrir
- ✅ La importación de inventario funcionará correctamente

## Verificación

Para verificar que se solucionó:

```sql
-- Verificar que no hay duplicados
SELECT cve_art, COUNT(*) as cantidad
FROM products
GROUP BY cve_art
HAVING COUNT(*) > 1;
-- Resultado: sin filas (vacío)

-- Verificar que existe la restricción UNIQUE
SHOW INDEXES FROM products WHERE Key_name = 'uk_products_cve_art';
```

