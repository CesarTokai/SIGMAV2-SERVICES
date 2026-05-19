# Checklist de Verificación - Impresión de Marbetes

Este documento proporciona un checklist completo para verificar que todas las reglas de negocio de impresión de marbetes están funcionando correctamente.

---

## 📋 Checklist de Pruebas

### 1. Control de Acceso por Rol

#### 1.1 Usuario ADMINISTRADOR
- [ ] Puede seleccionar y cambiar entre diferentes almacenes
- [ ] Puede imprimir marbetes en cualquier almacén sin restricciones
- [ ] No recibe error de permisos al acceder a almacenes no asignados

#### 1.2 Usuario AUXILIAR
- [ ] Puede seleccionar y cambiar entre diferentes almacenes
- [ ] Puede imprimir marbetes en cualquier almacén sin restricciones
- [ ] No recibe error de permisos al acceder a almacenes no asignados

#### 1.3 Usuario ALMACENISTA
- [ ] Solo puede imprimir en su almacén asignado
- [ ] Recibe error de permisos al intentar imprimir en otro almacén
- [ ] Mensaje de error es claro: "No tiene acceso al almacén especificado"

#### 1.4 Usuario AUXILIAR_DE_CONTEO
- [ ] Solo puede imprimir en su almacén asignado
- [ ] Recibe error de permisos al intentar imprimir en otro almacén
- [ ] Mensaje de error es claro: "No tiene acceso al almacén especificado"

---

### 2. Validación de Catálogos Cargados

#### 2.1 Sin Catálogos Cargados
- [ ] Sistema verifica existencia de datos en `inventory_stock`
- [ ] No permite imprimir si faltan catálogos
- [ ] Mensaje de error es claro: "No se pueden imprimir marbetes porque no se han cargado los catálogos..."
- [ ] Indica específicamente que faltan catálogos de inventario y multialmacén

#### 2.2 Con Catálogos Cargados
- [ ] Sistema permite continuar con la impresión
- [ ] Verifica que los catálogos sean del periodo correcto
- [ ] Verifica que los catálogos sean del almacén correcto

#### 2.3 Catálogos Parciales
- [ ] Si solo existe inventario sin multialmacén, no permite imprimir
- [ ] Si solo existe multialmacén sin inventario, no permite imprimir

---

### 3. Validación de Rango de Folios

#### 3.1 Rango Válido
- [ ] Acepta folioInicial < folioFinal
- [ ] Acepta folioInicial = folioFinal (un solo folio)
- [ ] Procesa correctamente rangos pequeños (1-10 folios)
- [ ] Procesa correctamente rangos medianos (50-100 folios)
- [ ] Procesa correctamente rangos grandes (hasta 500 folios)

#### 3.2 Rango Inválido
- [ ] Rechaza folioInicial > folioFinal
- [ ] Mensaje de error: "El folio inicial no puede ser mayor que el folio final"
- [ ] Rechaza rangos mayores a 500 folios
- [ ] Mensaje de error: "Máximo 500 folios por lote"

#### 3.3 Folios Faltantes
- [ ] Detecta cuando faltan folios en el rango
- [ ] Lista específicamente qué folios faltan
- [ ] Mensaje de error: "No es posible imprimir marbetes no generados. Folios faltantes: X, Y, Z"

---

### 4. Impresión Normal

#### 4.1 Primera Impresión
- [ ] Permite imprimir marbetes en estado GENERADO
- [ ] Cambia estado de GENERADO a IMPRESO
- [ ] Actualiza campo `impresoAt` con fecha/hora actual
- [ ] Registra usuario que imprimió en el marbete
- [ ] Crea registro en tabla `label_prints`
- [ ] Log indica: "Impresión exitosa: X folio(s) impresos del Y al Z"

#### 4.2 Marbetes Recién Generados
- [ ] Sistema muestra por default el último rango de folios generados
- [ ] Usuario puede ver rango sugerido antes de imprimir
- [ ] Usuario puede modificar el rango si lo desea

---

### 5. Impresión Extraordinaria (Reimpresión)

#### 5.1 Reimpresión de Marbetes
- [ ] Permite reimprimir marbetes en estado IMPRESO
- [ ] Mantiene estado IMPRESO (no cambia)
- [ ] Actualiza campo `impresoAt` con nueva fecha/hora
- [ ] Crea nuevo registro en tabla `label_prints` para auditoría
- [ ] Log indica: "Impresión exitosa: X folio(s) impresos del Y al Z"

