# SIGMAV2 2.0 - Release Notes

**Fecha de Lanzamiento**: 23 de Marzo de 2026  
**Versión**: 2.0.0  
**Estado**: Estable

---

## Descripcion General

SIGMAV2 2.0 es una version importante que introduce mejoras significativas en la visualizacion de datos, generacion de etiquetas, y reportes. Este release se enfoca en optimizar el flujo de trabajo para usuarios y mejorar la precisión de los registros de inventario.

---

## Caracteristicas Principales

### 1. Sistema de Seleccion Inteligente de Productos

- Los usuarios pueden generar etiquetas SOLO para los productos que modifican en la sesion actual
- Se implemento el sistema `isNewlyModified` para rastrear cambios en tiempo real
- Previene generacion accidental de marbetes innecesarios

### 2. Mejora en la Visualizacion de Folios Existentes

- Correccion del mapeo de datos desde el backend
- Sistema de multiples fallbacks para detectar campos de folios
- Ahora muestra correctamente la cantidad de folios generados para cada producto
- Soporta 8 variantes diferentes de nombres de campo del API

### 3. Reorganizacion de Tablas de Reportes

- Se reorganizaron las columnas en reportes comparativos para mejor legibilidad
- Nuevo orden: Almacen, Producto, Descripcion, Unidad, Existencias Fisicas, Existencias Teoricas, Diferencia
- Se elimino la columna "% Diferencia" para simplificar la visualizacion

### 4. Agrupacion de Datos por Almacen y Producto

- Los marbetes se agrupan automaticamente por almacen y producto
- Se muestran todos los folios relacionados en una sola fila
- Mejora significativa en la legibilidad de los reportes de inventario

### 5. Actualizacion del Estado de Cancelacion

- La columna de Estado ahora muestra:
  - "Cancelado" si el marbete esta cancelado
  - Vacio si el marbete esta activo
- Se elimino la visualizacion de estados redundantes

---

## Mejoras Tecnicas

### Backend - Mapeo de Datos

Se implemento un sistema robusto de mapeo que maneja multiples formatos de respuesta del API:

- `foliosExistentes` - Campo principal
- `generatedFolios` - Alternativa en ingles
- `foliosGenerados` - Alternativa en espanol
- Calculo automatico desde array `folios`
- Calculo desde rango `primerFolio` - `ultimoFolio`

### Frontend - Validaciones Mejoradas

Se fortalecieron las validaciones en los puntos criticos:

- Validacion de periodo cerrado antes de generar etiquetas
- Validacion de seleccion de productos para evitar generacion vacia
- Mensajes de error descriptivos y especificos
- Logging automatico para debugging

### Paginacion y Busqueda

- Se mantuvo paginacion consistente en todos los reportes
- Busqueda flexible que ignora espacios y mayusculas/minusculas
- Debounce de 500ms en busquedas para optimizar rendimiento

---

## Cambios de Interfaz de Usuario

### Eliminacion de Columnas

Se removieron las siguientes columnas para simplificar:

- "Cant. Folios" en ImpresionMarbetes.vue (todos los modulos)
- "% Diferencia" en ComparativosMarbetes.vue

### Reorganizacion de Columnas

Nuevo orden en ComparativosMarbetes.vue:

1. Clave Almacen
2. Nombre Almacen
3. Clave Producto
4. Descripcion Producto
5. Unidad
6. Exist. Fisicas (antes estaba Exist. Teoricas)
7. Exist. Teoricas
8. Diferencia

### Nuevas Interfaces Agrupadas

En AlmacenDetalle.vue:

- Agrupacion por Almacen + Producto
- Visualizacion de multiples marbetes en lista horizontal
- Badges de estado mejorados
- Mejor manejo de productos cancelados

---

## Correcciones de Bugs

### Bug: Generacion de Todos los Productos

SOLUCIONADO: El sistema generaba todos los productos con `foliosSolicitados > 0` sin importar cual queria el usuario.

- Implementacion de filtro `isNewlyModified`
- Se procesan SOLO los productos modificados en la sesion actual
- Modal de confirmacion muestra cantidad exacta a generar

### Bug: Folios Existentes Mostrava 0

SOLUCIONADO: El campo `foliosExistentes` siempre mostraba 0 aunque habia folios generados.

