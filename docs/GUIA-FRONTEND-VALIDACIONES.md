# 🎨 Guía de Integración Frontend - Validaciones Actualizadas

## 📋 Resumen

Este documento describe los cambios en las validaciones del backend y cómo el frontend debe adaptarse para una mejor experiencia de usuario.

---

## ⚠️ BREAKING CHANGES

### 1. Conteos No Pueden Ser Cero o Negativos

**APIs Afectadas**:
- `POST /api/sigmav2/labels/counts/c1`
- `POST /api/sigmav2/labels/counts/c2`
- `PUT /api/sigmav2/labels/counts/c1`
- `PUT /api/sigmav2/labels/counts/c2`

**Antes**: Se aceptaban valores como `0`, `-1`, etc.  
**Ahora**: Solo se aceptan valores `> 0`

**Acción Requerida**: Agregar validación en el formulario de conteos.

---

## 🔧 Validaciones Recomendadas para el Frontend

### ✅ Validación de Conteos (C1 y C2)

#### **Formulario de Registro de Conteo**

```typescript
// Validación con Yup (React)
const conteoSchema = yup.object({
  folio: yup.number()
    .required('El folio es obligatorio')
    .positive('El folio debe ser positivo')
    .integer('El folio debe ser un número entero'),
  
  countedValue: yup.number()
    .required('El valor del conteo es obligatorio')
    .positive('El valor del conteo debe ser mayor a cero')
    .test('has-decimals', 'Máximo 2 decimales', (value) => {
      if (!value) return true;
      return /^\d+(\.\d{1,2})?$/.test(value.toString());
    }),
});
```

#### **Validación HTML5 (Simple)**

```html
<form>
  <label for="folio">Folio:</label>
  <input 
    type="number" 
    id="folio" 
    name="folio" 
    required 
    min="1"
    step="1"
  />

  <label for="conteo">Valor Conteo:</label>
  <input 
    type="number" 
    id="conteo" 
    name="countedValue" 
    required 
    min="0.01"
    step="0.01"
    placeholder="Ejemplo: 10.50"
  />

  <button type="submit">Registrar Conteo</button>
</form>
```

#### **Mensajes de Error Sugeridos**

```typescript
const mensajesError = {
  valorCero: '⚠️ El conteo debe ser mayor a cero. Si no hay producto, cancele el marbete.',
  valorNegativo: '❌ El valor del conteo no puede ser negativo.',
  campoVacio: '⚠️ Debe ingresar un valor para el conteo.',
  decimales: '⚠️ Máximo 2 decimales permitidos (Ejemplo: 10.50)',
};
```

---

### ✅ Validación de Observaciones

**Límite**: 500 caracteres

```typescript
// Validación con Yup
const observacionesSchema = yup.string()
  .max(500, 'Las observaciones no pueden exceder 500 caracteres')
  .nullable();
```

```html
<textarea 
  name="observaciones" 
  maxlength="500" 
  placeholder="Observaciones (opcional)"
  rows="3"
></textarea>
<small>Caracteres restantes: <span id="char-count">500</span></small>
```

```javascript
// Contador de caracteres
const textarea = document.querySelector('[name="observaciones"]');
const charCount = document.getElementById('char-count');

textarea.addEventListener('input', (e) => {
  const remaining = 500 - e.target.value.length;
  charCount.textContent = remaining;
  charCount.style.color = remaining < 50 ? 'red' : 'inherit';
});
```

---

### ✅ Validación de Motivo de Cancelación

**Límite**: 500 caracteres

```typescript
const cancelacionSchema = yup.object({
  folio: yup.number().required('El folio es obligatorio'),
  periodId: yup.number().required('El periodo es obligatorio'),
  warehouseId: yup.number().required('El almacén es obligatorio'),
  motivoCancelacion: yup.string()
    .max(500, 'El motivo no puede exceder 500 caracteres')
    .nullable(),
});
```

---

## 🎯 Manejo de Errores del Backend

### Estructura de Respuesta de Error

```typescript
interface ErrorResponse {
  success: false;
  error: string;      // Tipo de error (ej: "Estado inválido")
  message: string;    // Mensaje descriptivo
}
```

### Ejemplo de Respuesta de Error

```json
{
  "success": false,
  "error": "Estado inválido",
  "message": "El valor del conteo debe ser mayor a cero"
}
```

### Manejo en el Frontend

```typescript
// React + Axios
const registrarConteo = async (data: ConteoDTO) => {
  try {
    const response = await axios.post('/api/sigmav2/labels/counts/c1', data);
    toast.success('✅ Conteo registrado exitosamente');
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const errorData = error.response.data as ErrorResponse;
      
      // Mostrar mensaje específico del backend
      toast.error(`${errorData.error}: ${errorData.message}`);
      
      // O mapear a mensajes personalizados
      switch (error.response.status) {
        case 400:
          toast.error('❌ ' + errorData.message);
          break;
        case 403:
          toast.error('🚫 No tiene permisos para esta acción');
          break;
        case 404:
          toast.error('❓ Marbete no encontrado');
          break;
        case 409:
          toast.error('⚠️ El conteo ya fue registrado');
          break;
        default:
          toast.error('❌ Error al registrar el conteo');
      }
    } else {
      toast.error('❌ Error de conexión con el servidor');
    }
    throw error;
  }
};
```

