# Manual de Pruebas Funcionales - SIGMAV2-APP

**Fecha:** 06/05/2026  
**Probador:** Cesar Uriel Gonzalez S.  
**Versión:** V1.1.3.0

---

## 1. AUTENTICACIÓN

### 1.1 Login Exitoso
**Descripción:** Usuario puede iniciar sesión con credenciales válidas

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Navegar a `/login` | Página de login carga |✓ | |
| 2 | Ingresar email: `cgonzalez@tokai.com.mx` | Campo acepta email | ✓| |
| 3 | Ingresar password: `Password123!` | Campo acepta password |✓ | |
| 4 | Click en "Ingresar" | Botón responde |✓ | |
| 5 | Esperar redirección | Se va a dashboard admin |✓ | |
| 6 | Verificar token | Token en localStorage |✓ | |

**Resultado Final: ✓ PASÓ ☐ FALLÓ

---

### 1.2 Login con Email Vacío
**Descripción:** Sistema muestra error si email está vacío

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Dejar email vacío | Campo vacío | ✗| |
| 2 | Ingresar password válido | Campo lleno |✗ | |
| 3 | Click en "Ingresar" | Muestra error |✗ | |
| 4 | Verificar mensaje | "usuario y contraseña son obligatorios" |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 1.3 Login con Contraseña Vacía
**Descripción:** Sistema muestra error si password está vacía

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Ingresar email válido | Campo lleno | ✓| |
| 2 | Dejar password vacío | Campo vacío | ✓| |
| 3 | Click en "Ingresar" | Muestra error |✓ | |
| 4 | Verificar mensaje | "usuario y contraseña son obligatorios" | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 1.4 Login con Credenciales Inválidas
**Descripción:** Sistema muestra error con credenciales incorrectas

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Ingresar email incorrecto | Campo lleno |✓ | |
| 2 | Ingresar password incorrecto | Campo lleno |✓ | |
| 3 | Click en "Ingresar" | Muestra error |✓ | |
| 4 | Verificar mensaje | Error del servidor |✓ | |
| 5 | Verificar token | NO se guarda token |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

## 2. DASHBOARD ADMIN

### 2.1 Dashboard Carga Correctamente
**Descripción:** Dashboard admin muestra información correcta

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Login exitoso | Redirección a dashboard | ✓| |
| 2 | Esperar carga | Dashboard visible |✓ | |
| 3 | Verificar sidebar | Sidebar visible a la izquierda | ✓| |
| 4 | Verificar título | Título "Gestión de Períodos" |✓ | |
| 5 | Verificar logo | Logo visible en sidebar | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 2.2 Información de Usuario en Sidebar
**Descripción:** Sidebar muestra información del usuario logueado

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Estar en dashboard | Dashboard cargado |✓ | |
| 2 | Mirar sidebar abajo | Footer con info usuario |✓ | |
| 3 | Verificar nombre | Nombre o email visible | ✓| |
| 4 | Verificar rol | "Administrador" visible |✓ | |
| 5 | Avatar visible | Avatar o iniciales | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 2.3 Botón Logout Funciona
**Descripción:** Logout limpia sesión y redirige a login

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|----|-------|
| 1 | Estar en dashboard | Dashboard visible |✓| |
| 2 | Buscar botón logout | Botón con icono power ⏻ | ✓| |
| 3 | Click en logout | Botón responde |✓ | |
| 4 | Esperar cambio | Token se limpia |✓ | |
| 5 | Verificar redirección | Va a login | ✓| |
| 6 | Verificar localStorage | Token vacío | ✓| |

**Resultado Final:**  ✓ PASÓ ☐ FALLÓ

---

## 3. GESTIÓN DE PERÍODOS

