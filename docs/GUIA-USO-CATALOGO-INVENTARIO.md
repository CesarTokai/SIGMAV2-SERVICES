# 🚀 Guía Rápida de Uso - Catálogo de Inventario

## Para Desarrolladores

### 1. Compilar el Proyecto
```bash
cd C:\Users\cesarg\Desktop\SIGMAV2\SIGMAV2
.\mvnw.cmd clean install
```

### 2. Ejecutar el Servidor
```bash
.\mvnw.cmd spring-boot:run
```

### 3. Acceder a la Aplicación
Abrir en navegador:
```
http://localhost:8080/inventory-catalog.html
```

## Para Usuarios Finales (Administradores)

### Acceso al Catálogo de Inventario

1. **Iniciar sesión** en SIGMA con credenciales de Administrador

2. **Navegar** a: http://localhost:8080/inventory-catalog.html

3. **Seleccionar Periodo**:
   - El sistema cargará automáticamente el último periodo registrado
   - Puede cambiar el periodo usando el menú desplegable superior izquierdo

### Funciones Disponibles

#### 🔍 Búsqueda
- Escribir en el campo "Buscar" en la esquina superior derecha
- La búsqueda filtra por:
  - Clave de Producto
  - Nombre del Producto
  - Unidad de Medida
- Los resultados se actualizan automáticamente mientras escribe

#### 📊 Ordenación
- Hacer **clic en cualquier encabezado de columna** para ordenar
- Un segundo clic invierte el orden (ascendente ⟷ descendente)
- Columnas ordenables:
  - ✅ Clave de Producto
  - ✅ Producto
  - ✅ Unidad
  - ✅ Existencias
  - ✅ Estado

#### 📄 Paginación
- **Cambiar tamaño de página**: Seleccionar 10, 25, 50 o 100 registros
- **Navegar páginas**:
  - "Primera" - Va a la primera página
  - "Anterior" - Página anterior
  - "Siguiente" - Página siguiente
  - "Última" - Va a la última página

#### 🏷️ Estados de Producto
- **A (Alta)**: Producto activo y disponible - Badge verde
- **B (Baja)**: Producto dado de baja - Badge rojo

### Ejemplos de Uso

#### Ejemplo 1: Consultar inventario actual
1. La página carga automáticamente el último periodo
2. Ver lista de productos con existencias

#### Ejemplo 2: Buscar un producto específico
1. Seleccionar el periodo deseado
2. Escribir código o nombre del producto en "Buscar"
3. Ver resultados filtrados

#### Ejemplo 3: Ver productos con menos existencias
1. Seleccionar periodo
2. Hacer clic en columna "Existencias"
3. Los productos con menos existencias aparecen primero

#### Ejemplo 4: Ver solo productos activos
1. Hacer clic en columna "Estado" dos veces
2. Productos con estado "A" (Alta) aparecen primero

## Solución de Problemas

### ❌ "Error al cargar los periodos"
**Solución**:
- Verificar que el servidor esté ejecutándose
- Verificar que tiene sesión activa como Administrador
- Verificar que existen periodos en la base de datos

### ❌ "No se encontraron productos"
**Posibles causas**:
- No hay inventario registrado para el periodo seleccionado
- El filtro de búsqueda es muy específico
- **Solución**: Borrar el texto de búsqueda o seleccionar otro periodo

### ❌ Página en blanco
**Solución**:
- Limpiar caché del navegador (Ctrl + Shift + Del)
- Verificar consola de JavaScript (F12)
- Verificar que el token de autenticación no haya expirado

## Características Técnicas

### Rendimiento
- Paginación en servidor (no carga todo en memoria)
- Búsqueda optimizada con índices de base de datos
- Debounce en búsqueda (reduce llamadas al servidor)

### Compatibilidad
- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Edge 90+
- ✅ Safari 14+

### Seguridad
- Solo accesible por usuarios con rol Administrador
- Autenticación mediante JWT
- Datos encriptados en tránsito (HTTPS recomendado)

## Mejoras Futuras Planificadas

- 📥 Exportar a Excel
- 📊 Gráficos estadísticos
- 🔔 Alertas de stock bajo
- 📱 Versión móvil optimizada
- 🌐 Multi-idioma

## Soporte

Para reportar problemas o solicitar nuevas funciones, contactar a:
- Equipo de Desarrollo SIGMA
- Email: soporte@tokai.com.mx

---

**Versión:** 1.0
**Última actualización:** 24 de Noviembre de 2025