---

## 🎨 Componentes de UI Recomendados

### Componente de Input para Conteos

```tsx
// React + TypeScript
interface ConteoInputProps {
  value: number | '';
  onChange: (value: number | '') => void;
  label: string;
  disabled?: boolean;
}

const ConteoInput: React.FC<ConteoInputProps> = ({ 
  value, 
  onChange, 
  label,
  disabled = false 
}) => {
  const [error, setError] = useState<string>('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    
    if (val === '') {
      onChange('');
      setError('');
      return;
    }

    const num = parseFloat(val);
    
    if (isNaN(num)) {
      setError('Debe ingresar un número válido');
      return;
    }

    if (num <= 0) {
      setError('El valor debe ser mayor a cero');
      onChange(num);
      return;
    }

    // Validar decimales
    const decimals = val.split('.')[1];
    if (decimals && decimals.length > 2) {
      setError('Máximo 2 decimales');
      return;
    }

    setError('');
    onChange(num);
  };

  return (
    <div className="form-group">
      <label htmlFor="conteo-input">{label}</label>
      <input
        id="conteo-input"
        type="number"
        className={`form-control ${error ? 'is-invalid' : ''}`}
        value={value}
        onChange={handleChange}
        min="0.01"
        step="0.01"
        disabled={disabled}
        placeholder="Ej: 10.50"
      />
      {error && <div className="invalid-feedback">{error}</div>}
    </div>
  );
};
```

---

## 📱 Consideraciones de UX

### 1. **Deshabilitar botón de envío si hay errores**

```tsx
const [formErrors, setFormErrors] = useState<Record<string, string>>({});

const isFormValid = () => {
  return Object.keys(formErrors).length === 0 && 
         countedValue > 0 &&
         folio > 0;
};

<button 
  type="submit" 
  disabled={!isFormValid()}
  className="btn btn-primary"
>
  Registrar Conteo
</button>
```

### 2. **Feedback visual inmediato**

```css
/* CSS para inputs inválidos */
.form-control.is-invalid {
  border-color: #dc3545;
  box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.25);
}

.form-control.is-valid {
  border-color: #28a745;
  box-shadow: 0 0 0 0.2rem rgba(40, 167, 69, 0.25);
}
```

### 3. **Mensaje de ayuda contextual**

```html
<div class="form-text">
  💡 Si el producto no se encuentra, use el botón "Cancelar Marbete" en lugar de registrar 0.
</div>
```

---

## 🧪 Casos de Prueba para el Frontend

### Test 1: Validación de Valor Cero
```typescript
describe('ConteoInput', () => {
  it('debe mostrar error si el valor es cero', () => {
    const { getByLabelText, getByText } = render(
      <ConteoInput value={0} onChange={jest.fn()} label="Conteo" />
    );
    
    const input = getByLabelText('Conteo') as HTMLInputElement;
    fireEvent.change(input, { target: { value: '0' } });
    
    expect(getByText('El valor debe ser mayor a cero')).toBeInTheDocument();
  });
});
```

### Test 2: Validación de Decimales
```typescript
it('debe rechazar más de 2 decimales', () => {
  const { getByLabelText, getByText } = render(
    <ConteoInput value={''} onChange={jest.fn()} label="Conteo" />
  );
  
  const input = getByLabelText('Conteo') as HTMLInputElement;
  fireEvent.change(input, { target: { value: '10.123' } });
  
  expect(getByText('Máximo 2 decimales')).toBeInTheDocument();
});
```

### Test 3: Manejo de Error del Backend
```typescript
it('debe mostrar error del backend al fallar el registro', async () => {
  mockAxios.post.mockRejectedValue({
    response: {
      status: 400,
      data: {
        success: false,
        error: 'Validación fallida',
        message: 'El valor del conteo debe ser mayor a cero'
      }
    }
  });

  const { getByRole } = render(<FormularioConteo />);
  
  fireEvent.click(getByRole('button', { name: 'Registrar' }));
  
  await waitFor(() => {
    expect(toast.error).toHaveBeenCalledWith(
      expect.stringContaining('El valor del conteo debe ser mayor a cero')
    );
  });
});
```

---

## 📊 Checklist de Implementación

### Frontend - Validaciones
- [ ] Input de conteo con validación de valor > 0
- [ ] Input de conteo con validación de máximo 2 decimales
- [ ] Textarea de observaciones con límite de 500 caracteres
- [ ] Textarea de motivo cancelación con límite de 500 caracteres
- [ ] Contador de caracteres restantes
- [ ] Deshabilitar botón submit si hay errores

### Frontend - Manejo de Errores
- [ ] Interceptor de Axios para errores globales
- [ ] Toast/Snackbar para mostrar errores
- [ ] Mapeo de códigos HTTP a mensajes amigables
- [ ] Timeout de 30 segundos en peticiones