### 3.1 Ver Lista de Períodos
**Descripción:** Página muestra lista de períodos existentes

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en "Gestión de Períodos" en sidebar | Página carga |✓ | |
| 2 | Esperar carga | Tabla visible |✓ | |
| 3 | Verificar columnas | ID, Fecha, Comentarios, Estado, Acciones | ✓| |
| 4 | Verificar filas | Al menos 1 período |✓ | |
| 5 | Verificar datos | Fechas y comentarios visibles | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 3.2 Crear Nuevo Período
**Descripción:** Usuario puede crear un nuevo período

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en "Agregar Período" | Modal se abre | ✓| |
| 2 | Verificar campos | Fecha y Comentarios | ✓| |
| 3 | Ingresar fecha | Campo acepta fecha |✓ | |
| 4 | Ingresar comentarios | Campo acepta texto (min 10 caracteres) | ✓| |
| 5 | Click en "Guardar" | Modal se cierra |✓ | |
| 6 | Verificar alert | "Registro exitoso" | ✓| |
| 7 | Verificar tabla | Período aparece en tabla | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 3.3 Búsqueda en Períodos
**Descripción:** Búsqueda filtra períodos correctamente

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Estar en página de períodos | Tabla visible | | |
| 2 | Click en input de búsqueda | Campo enfocado | | |
| 3 | Escribir texto para buscar | Campo tiene valor | | |
| 4 | Esperar filtrado | Tabla se filtra | | |
| 5 | Verificar resultados | Solo períodos coincidentes | | |
| 6 | Limpiar búsqueda | Todos los períodos vuelven | | |

**Resultado Final:** ☐ PASÓ ☐ FALLÓ

---

### 3.4 Ver Detalles del Período
**Descripción:** Modal muestra detalles completos del período

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en icono "ojo" de un período | Modal se abre | ✓| |
| 2 | Verificar ID | ID visible | ✓| |
| 3 | Verificar Fecha | Fecha completa | ✓| |
| 4 | Verificar Comentarios | Texto completo |✓ | |
| 5 | Verificar Estado | OPEN o CLOSED | ✓| |
| 6 | Verificar fecha actualización | Si aplica | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

## 4. GESTIÓN DE INVENTARIO

### 4.1 Ver Inventario
**Descripción:** Página muestra productos del período seleccionado

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Navegar a Inventario Admin | Página carga |✓ | |
| 2 | Esperar carga | Tabla de productos visible |✓ | |
| 3 | Verificar selector período | Dropdown con períodos | ✓| |
| 4 | Verificar tabla | Columnas de productos |✓ | |
| 5 | Verificar datos | Productos con clave, descripción, etc |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 4.2 Cambiar Período en Inventario
**Descripción:** Cambiar período recarga datos del inventario

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Estar en inventario | Tabla visible |✓ | |
| 2 | Click en selector de período | Dropdown abre | ✓| |
| 3 | Seleccionar otro período | Opción se marca | ✓| |
| 4 | Esperar carga | Tabla se actualiza |✓ | |
| 5 | Verificar datos | Productos del nuevo período |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 4.3 Búsqueda en Inventario
**Descripción:** Búsqueda filtra productos

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en búsqueda | Input enfocado |✓ | |
| 2 | Escribir clave o nombre | Campo lleno | ✓| |
| 3 | Esperar filtrado | Tabla filtra productos | ✓| |
| 4 | Verificar resultados | Solo coincidentes |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

## 5. GESTIÓN DE ALMACENES

### 5.1 Ver Almacenes
**Descripción:** Tabla muestra lista de almacenes

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Navegar a Almacén Admin | Página carga | ✓| |
| 2 | Esperar tabla | Almacenes visibles | ✓| |
| 3 | Verificar columnas | Clave, Nombre, Observaciones, etc |✓ | |
| 4 | Verificar datos | Almacenes con información |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 5.2 Crear Almacén
**Descripción:** Crear nuevo almacén con datos válidos

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click "Nuevo Almacén" | Modal abre |✓ | |
| 2 | Ingresar clave | Campo lleno | ✓| |
| 3 | Ingresar nombre | Campo lleno |✓ | |
| 4 | Ingresar observaciones (opcional) | Campo aceptable |✓ | |
| 5 | Click "Guardar" | Modal cierra | ✓| |
| 6 | Verificar alert | "Éxito" | |✓ |
| 7 | Verificar tabla | Almacén aparece | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 5.3 Editar Almacén
**Descripción:** Editar datos de almacén existente

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click icono lápiz | Modal abre con datos | ✓| |
| 2 | Modificar campo | Cambio visible |✓ | |
| 3 | Click "Guardar" | Modal cierra |✓ | |
| 4 | Verificar alert | "Actualizado" | ✓| |
| 5 | Verificar tabla | Cambios aplicados | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 5.4 Búsqueda de Almacenes
**Descripción:** Búsqueda filtra almacenes

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en búsqueda | Input enfocado | ✓| |
| 2 | Escribir texto | Campo lleno |✓ | |
| 3 | Esperar filtrado | Tabla filtra | ✓| |
| 4 | Verificar resultados | Solo coincidentes | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

