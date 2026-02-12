# 🎯 Recomendaciones Frontend - Auxiliar de Conteo

## 📌 Contexto

El error "El marbete no pertenece al periodo/almacén especificado" ocurrió porque el frontend estaba enviando índices de tabla en lugar de folios reales. Se ha mejorado el backend para proporcionar mensajes más informativos, pero el frontend también debe validar.

---

## ✅ Implementación Recomendada

### 1. Obtener Folios Disponibles Antes

**Antes de que el usuario ingrese un folio**, mostrar la lista de folios disponibles:

```javascript
// 1. Cargar folios disponibles cuando se selecciona período/almacén
async function cargarFoliosDisponibles(periodId, warehouseId) {
    try {
        const response = await fetch('/api/sigmav2/labels/for-count/list', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                periodId: periodId,
                warehouseId: warehouseId
            })
        });
        
        if (response.ok) {
            const folios = await response.json();
            const foliosDisponibles = folios.map(f => f.folio);
            console.log('Folios disponibles:', foliosDisponibles);
            
            // Guardar en estado global o variable
            window.foliosDisponibles = foliosDisponibles;
            
            // Mostrar en UI
            mostrarFoliosDisponibles(foliosDisponibles);
        }
    } catch (error) {
        console.error('Error cargando folios:', error);
    }
}

// 2. Mostrar lista de folios disponibles
function mostrarFoliosDisponibles(folios) {
    const listaContainer = document.getElementById('folios-disponibles');
    
    if (folios.length === 0) {
        listaContainer.innerHTML = '<p>No hay marbetes disponibles para conteo</p>';
        return;
    }
    
    const html = `
        <div class="folios-panel">
            <h3>Folios Disponibles</h3>
            <div class="folios-grid">
                ${folios.map(folio => `
                    <button class="folio-btn" onclick="seleccionarFolio(${folio})">
                        ${folio}
                    </button>
                `).join('')}
            </div>
        </div>
    `;
    
    listaContainer.innerHTML = html;
}

// 3. Seleccionar folio desde lista
function seleccionarFolio(folio) {
    const inputFolio = document.getElementById('input-folio');
    inputFolio.value = folio;
    inputFolio.focus();
    
    // Opcionalmente, cargar datos automáticamente
    // cargarMarbete(folio);
}
```

### 2. Validar Folio Antes de Enviar

```javascript
// Función de validación previa
function validarFolioAntesDeSendar(folio, periodId, warehouseId) {
    // Convertir a número
    const folioNum = parseInt(folio);
    
    // Validar que sea un número
    if (isNaN(folioNum)) {
        mostrarError('El folio debe ser un número válido');
        return false;
    }
    
    // Validar que sea positivo
    if (folioNum <= 0) {
        mostrarError('El folio debe ser mayor a 0');
        return false;
    }
    
    // Validar que esté en lista de disponibles
    if (window.foliosDisponibles && !window.foliosDisponibles.includes(folioNum)) {
        const sugerencias = window.foliosDisponibles.slice(0, 5).join(', ');
        mostrarError(
            `Folio ${folioNum} no disponible. Sugerencias: ${sugerencias}`
        );
        return false;
    }
    
    return true;
}

// Usar en evento submit
document.getElementById('form-buscar-marbete').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const folio = document.getElementById('input-folio').value;
    const periodId = document.getElementById('select-periodo').value;
    const warehouseId = document.getElementById('select-almacen').value;
    
    // Validación previa
    if (!validarFolioAntesDeSendar(folio, periodId, warehouseId)) {
        return;
    }
    
    // Si pasa validación, enviar al servidor
    await consultarMarbete(folio, periodId, warehouseId);
});
```

### 3. Manejo Mejorado de Errores

```javascript
async function consultarMarbete(folio, periodId, warehouseId) {
    try {
        const response = await fetch('/api/sigmav2/labels/for-count', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                folio: parseInt(folio),
                periodId: parseInt(periodId),
                warehouseId: parseInt(warehouseId)
            })
        });
        
        if (!response.ok) {
            const errorData = await response.json();
            const mensajeError = errorData.message || 'Error desconocido';
            
            // Extraer folios disponibles del mensaje
            const foliosMatch = mensajeError.match(/Folios disponibles: ([^"]+)/);
            
            if (foliosMatch) {
                const foliosDisponibles = foliosMatch[1].split(', ');
                mostrarErrorConSugerencias(mensajeError, foliosDisponibles);
            } else {
                mostrarError(mensajeError);
            }
            
            return;
        }
        
        const marbete = await response.json();
        mostrarDetallesMarbete(marbete);
        
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error de conexión. Intente nuevamente.');
    }
}

function mostrarErrorConSugerencias(mensaje, foliosSugeridos) {
    const html = `
        <div class="error-container">
            <h3>⚠️ Error en la búsqueda</h3>
            <p>${mensaje}</p>
            
            <div class="sugerencias">
                <p><strong>Folios disponibles que puede usar:</strong></p>
                <div class="folio-buttons">
                    ${foliosSugeridos.map(folio => `
                        <button onclick="seleccionarFolio(${folio})" class="btn-sugerencia">
                            ${folio}
                        </button>
                    `).join('')}
                </div>
            </div>
        </div>
    `;
    
    document.getElementById('error-container').innerHTML = html;
}
```