### Frontend - UX
- [ ] Feedback visual en inputs (válido/inválido)
- [ ] Mensajes de ayuda contextuales
- [ ] Loading states durante peticiones
- [ ] Confirmación antes de enviar (opcional)

### Testing
- [ ] Tests unitarios de componentes de input
- [ ] Tests de integración de formularios
- [ ] Tests de manejo de errores del backend
- [ ] Tests E2E del flujo completo

---

## 🔗 Recursos Adicionales

### Librerías Recomendadas

**Validación de Formularios**:
- [Yup](https://github.com/jquense/yup) - Schema validation
- [React Hook Form](https://react-hook-form.com/) - Form management
- [Formik](https://formik.org/) - Form library

**Notificaciones**:
- [React Toastify](https://fkhadra.github.io/react-toastify/) - Toast notifications
- [React Hot Toast](https://react-hot-toast.com/) - Lightweight toasts
- [Notistack](https://notistack.com/) - Snackbar notifications

**HTTP Client**:
- [Axios](https://axios-http.com/) - Promise based HTTP client
- [React Query](https://tanstack.com/query) - Data fetching & caching

---

## 📝 Ejemplo Completo - Formulario de Conteo

```tsx
import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { toast } from 'react-toastify';
import axios from 'axios';

// Schema de validación
const schema = yup.object({
  folio: yup.number()
    .required('El folio es obligatorio')
    .positive('El folio debe ser positivo')
    .integer('El folio debe ser un número entero'),
  countedValue: yup.number()
    .required('El valor del conteo es obligatorio')
    .positive('El valor debe ser mayor a cero')
    .test('decimals', 'Máximo 2 decimales', (value) => {
      if (!value) return true;
      return /^\d+(\.\d{1,2})?$/.test(value.toString());
    }),
  observaciones: yup.string()
    .max(500, 'Las observaciones no pueden exceder 500 caracteres')
    .nullable(),
});

interface FormData {
  folio: number;
  countedValue: number;
  observaciones?: string;
}

const FormularioConteoC1: React.FC = () => {
  const [loading, setLoading] = useState(false);
  
  const { 
    register, 
    handleSubmit, 
    formState: { errors, isValid },
    reset,
    watch
  } = useForm<FormData>({
    resolver: yupResolver(schema),
    mode: 'onChange',
  });

  const observaciones = watch('observaciones', '');
  const charsRemaining = 500 - (observaciones?.length || 0);

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    
    try {
      await axios.post('/api/sigmav2/labels/counts/c1', data);
      toast.success('✅ Conteo C1 registrado exitosamente');
      reset();
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        const errorData = error.response.data;
        toast.error(`❌ ${errorData.message || 'Error al registrar el conteo'}`);
      } else {
        toast.error('❌ Error de conexión con el servidor');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="conteo-form">
      <div className="form-group">
        <label htmlFor="folio">Folio del Marbete</label>
        <input
          id="folio"
          type="number"
          className={`form-control ${errors.folio ? 'is-invalid' : ''}`}
          {...register('folio')}
          placeholder="Ej: 123"
          disabled={loading}
        />
        {errors.folio && (
          <div className="invalid-feedback">{errors.folio.message}</div>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="countedValue">Valor del Conteo</label>
        <input
          id="countedValue"
          type="number"
          step="0.01"
          className={`form-control ${errors.countedValue ? 'is-invalid' : ''}`}
          {...register('countedValue')}
          placeholder="Ej: 10.50"
          disabled={loading}
        />
        {errors.countedValue && (
          <div className="invalid-feedback">{errors.countedValue.message}</div>
        )}
        <small className="form-text text-muted">
          💡 Si el producto no se encuentra, cancele el marbete en lugar de registrar 0
        </small>
      </div>

      <div className="form-group">
        <label htmlFor="observaciones">Observaciones (Opcional)</label>
        <textarea
          id="observaciones"
          className={`form-control ${errors.observaciones ? 'is-invalid' : ''}`}
          {...register('observaciones')}
          rows={3}
          placeholder="Observaciones adicionales..."
          disabled={loading}
        />
        <small className={`form-text ${charsRemaining < 50 ? 'text-danger' : 'text-muted'}`}>
          Caracteres restantes: {charsRemaining}
        </small>
        {errors.observaciones && (
          <div className="invalid-feedback">{errors.observaciones.message}</div>
        )}
      </div>

      <button
        type="submit"
        className="btn btn-primary"
        disabled={!isValid || loading}
      >
        {loading ? (
          <>
            <span className="spinner-border spinner-border-sm mr-2" />
            Registrando...
          </>
        ) : (
          'Registrar Conteo C1'
        )}
      </button>
    </form>
  );
};

export default FormularioConteoC1;
```

---

## 📞 Contacto y Soporte

Si tienes dudas sobre la integración:
1. Revisa la documentación de la auditoría: `docs/AUDITORIA-APIS-VALIDACIONES.md`
2. Consulta los ejemplos en este documento
3. Contacta al equipo de backend para aclaraciones

---

**Última actualización**: 2026-01-22  
**Versión del documento**: 1.0