- Correccion del mapeo de datos desde el backend
- Implementacion de sistema de fallbacks
- Ahora detecta el campo correcto independientemente de su nombre

### Bug: Campos Bloqueados Incorrectamente

SOLUCIONADO: Los campos se bloqueaban incorrectamente cuando no habia folios generados.

- El bloqueo ahora responde correctamente a `foliosExistentes`
- Usuario puede modificar campos solo cuando es necesario

---

## Cambios en Archivos

### Modulos Actualizados

ADMIN:
- `/admin/views/marbetesAdmin/ConsultaCaptura.vue` - Seleccion inteligente
- `/admin/views/marbetesAdmin/ImpresionMarbetes.vue` - Eliminacion columna
- `/admin/views/reportesAdmin/ComparativosMarbetes.vue` - Reorden columnas
- `/admin/views/reportesAdmin/AlmacenDetalle.vue` - Agrupacion de datos

ALMACENISTA:
- `/almacenista/views/marbetes/ConsultaCaptura.vue` - Mejoras mapeo
- `/almacenista/views/marbetes/ImpresionMarbetes.vue` - Eliminacion columna

AUXILIAR:
- `/auxiliar/views/marbetes/ConsultaCaptura.vue` - Mejoras mapeo
- `/auxiliar/views/marbetes/ImpresionMarbetes.vue` - Eliminacion columna

AUXILIAR DE CONTEO:
- `/auxiliar_de_conteo/views/marbetes/ConsultaCaptura.vue` - Mejoras mapeo
- `/auxiliar_de_conteo/views/marbetes/ImpresionMarbetes.vue` - Eliminacion columna

---

## Notas Importantes para Desarrolladores

### Patrones Implementados

1. **Mapeo Robusto de Datos**
   - Siempre incluir multiples variantes de nombres de campo
   - Implementar fallbacks en cascada
   - Loguear primer item para debugging

2. **Validaciones Pre-API**
   - Validar periodo y almacen antes de enviar
   - Verificar estado del marbete antes de generar
   - Mostrar mensajes de error descriptivos

3. **Estado de Carga**
   - Usar `loadingStates` granular por operacion
   - Siempre llamar `LoadAlert(false)` en finally
   - Deshabilitar botones durante operaciones

4. **Paginacion**
   - Resetear pagina a 0 en cambios de filtro
   - Mantener totales actualizados
   - Validar limites de paginacion

---

## Instrucciones de Actualizacion

### Desde SIGMAV2 1.x

1. Realizar backup de la base de datos
2. Actualizar dependencias: `npm install`
3. Ejecutar build: `npm run build`
4. Limpiar cache del navegador
5. Verificar acceso con cada rol de usuario

### Rollback

Si es necesario volver a la version anterior:

1. Revertir cambios del repositorio
2. Limpiar cache de navegador
3. Ejecutar `npm install` nuevamente
4. Rebuild y redeploy

---

## Testing Recomendado

### Casos de Prueba Criticos

1. Generacion de etiquetas con producto individual
2. Visualizacion correcta de folios existentes
3. Bloqueo/desbloqueo de campos segun folios
4. Reportes comparativos con datos agrupados
5. Estado "Cancelado" en todos los reportes

### Entornos a Probar

- Chrome/Edge (version reciente)
- Firefox (version reciente)
- Todos los roles de usuario (Admin, Almacenista, Auxiliar, Auxiliar de Conteo)

---

## Conocidos - Issues Abiertos

### Ninguno en este release

Todos los bugs identificados fueron solucionados antes del lanzamiento.

---

## Proximo Release (v2.1)

Se estan planificando las siguientes mejoras para la proxima version:

- Dashboard mejorado con estadisticas en tiempo real
- Exportacion mejorada a Excel
- Integracion con sistemas de etiquetado fisico
- Mejoras en rendimiento para grandes volumenes de datos

---

## Soporte y Reportes de Bugs

Para reportar bugs o solicitar features:

- Crear un issue en el repositorio del proyecto
- Incluir detalles: version, rol de usuario, pasos para reproducir
- Adjuntar screenshots o logs cuando sea posible

---

## Agradecimientos

Se agradece a todo el equipo de QA que participo en las pruebas exhaustivas de esta version.

---

**SIGMAV2 Development Team**  
Marzo 2026