## 6. MULTI-ALMACÉN

### 6.1 Ver Productos Multi-Almacén
**Descripción:** Página muestra productos en múltiples almacenes

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Navegar a MultiAlmacén | Página carga |✓ | |
| 2 | Esperar tabla | Productos por almacén visibles |✓ | |
| 3 | Verificar columnas | Producto, Almacén, Existencias, Estado |✓ | |
| 4 | Verificar datos | Información completa |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 6.2 Cambiar Período en MultiAlmacén
**Descripción:** Cambio de período recarga datos

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Estar en página | Tabla visible |✓ | |
| 2 | Click selector período | Dropdown abre | ✓| |
| 3 | Seleccionar otro | Opción se marca |✓ | |
| 4 | Esperar carga | Tabla actualiza | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

## 7. GESTIÓN DE MARBETES

### 7.1 Ver Submódulos de Marbetes
**Descripción:** Todos los submódulos están disponibles

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Navegar a Marbetes | Página carga | ✓| |
| 2 | Verificar botones | Consulta, Impresión, Conteo, Reimpresión, Listado | ✓| |
| 3 | Verificar activo | "Consulta y Captura" activo por defecto |✓ | |
| 4 | Verificar estilos | Botón activo resalta |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 7.2 Navegar Entre Submódulos
**Descripción:** Cambiar entre submódulos funciona

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en "Impresión" | Botón se marca activo |✓ | |
| 2 | Esperar carga | Contenido cambia |✓ | |
| 3 | Click en "Conteo" | Botón se marca activo | ✓| |
| 4 | Esperar carga | Contenido cambia |✓ | |
| 5 | Click en "Listado" | Botón se marca activo |✓ | |
| 6 | Esperar carga | Contenido cambia |✓ | |

**Resultado Final:** ✓  PASÓ ☐ FALLÓ

---

## 8. REPORTES

### 8.1 Ver Listado de Marbetes
**Descripción:** Reporte muestra lista de marbetes

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Navegar a ListadoMarbetes | Página carga |✓ | |
| 2 | Esperar carga | Tabla visible |✓ | |
| 3 | Verificar selector período | Dropdown disponible |✓ | |
| 4 | Verificar tabla | Marbetes listados | ✓| |
| 5 | Verificar columnas | Número, Producto, Almacén, Estados | ✓| |

**Resultado Final:** ☐ PASÓ ☐ FALLÓ

---

### 8.2 Ver Marbetes Cancelados
**Descripción:** Reporte muestra solo marbetes cancelados

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Navegar a MarbetesCancelados | Página carga | ✓| |
| 2 | Esperar carga | Tabla visible |✓ | |
| 3 | Verificar datos | Solo cancelados |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 8.3 Ver Reportes Adicionales
**Descripción:** Otros reportes carguen correctamente

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Navegar a MarbetesPendientes | Carga |✓ | |
| 2 | Navegar a MarbetesConDiferencia | Carga |✓ | |
| 3 | Navegar a DistribucionMarbetes | Carga |✓ | |
| 4 | Navegar a ComparativosMarbetes | Carga | ✓| |
| 5 | Navegar a AlmacenDetalle | Carga |✓ | |
| 6 | Navegar a ProductoDetalle | Carga | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

## 9. GESTIÓN DE USUARIOS

