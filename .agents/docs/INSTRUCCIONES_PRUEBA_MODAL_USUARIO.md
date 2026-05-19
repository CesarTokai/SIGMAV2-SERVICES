# 🧪 Instrucciones de Prueba - Modal de Usuario Mejorado

## 📋 Cambios Implementados

### 1. **Interceptor de Axios** ✅
- Ahora muestra mensajes específicos del backend
- Ejemplo: "Ya existe un usuario con el email: cgonzalez@tokai.com.mx"

### 2. **Visualización de Contraseñas** 👁️
- Botón con emoji para mostrar/ocultar contraseña
- Se sincroniza en ambos campos

### 3. **Bloqueo del Primer Input** 🔒
- Al hacer focus en "Confirmar contraseña", el primer input se bloquea
- Al salir del segundo campo, se desbloquea

### 4. **Validación Visual** ✅
- Borde verde cuando las contraseñas coinciden
- Mensaje de error cuando no coinciden

### 5. **Modal Optimizado** 🎨
- Tamaño reducido de 900px a 700px
- Sin iconos SVG de w3
- Mejor distribución de campos

### 6. **Botón "Sí, eliminar" Corregido** 🔴
- Ahora se ve con texto blanco sobre fondo rojo

---

## 🧪 Casos de Prueba

### **Prueba 1: Mensaje de Error del Backend**
1. Ir a Gestión de Usuarios
2. Click en "Nuevo Usuario"
3. Llenar el formulario con un email que ya exista
4. Click en "Guardar"
5. **Resultado esperado:** Toast con mensaje específico: "Ya existe un usuario con el email: xxx@xxx.com"

---

### **Prueba 2: Visualización de Contraseñas**
1. Abrir modal de nuevo usuario
2. Escribir contraseña en el primer campo (debería estar oculta: ••••)
3. Click en el botón 👁️ del primer campo
4. **Resultado esperado:** La contraseña se muestra en texto plano
5. Click nuevamente en 👁️
6. **Resultado esperado:** Vuelve a ocultarse

---

### **Prueba 3: Bloqueo del Primer Input**
1. Escribir "MiPassword123!" en el primer campo
2. Click en el segundo campo (Confirmar contraseña)
3. **Resultado esperado:** El primer campo queda bloqueado (readonly)
4. Intentar editar el primer campo
5. **Resultado esperado:** No se puede editar
6. Click fuera del segundo campo
7. **Resultado esperado:** El primer campo se desbloquea

---

### **Prueba 4: Validación Visual con Borde Verde**

#### Caso A: Contraseñas Coincidentes
1. Escribir "Password123!" en el primer campo
2. Escribir "Password123!" en el segundo campo
3. **Resultado esperado:**
   - Ambos inputs tienen borde verde
   - Mensaje: "✓ Las contraseñas coinciden"

#### Caso B: Contraseñas Diferentes
1. Escribir "Password123!" en el primer campo
2. Escribir "Password456!" en el segundo campo
3. **Resultado esperado:**
   - Inputs NO tienen borde verde
   - Mensaje: "Las contraseñas no coinciden"

---

### **Prueba 5: Tamaño del Modal**
1. Abrir modal de nuevo usuario
2. **Resultado esperado:**
   - Modal con ancho máximo de 700px
   - Campos bien distribuidos en 2 columnas
   - Campos de contraseña ocupan el ancho completo
   - Modal centrado en la pantalla

---

### **Prueba 6: Botón de Cerrar Modal**
1. Abrir modal de nuevo usuario
2. Observar el botón "✕" en la esquina superior derecha
3. **Resultado esperado:**
   - Botón visible con símbolo ✕
   - Fondo semitransparente blanco
   - Al hover, cambia ligeramente de color
4. Click en el botón
5. **Resultado esperado:** Modal se cierra

---

### **Prueba 7: Botón "Sí, eliminar"**
1. Ir a la lista de usuarios
2. Click en botón de eliminar (🗑️) de cualquier usuario
3. Observar el modal de confirmación de SweetAlert2
4. **Resultado esperado:**
   - Botón "Sí, eliminar" con fondo rojo (#dc2626)
   - Texto en blanco y negrita
   - Completamente visible y legible
5. Click en "Cancelar" (no eliminar en esta prueba)

---

### **Prueba 8: Contraseña Autogenerada**
1. Abrir modal de nuevo usuario
2. Llenar nombre, apellidos, email, rol
3. **NO** escribir contraseña
4. Click en "Guardar"
5. **Resultado esperado:**
   - Usuario creado exitosamente
   - Toast muestra: "Usuario creado - Contraseña: [contraseña_generada]"
   - La contraseña cumple con todos los requisitos

---

### **Prueba 9: Validación de Requisitos de Contraseña**

#### Caso A: Contraseña Válida
- Contraseña: "MyPass123!@"
- **Resultado:** Usuario se crea correctamente

#### Caso B: Contraseña Muy Corta
- Contraseña: "Pass1!"
- **Resultado:** Error: "La contraseña debe tener al menos 8 caracteres"

#### Caso C: Sin Mayúsculas
- Contraseña: "password123!"
- **Resultado:** Error: "La contraseña debe contener mayúsculas y minúsculas"

#### Caso D: Sin Caracteres Especiales
- Contraseña: "Password123"
- **Resultado:** Error: "La contraseña debe contener al menos un número y un carácter especial"

#### Caso E: Con Espacios
- Contraseña: "Pass word123!"
- **Resultado:** Error: "La contraseña no puede contener espacios"

---

### **Prueba 10: Responsive en Móvil**
1. Abrir DevTools (F12)
2. Cambiar a vista móvil (375px de ancho)
3. Abrir modal de nuevo usuario
4. **Resultado esperado:**
   - Modal ocupa 95% del ancho
   - Formulario cambia a 1 columna
   - Todos los campos visibles
   - Scroll funciona correctamente

---

## 🐛 Troubleshooting

### Problema: El botón "Sí, eliminar" sigue en blanco
**Solución:**
- Verificar que `buttonsStyling: true` esté en el Swal.fire
- Verificar que los estilos `:deep(.swal2-confirm)` estén en el componente
- Limpiar caché del navegador (Ctrl + Shift + Del)

### Problema: El primer input no se bloquea
**Solución:**
- Verificar que `passwordLocked` esté en el data()
- Verificar eventos `@focus` y `@blur` en el segundo input

### Problema: Borde verde no aparece
**Solución:**
- Verificar que `passwordMatch` sea `true`
- Verificar que clase `.input-success` tenga `!important`

---

## ✅ Checklist de Validación

- [ ] Mensaje de error específico del backend se muestra correctamente
- [ ] Botón 👁️ muestra/oculta contraseña
- [ ] Primer input se bloquea al hacer focus en el segundo
- [ ] Borde verde aparece cuando las contraseñas coinciden
- [ ] Mensaje "✓ Las contraseñas coinciden" se muestra
- [ ] Mensaje de error cuando no coinciden
- [ ] Modal tiene 700px de ancho máximo
- [ ] Botón ✕ del modal es visible
- [ ] Botón "Sí, eliminar" es completamente visible (texto blanco)
- [ ] Contraseña autogenerada funciona
- [ ] Validaciones de contraseña funcionan
- [ ] Modal es responsive en móvil

---

## 📞 Soporte

Si encuentras algún problema, verifica:
1. Consola del navegador (F12) para ver errores
2. Network tab para ver respuestas del backend
3. Vue DevTools para verificar el estado del componente

---

**Fecha de implementación:** 26 de Enero de 2026
**Componente:** `AdminUserManagement.vue`
**Configuración:** `axiosConfig.ts`