#### 5.2 Rango Personalizado
- [ ] Usuario puede ingresar cualquier rango de folios
- [ ] Sistema valida que los folios existan
- [ ] Sistema valida que los folios pertenezcan al periodo/almacén
- [ ] Permite reimprimir folios no consecutivos (con saltos)

#### 5.3 Reimpresión de un Solo Folio
- [ ] Usuario puede ingresar mismo número en folioInicial y folioFinal
- [ ] Sistema imprime solo ese folio
- [ ] Registra correctamente en auditoría

---

### 6. Validación de Estados de Marbetes

#### 6.1 Marbetes GENERADOS
- [ ] Se pueden imprimir por primera vez
- [ ] Cambian a estado IMPRESO después de imprimir

#### 6.2 Marbetes IMPRESOS
- [ ] Se pueden reimprimir (impresión extraordinaria)
- [ ] Mantienen estado IMPRESO

#### 6.3 Marbetes CANCELADOS
- [ ] NO se pueden imprimir
- [ ] Mensaje de error: "No es posible imprimir marbetes cancelados. Folio: X"
- [ ] Sistema identifica específicamente qué folio está cancelado

---

### 7. Validación de Pertenencia

#### 7.1 Periodo Correcto
- [ ] Verifica que folios pertenezcan al periodo seleccionado
- [ ] Si folio es de otro periodo, muestra error claro
- [ ] Mensaje: "El folio X no pertenece al periodo/almacén seleccionado"

#### 7.2 Almacén Correcto
- [ ] Verifica que folios pertenezcan al almacén seleccionado
- [ ] Si folio es de otro almacén, muestra error claro
- [ ] Mensaje: "El folio X no pertenece al periodo/almacén seleccionado"

---

### 8. Registro de Auditoría

#### 8.1 Tabla label_prints
- [ ] Se crea un registro por cada operación de impresión
- [ ] Incluye: periodId, warehouseId, folioInicial, folioFinal
- [ ] Incluye: cantidadImpresa, printedBy, printedAt
- [ ] Registros de reimpresión son diferenciables de impresión normal

#### 8.2 Actualización de Marbetes
- [ ] Campo `estado` se actualiza correctamente
- [ ] Campo `impresoAt` se actualiza en cada impresión/reimpresión
- [ ] Campo `printedBy` se actualiza correctamente

---

### 9. Logging y Monitoreo

#### 9.1 Logs Informativos
- [ ] Log al inicio: muestra todos los parámetros de entrada
- [ ] Log de validación de rol
- [ ] Log de cantidad de folios a imprimir
- [ ] Log de éxito: muestra cantidad impresa y rango

#### 9.2 Logs de Error
- [ ] Log cuando falta validación de acceso
- [ ] Log cuando faltan catálogos
- [ ] Log cuando hay folios faltantes
- [ ] Log cuando hay folios cancelados
- [ ] Todos los logs incluyen contexto suficiente para debugging

---

### 10. Interfaz de Usuario

#### 10.1 Selección de Periodo y Almacén
- [ ] Usuario puede seleccionar periodo de lista desplegable
- [ ] Usuario puede seleccionar almacén de lista desplegable
- [ ] Al cambiar periodo/almacén, se actualiza rango sugerido
- [ ] Al cambiar periodo/almacén, se actualiza listado de marbetes

#### 10.2 Sección de Impresión
- [ ] Muestra rango sugerido por default (últimos folios generados)
- [ ] Usuario puede modificar folioInicial
- [ ] Usuario puede modificar folioFinal
- [ ] Botón "Exportar folios" está visible y funcional

#### 10.3 Listado de Marbetes
- [ ] Muestra todos los marbetes del periodo/almacén
- [ ] Columna "Impreso" indica SI o NO
- [ ] Permite buscar marbetes
- [ ] Permite ordenar por columnas
- [ ] Permite paginar resultados

---

### 11. Mensajes de Usuario

#### 11.1 Mensajes de Éxito
- [ ] "Impresión exitosa: X folio(s) impresos del Y al Z"
- [ ] Mensaje es claro y específico
- [ ] Incluye cantidad exacta de folios impresos

#### 11.2 Mensajes de Error
- [ ] Errores de permisos son claros
- [ ] Errores de catálogos indican qué falta
- [ ] Errores de validación indican qué está mal
- [ ] Todos los mensajes son en español
- [ ] Todos los mensajes son comprensibles para usuario final