### 9.1 Ver Lista de Usuarios
**Descripción:** Tabla muestra usuarios del sistema

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Navegar a Gestión de Usuarios | Página carga |✓ | |
| 2 | Esperar carga | Tabla visible |✓ | |
| 3 | Verificar tarjetas | Total, Admin, Almacenista, Auxiliar, Aux.Conteo |✓ | |
| 4 | Verificar números | Conteos correctos |✓ | |
| 5 | Verificar columnas | ID, Usuario, Email, Rol, Estado, Fecha |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 9.2 Buscar Usuario
**Descripción:** Búsqueda filtra usuarios por email/nombre

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en búsqueda | Input enfocado | ✓| |
| 2 | Escribir email o nombre | Campo lleno |✓ | |
| 3 | Esperar filtrado | Tabla filtra |✓ | |
| 4 | Verificar resultados | Solo coincidentes | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 9.3 Filtrar por Rol
**Descripción:** Filtro por rol funciona correctamente

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en selector "Todos los roles" | Dropdown abre |✓ | |
| 2 | Seleccionar "ADMINISTRADOR" | Usuarios admin solo |✓ | |
| 3 | Seleccionar "ALMACENISTA" | Usuarios almacenistas solo | ✓| |
| 4 | Seleccionar "AUXILIAR" | Usuarios auxiliares solo |✓ | |
| 5 | Volver a "Todos" | Todos los usuarios | ✓| |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 9.4 Editar Usuario
**Descripción:** Editar información del usuario

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click icono lápiz | Modal abre | ✓| |
| 2 | Verificar nombre | Nombre visible |✓ | |
| 3 | Verificar selector rol | Dropdown con roles |✓ | |
| 4 | Cambiar rol | Nueva opción se marca |✓ | |
| 5 | Click "Guardar" | Modal cierra | ✓| |
| 6 | Verificar alert | "Actualizado" |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 9.5 Cambio de Rol
**Descripción:** Rol del usuario se actualiza correctamente

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Editar usuario | Modal abre |✓ | |
| 2 | Seleccionar nuevo rol | Opción marcada | ✓| |
| 3 | Guardar | Se actualiza |✓ | |
| 4 | Verificar tabla | Nuevo rol visible |✓ | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

## 10. ROL ALMACENISTA

> **Credenciales de prueba**: usar cuenta con rol `ALMACENISTA`

### 10.1 Login y Dashboard Almacenista
**Descripción:** Almacenista puede iniciar sesión y ver su dashboard

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Login con cuenta Almacenista | Redirección a dashboard |✓  | |
| 2 | Verificar layout | Header con nombre/rol visible | ✓ | |
| 3 | Verificar submódulos | "Consulta y Captura", "Impresión", "Conteo" | ✓ | |
| 4 | Verificar submódulo activo | "Consulta y Captura" por defecto | ✓ | |
| 5 | Verificar botón logout | Ícono de salida visible |✓  | |

**Resultado Final:** ✓ PASÓ ☐ FALLÓ

---

### 10.2 Consulta y Captura - Almacenista
**Descripción:** Almacenista puede ver y capturar marbetes

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Estar en "Consulta y Captura" | Tabla de marbetes carga | ✓ | |
| 2 | Verificar selector período | Dropdown con períodos disponibles |✓  | |
| 3 | Verificar selector almacén | Dropdown con almacenes asignados | ✓ | |
| 4 | Seleccionar período y almacén | Tabla se actualiza |  ✓| |
| 5 | Verificar columnas tabla | Producto, Folios, Estado, etc. | ✓ | |
| 6 | Verificar buscador | Input de búsqueda funciona | ✓ | |
| 7 | Escribir en búsqueda | Tabla filtra produtos | ✓ | |

**Resultado Final:** ✓  PASÓ ☐ FALLÓ

---

### 10.3 Impresión de Marbetes - Almacenista
**Descripción:** Almacenista puede imprimir marbetes generados

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en "Impresión" | Submódulo carga | ✓  | |
| 2 | Seleccionar período | Dropdown funciona |✓   | |
| 3 | Seleccionar almacén | Dropdown funciona |  ✓ | |
| 4 | Verificar tabla | Marbetes en estado GENERADO visibles | ✓  | |
| 5 | Seleccionar marbetes | Checkboxes funcionales | ✓  | |
| 6 | Click "Imprimir" | Modal de confirmación aparece | ✓  | |
| 7 | Confirmar impresión | PDF se descarga / toast éxito | ✓  | |