---

## 🎨 Interfaz Mejorada

### Estructura HTML Recomendada

```html
<div class="conteo-container">
    <!-- Selección de período y almacén -->
    <div class="selector-area">
        <select id="select-periodo" onchange="actualizarFoliosDisponibles()">
            <option value="">Seleccionar Período</option>
            <!-- opciones -->
        </select>
        
        <select id="select-almacen" onchange="actualizarFoliosDisponibles()">
            <option value="">Seleccionar Almacén</option>
            <!-- opciones -->
        </select>
    </div>
    
    <!-- Panel de folios disponibles -->
    <div id="folios-disponibles" class="folios-panel">
        <!-- Se llenará dinámicamente -->
    </div>
    
    <!-- Formulario de búsqueda -->
    <form id="form-buscar-marbete" class="buscar-form">
        <div class="form-group">
            <label for="input-folio">Folio del Marbete</label>
            <input 
                type="number" 
                id="input-folio" 
                placeholder="Ingrese el folio"
                min="1"
                required
            >
            <small>Seleccione un folio de la lista superior o ingrese manualmente</small>
        </div>
        <button type="submit" class="btn-primary">Buscar Marbete</button>
    </form>
    
    <!-- Mostrar errores -->
    <div id="error-container" class="error-area"></div>
    
    <!-- Mostrar datos del marbete -->
    <div id="marbete-details" class="details-area"></div>
</div>
```

### Estilos CSS Recomendados

```css
.folios-panel {
    background-color: #f0f0f0;
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 20px;
}

.folios-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
    gap: 10px;
    margin-top: 10px;
}

.folio-btn {
    padding: 8px 12px;
    border: 1px solid #ddd;
    background-color: #fff;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;
    font-size: 14px;
}

.folio-btn:hover {
    background-color: #007bff;
    color: white;
    border-color: #007bff;
}

.error-container {
    background-color: #ffebee;
    border-left: 4px solid #f44336;
    padding: 15px;
    border-radius: 4px;
    margin-bottom: 20px;
}

.sugerencias {
    margin-top: 15px;
    padding-top: 15px;
    border-top: 1px solid #ffcdd2;
}

.folio-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 10px;
}

.btn-sugerencia {
    padding: 8px 12px;
    background-color: #ff9800;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
}

.btn-sugerencia:hover {
    background-color: #f57c00;
}
```

---

## 🔄 Flujo de Interacción Mejorado

```
1. Usuario selecciona Período
   ↓
2. Usuario selecciona Almacén
   ↓
3. Sistema carga folios disponibles (AJAX)
   ↓
4. Se muestra lista de folios con botones
   ↓
5. Usuario puede:
   a) Hacer clic en un folio
   b) Escribir manualmente
   ↓
6. Validación previa (es número, está en lista)
   ↓
7. Si es válido → Enviar al servidor
   Si es inválido → Mostrar sugerencias
   ↓
8. Mostrar detalles del marbete o error mejorado
```

---

## 📊 Beneficios

✅ **Experiencia mejorada**: El usuario ve qué folios están disponibles
✅ **Errores reducidos**: Validación previa evita muchos errores
✅ **Claridad**: Si ocurre error, el usuario sabe qué hacer
✅ **Productividad**: Menos intentos fallidos
✅ **Debugging**: Logs del backend más útiles

---

## 🔗 Integración con Backend

El backend ahora proporciona:

1. **Endpoint**: `POST /api/sigmav2/labels/for-count/list`
   - Lista folios disponibles en un período/almacén

2. **Mejores mensajes de error**
   - Incluye folios disponibles
   - Indica período/almacén actual vs solicitado

3. **Información en logs**
   - Facilita debugging de problemas

---

## 📝 Checklist de Implementación

- [ ] Implementar `cargarFoliosDisponibles()`
- [ ] Mostrar lista visual de folios
- [ ] Implementar `validarFolioAntesDeSendar()`
- [ ] Mejorar manejo de errores
- [ ] Extraer folios sugeridos del error
- [ ] Mostrar botones de sugerencias
- [ ] Prueba con diferentes períodos/almacenes
- [ ] Prueba con folios inválidos
- [ ] Prueba con períodos/almacenes sin marbetes