---

### 12. Casos Extremos

#### 12.1 Rangos Grandes
- [ ] Máximo 500 folios por operación
- [ ] Sistema sugiere dividir en múltiples operaciones
- [ ] Performance es aceptable con 500 folios

#### 12.2 Múltiples Impresiones Simultáneas
- [ ] Sistema maneja correctamente impresiones concurrentes
- [ ] No hay race conditions
- [ ] Transacciones son atómicas

#### 12.3 Datos Faltantes
- [ ] Maneja correctamente cuando no hay marbetes generados
- [ ] Maneja correctamente cuando no hay periodo seleccionado
- [ ] Maneja correctamente cuando no hay almacén seleccionado

---

### 13. Integración con Otros Módulos

#### 13.1 Módulo de Inventario
- [ ] Verifica correctamente existencia de datos en inventory_stock
- [ ] Respeta periodo seleccionado
- [ ] Respeta almacén seleccionado

#### 13.2 Módulo de Marbetes
- [ ] Se integra correctamente con solicitud de folios
- [ ] Se integra correctamente con generación de marbetes
- [ ] Respeta estados de marbetes definidos en el sistema

#### 13.3 Módulo de Conteo
- [ ] Los marbetes impresos pueden ser contados
- [ ] No interfiere con proceso de conteo C1/C2

---

### 14. Seguridad

#### 14.1 Autenticación
- [ ] Requiere token válido
- [ ] Rechaza tokens expirados
- [ ] Rechaza tokens inválidos

#### 14.2 Autorización
- [ ] Valida permisos según rol
- [ ] Valida acceso al almacén
- [ ] No permite bypass de validaciones

---

### 15. Performance

#### 15.1 Tiempos de Respuesta
- [ ] Impresión de 1 folio: < 1 segundo
- [ ] Impresión de 50 folios: < 2 segundos
- [ ] Impresión de 500 folios: < 5 segundos
- [ ] Consulta de listado: < 2 segundos

#### 15.2 Carga del Sistema
- [ ] No afecta otros módulos durante impresión
- [ ] Base de datos no se sobrecarga
- [ ] Memoria se libera correctamente después de impresión

---

## 📊 Resumen de Verificación

### Funcionalidades Críticas
- [ ] Control de acceso por rol funciona correctamente
- [ ] Validación de catálogos cargados funciona
- [ ] Validación de rango de folios funciona
- [ ] Impresión normal funciona
- [ ] Impresión extraordinaria (reimpresión) funciona
- [ ] Registro de auditoría funciona
- [ ] Logging está implementado

### Reglas de Negocio
- [ ] Se cumple: ADMINISTRADOR/AUXILIAR pueden cambiar almacén
- [ ] Se cumple: No se imprime sin catálogos cargados
- [ ] Se cumple: Se valida rango de folios
- [ ] Se cumple: Se soporta impresión normal
- [ ] Se cumple: Se soporta impresión extraordinaria
- [ ] Se cumple: No se imprimen marbetes cancelados
- [ ] Se cumple: Se registran todas las impresiones

---

## ✅ Criterios de Aceptación

Para que la funcionalidad sea considerada completa y aceptada:

1. ✅ Todas las reglas de negocio implementadas
2. ✅ Compilación sin errores
3. ✅ Todos los casos de prueba pasados
4. ✅ Documentación completa
5. ✅ Logging implementado
6. ✅ Manejo de errores correcto
7. ✅ Performance aceptable

---

## 📝 Notas de Testing

### Ambiente de Prueba
- **Base de datos:** PostgreSQL con datos de prueba
- **Usuarios de prueba:** Uno por cada rol (ADMINISTRADOR, AUXILIAR, ALMACENISTA, AUXILIAR_DE_CONTEO)
- **Periodos de prueba:** Al menos 2 periodos con datos
- **Almacenes de prueba:** Al menos 2 almacenes (250, 300)

### Datos de Prueba Necesarios
- Catálogos de inventario cargados
- Catálogos de multialmacén cargados
- Marbetes generados en diferentes estados (GENERADO, IMPRESO, CANCELADO)
- Asignaciones de almacenes a usuarios

---

## Fecha de Creación
2 de diciembre de 2025

## Última Actualización
2 de diciembre de 2025