**Resultado Final:** ✓  PASÓ ☐ FALLÓ

---

### 10.4 Conteo de Marbetes - Almacenista
**Descripción:** Almacenista puede registrar conteos

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en "Conteo" | Submódulo carga | ✓  | |
| 2 | Seleccionar período y almacén | Tabla carga | ✓  | |
| 3 | Verificar marbetes | Marbetes con estado IMPRESO visibles |✓   | |
| 4 | Ingresar cantidad en conteo | Campo acepta número |✓   | |
| 5 | Click en guardar/confirmar | Conteo registrado |  ✓ | |
| 6 | Verificar toast | Mensaje de éxito | ✓  | |
| 7 | Verificar estado | Estado cambia a CONTEO_1 o similar | ✓  | |

**Resultado Final:** ✓  PASÓ ☐ FALLÓ

---

### 10.5 Cancelación de Marbetes - Almacenista
**Descripción:** Almacenista puede cancelar marbetes

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Navegar a Cancelación (si visible) | Submódulo carga |  | |
| 2 | Seleccionar período y almacén | Datos cargan |  | |
| 3 | Seleccionar marbete a cancelar | Checkbox marcado |  | |
| 4 | Click "Cancelar" | Modal confirmación aparece |  | |
| 5 | Confirmar cancelación | Toast éxito |  | |
| 6 | Verificar tabla | Estado cambia a CANCELADO |  | |

**Resultado Final:** ✓  PASÓ ☐ FALLÓ

---

### 10.6 Logout Almacenista
**Descripción:** Cierre de sesión funciona correctamente

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en botón logout | Modal confirmación |✓   | |
| 2 | Confirmar logout | Token se limpia |✓   | |
| 3 | Verificar redirección | Va a `/login` |✓   | |
| 4 | Verificar localStorage | Token vacío |  ✓ | |

**Resultado Final:** ✓  PASÓ ☐ FALLÓ

---

## 11. ROL AUXILIAR

> **Credenciales de prueba**: usar cuenta con rol `AUXILIAR`

### 11.1 Login y Dashboard Auxiliar
**Descripción:** Auxiliar puede iniciar sesión y ver su dashboard

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Login con cuenta Auxiliar | Redirección a dashboard |✓   | |
| 2 | Verificar layout | Header con nombre/rol visible |✓   | |
| 3 | Verificar submódulos | "Consulta y Captura", "Impresión", "Conteo" |✓   | |
| 4 | Verificar submódulo activo | "Consulta y Captura" por defecto | ✓  | |
| 5 | Verificar no accede a Admin | URL admin redirige a su dashboard | ✓  | |

**Resultado Final:** ✓  PASÓ ☐ FALLÓ

---

### 11.2 Consulta y Captura - Auxiliar
**Descripción:** Auxiliar puede consultar y capturar conteos

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Estar en "Consulta y Captura" | Tabla de marbetes carga |  | |
| 2 | Verificar período auto-cargado | Período desde store global |  | |
| 3 | Verificar tabla | Productos con folios visibles |  | |
| 4 | Verificar campo cantidad | Input de conteo por fila |  | |
| 5 | Ingresar cantidad | Campo acepta número |  | |
| 6 | Click "Generar" / guardar | Toast confirmación |  | |
| 7 | Verificar buscador | Filtra productos correctamente |  | |

**Resultado Final:** ☐ PASÓ ☐ FALLÓ

---

### 11.3 Impresión - Auxiliar
**Descripción:** Auxiliar puede ver marbetes para imprimir

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en "Impresión" | Submódulo carga |  | |
| 2 | Verificar tabla | Marbetes generados visibles |  | |
| 3 | Verificar permisos | Solo puede ver, no generar |  | |
| 4 | Verificar datos | Folios, productos, estados correctos |  | |

**Resultado Final:** ☐ PASÓ ☐ FALLÓ

---

### 11.4 Conteo - Auxiliar
**Descripción:** Auxiliar puede registrar conteos de marbetes

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en "Conteo" | Submódulo carga |  | |
| 2 | Verificar tabla | Marbetes listos para contar |  | |
| 3 | Ingresar cantidad contada | Campo numérico acepta valor |  | |
| 4 | Guardar conteo | Toast éxito |  | |
| 5 | Verificar actualización | Fila refleja nuevo estado |  | |

