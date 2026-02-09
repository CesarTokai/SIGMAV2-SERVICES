# 🔧 LIMPIEZA URGENTE: Duplicados de Productos en BD

## ⚠️ Situación Actual

El error persiste porque:
1. ✗ Los duplicados NO se han eliminado de la BD
2. ✗ La migración Flyway aún no se ejecutó o falló
3. ✗ Hay aún ~1500+ productos con códigos duplicados

## ✅ Solución Inmediata

Necesitas ejecutar **manualmente** el script SQL de limpieza en tu BD MySQL:

### Opción 1: Usando MySQL Command Line (Recomendado)

```bash
# Desde cualquier terminal con acceso a MySQL
mysql -h localhost -u root -p nombre_base_datos < CLEANUP-DUPLICATE-PRODUCTS.sql
```

### Opción 2: Usando MySQL Workbench

1. Abre MySQL Workbench
2. Conecta a tu BD
3. Abre el archivo: `CLEANUP-DUPLICATE-PRODUCTS.sql`
4. Ejecuta el script completo (⚡ + Enter)

### Opción 3: Usando PhpMyAdmin

1. Accede a PhpMyAdmin
2. Selecciona tu BD
3. Ve a la pestaña "SQL"
4. Copia y pega el contenido de `CLEANUP-DUPLICATE-PRODUCTS.sql`
5. Click en "Ejecutar"

---

## 📋 Qué hace el script

```sql
1. Elimina snapshots de productos que serán borrados
2. Elimina productos duplicados (mantiene el ID más bajo)
3. Verifica que no queden duplicados
```

---

## 🔍 Verificar que funcionó

Después de ejecutar, deberías ver:

```
-- Resultado esperado: sin filas (vacío)
SELECT cve_art, COUNT(*) as cantidad
FROM products
GROUP BY cve_art
HAVING COUNT(*) > 1;
```

Si sale una fila con `cantidad: 2`, aún hay duplicados.

---

## 🚀 Pasos Finales

1. ✅ Ejecuta el script SQL
2. ✅ Reinicia la aplicación Spring Boot
3. ✅ Las migraciones Flyway completarán los cambios
4. ✅ Intenta la importación nuevamente

---

## ❌ Si sigue sin funcionar

**Verificar campos en la consulta:**

```sql
-- Ver exactamente qué se está duplicando
SELECT cve_art, COUNT(*) as duplicados
FROM products
GROUP BY cve_art
HAVING COUNT(*) > 1
LIMIT 10;

-- Ver los IDs de un producto duplicado (ejemplo)
SELECT id_product, cve_art, descr, created_at
FROM products
WHERE cve_art = 'CLR-1CSC3'
ORDER BY id_product;
```

---

## 📝 Archivos Generados

- `CLEANUP-DUPLICATE-PRODUCTS.sql` - Script de limpieza
- `V1_0_10__Clean_duplicate_products_and_add_unique_constraint.sql` - Migración Flyway
- `ProductEntity.java` - Entidad con UNIQUE constraint

---

**¡Ejecuta el script y reporta si el problema se resuelve!**