**Resultado Final:** ☐ PASÓ ☐ FALLÓ

---

### 11.5 Logout Auxiliar
**Descripción:** Cierre de sesión funciona correctamente

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en botón logout | Modal confirmación |  | |
| 2 | Confirmar logout | Token se limpia |  | |
| 3 | Verificar redirección | Va a `/login` |  | |

**Resultado Final:** ☐ PASÓ ☐ FALLÓ

---

## 12. ROL AUXILIAR DE CONTEO

> **Credenciales de prueba**: usar cuenta con rol `AUXILIAR_DE_CONTEO`

### 12.1 Login y Dashboard Auxiliar de Conteo
**Descripción:** Auxiliar de Conteo puede iniciar sesión con acceso restringido

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Login con cuenta Auxiliar de Conteo | Redirección a dashboard |  | |
| 2 | Verificar layout | Header con nombre/rol visible |  | |
| 3 | Verificar submódulos | SOLO "Conteo" visible |  | |
| 4 | Verificar restricción | No ve Consulta ni Impresión |  | |
| 5 | Verificar no accede a Admin | URL admin redirige a su dashboard |  | |

**Resultado Final:** ☐ PASÓ ☐ FALLÓ

---

### 12.2 Conteo de Marbetes - Auxiliar de Conteo
**Descripción:** Auxiliar de Conteo registra conteos de verificación

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Estar en "Conteo" (único submódulo) | Tabla carga automáticamente |  | |
| 2 | Verificar período | Período cargado desde store |  | |
| 3 | Verificar tabla | Marbetes en estado IMPRESO visibles |  | |
| 4 | Verificar columnas | Folio, Producto, Almacén, Conteo |  | |
| 5 | Ingresar cantidad contada | Campo acepta número |  | |
| 6 | Guardar conteo | Toast de éxito |  | |
| 7 | Verificar estado actualizado | Fila refleja estado nuevo |  | |
| 8 | Buscar por folio/producto | Filtro funciona |  | |

**Resultado Final:** ☐ PASÓ ☐ FALLÓ

---

### 12.3 Restricción de Acceso - Auxiliar de Conteo
**Descripción:** Rol no puede acceder a funciones no permitidas

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Intentar navegar a `/admin` | Redirige a su dashboard |  | |
| 2 | Intentar navegar a ruta almacenista | Redirige o error |  | |
| 3 | Intentar navegar a ruta auxiliar | Redirige o error |  | |
| 4 | Verificar solo ve Conteo | Sin botones extra en layout |  | |

**Resultado Final:** ☐ PASÓ ☐ FALLÓ

---

### 12.4 Logout Auxiliar de Conteo
**Descripción:** Cierre de sesión funciona correctamente

| Paso | Acción | Resultado Esperado | ✓/✗ | Notas |
|------|--------|-------------------|-----|-------|
| 1 | Click en botón logout | Modal confirmación |  | |
| 2 | Confirmar logout | Token se limpia |  | |
| 3 | Verificar redirección | Va a `/login` |  | |

**Resultado Final:** ☐ PASÓ ☐ FALLÓ

---

## RESUMEN DE PRUEBAS

| Módulo | Total Tests | Pasados | Fallidos | % |
|--------|------------|---------|----------|---|
| Autenticación | 4 | 4       | | |
| Dashboard Admin | 3 |         | | |
| Períodos | 4 |         | | |
| Inventario | 3 |         | | |
| Almacenes | 4 |         | | |
| MultiAlmacén | 2 |         | | |
| Marbetes Admin | 2 |         | | |
| Reportes | 3 |         | | |
| Usuarios | 5 |         | | |
| **Almacenista** | **6** |         | | |
| **Auxiliar** | **5** |         | | |
| **Auxiliar de Conteo** | **4** |         | | |
| **TOTAL** | **45** |         | | |

**Firma del Probador:** _______________

**Fecha Conclusión:** _______________

**Observaciones Generales:**
```
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________
```

**Problemas Encontrados:**
```
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________
```

**Recomendaciones:**
```
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________
```
