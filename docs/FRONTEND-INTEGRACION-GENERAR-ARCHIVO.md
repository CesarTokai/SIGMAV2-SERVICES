# Guía de Integración Frontend - Generar Archivo TXT

**Módulo:** Generar Archivo de Existencias  
**API:** `/api/sigmav2/labels/generate-file`  
**Fecha:** 16 de enero de 2026

---

## 🎯 Objetivo

Documentar la integración frontend para el módulo "Generar Archivo" que permite descargar un archivo TXT con el inventario físico de productos.

---

## 📡 Endpoint API

### Información Básica

```
POST /api/sigmav2/labels/generate-file
```

**Requiere autenticación:** ✅ Sí (Bearer Token)

**Roles permitidos:**
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

---

## 🔧 Implementación Frontend

### 1. Interfaz de Usuario

La interfaz debe contener:

```tsx
import React, { useState, useEffect } from 'react';

interface Period {
  id: number;
  name: string;
  date: string;
}

const GenerateFileModule: React.FC = () => {
  const [periods, setPeriods] = useState<Period[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string>('');

  // Cargar periodos disponibles
  useEffect(() => {
    fetchPeriods();
  }, []);

  const fetchPeriods = async () => {
    try {
      const response = await fetch('/api/sigmav2/periods', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      const data = await response.json();
      setPeriods(data);
      
      // Seleccionar el último periodo por defecto
      if (data.length > 0) {
        setSelectedPeriod(data[data.length - 1].id);
      }
    } catch (error) {
      console.error('Error cargando periodos:', error);
    }
  };

  const handleGenerateFile = async () => {
    if (!selectedPeriod) {
      alert('Por favor seleccione un periodo');
      return;
    }

    setLoading(true);
    setMessage('');

    try {
      const response = await fetch('/api/sigmav2/labels/generate-file', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          periodId: selectedPeriod
        })
      });

      if (!response.ok) {
        throw new Error('Error al generar archivo');
      }

      const data = await response.json();
      
      // Mostrar mensaje de éxito
      setMessage(`✅ ${data.mensaje}\n\nArchivo: ${data.fileName}\nUbicación: ${data.filePath}\nTotal productos: ${data.totalProductos}`);
      
      // Opcional: Mostrar modal de éxito
      showSuccessModal(data);
      
    } catch (error) {
      console.error('Error:', error);
      setMessage('❌ Error al generar el archivo. Intente nuevamente.');
    } finally {
      setLoading(false);
    }
  };

  const showSuccessModal = (data: any) => {
    // Implementar modal personalizado
    alert(`El archivo se generó/actualizó correctamente.\n\nArchivo: ${data.fileName}\nUbicación: ${data.filePath}`);
  };

  return (
    <div className="generate-file-container">
      <h2>Generar Archivo</h2>
      
      <div className="form-group">
        <label htmlFor="period">Periodo:</label>
        <select 
          id="period"
          value={selectedPeriod || ''}
          onChange={(e) => setSelectedPeriod(Number(e.target.value))}
          disabled={loading}
        >
          <option value="">Seleccione un periodo</option>
          {periods.map(period => (
            <option key={period.id} value={period.id}>
              {period.name}
            </option>
          ))}
        </select>
      </div>

      <button 
        onClick={handleGenerateFile} 
        disabled={loading || !selectedPeriod}
        className="btn-primary"
      >
        {loading ? 'Generando...' : 'Generar Archivo'}
      </button>

      {message && (
        <div className={`message ${message.includes('✅') ? 'success' : 'error'}`}>
          {message}
        </div>
      )}

      {loading && (
        <div className="loading-overlay">
          <div className="spinner"></div>
          <p>Espere a que se genere el archivo TXT...</p>
        </div>
      )}
    </div>
  );
};

export default GenerateFileModule;
```

---

## 🎨 Estilos CSS

```css
.generate-file-container {
  max-width: 600px;
  margin: 20px auto;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.generate-file-container h2 {
  margin-bottom: 20px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #555;
}

.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.btn-primary {
  width: 100%;
  padding: 12px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-primary:hover:not(:disabled) {
  background: #0056b3;
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.message {
  margin-top: 20px;
  padding: 12px;
  border-radius: 4px;
  white-space: pre-line;
}

.message.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.message.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-overlay .spinner {
  width: 50px;
  height: 50px;
  border: 5px solid #f3f3f3;
  border-top: 5px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-overlay p {
  margin-top: 20px;
  color: white;
  font-size: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
```

---

## 📋 Request y Response

### Request Example

```javascript
const requestBody = {
  periodId: 16
};

fetch('/api/sigmav2/labels/generate-file', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
  },
  body: JSON.stringify(requestBody)
})
```

### Response Success (200 OK)

```json
{
  "fileName": "Existencias_Diciembre2016.txt",
  "filePath": "C:\\Sistemas\\SIGMA\\Documentos\\Existencias_Diciembre2016.txt",
  "totalProductos": 150,
  "mensaje": "Archivo generado exitosamente"
}
```

### Response Error (404 Not Found)

```json
{
  "error": "Periodo no encontrado",
  "status": 404,
  "timestamp": "2026-01-16T10:30:00"
}
```

### Response Error (500 Internal Server Error)

```json
{
  "error": "Error al generar archivo: No se pudo crear el directorio",
  "status": 500,
  "timestamp": "2026-01-16T10:30:00"
}
```

---

## 🔒 Manejo de Errores

```javascript
const handleGenerateFile = async () => {
  try {
    const response = await fetch('/api/sigmav2/labels/generate-file', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ periodId: selectedPeriod })
    });

    // Verificar código de respuesta
    if (response.status === 401) {
      // Token expirado o inválido
      redirectToLogin();
      return;
    }

    if (response.status === 403) {
      // Sin permisos
      showError('No tiene permisos para generar archivos');
      return;
    }

    if (response.status === 404) {
      // Periodo no encontrado
      showError('El periodo seleccionado no existe');
      return;
    }

    if (!response.ok) {
      // Error genérico del servidor
      const errorData = await response.json();
      showError(errorData.error || 'Error al generar archivo');
      return;
    }

    // Éxito
    const data = await response.json();
    showSuccess(data);

  } catch (error) {
    // Error de red o conexión
    console.error('Error de red:', error);
    showError('Error de conexión. Verifique su conexión a internet.');
  }
};
```

---

## 📱 Versión Angular

```typescript
import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

interface GenerateFileRequest {
  periodId: number;
}

interface GenerateFileResponse {
  fileName: string;
  filePath: string;
  totalProductos: number;
  mensaje: string;
}

@Component({
  selector: 'app-generate-file',
  templateUrl: './generate-file.component.html',
  styleUrls: ['./generate-file.component.css']
})
export class GenerateFileComponent implements OnInit {
  periods: any[] = [];
  selectedPeriod: number | null = null;
  loading = false;
  message = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadPeriods();
  }

  loadPeriods(): void {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });

    this.http.get<any[]>('/api/sigmav2/periods', { headers })
      .subscribe({
        next: (data) => {
          this.periods = data;
          if (data.length > 0) {
            this.selectedPeriod = data[data.length - 1].id;
          }
        },
        error: (error) => {
          console.error('Error cargando periodos:', error);
        }
      });
  }

  generateFile(): void {
    if (!this.selectedPeriod) {
      alert('Por favor seleccione un periodo');
      return;
    }

    this.loading = true;
    this.message = '';

    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });

    const body: GenerateFileRequest = {
      periodId: this.selectedPeriod
    };

    this.http.post<GenerateFileResponse>('/api/sigmav2/labels/generate-file', body, { headers })
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.message = `✅ ${response.mensaje}\n\nArchivo: ${response.fileName}\nUbicación: ${response.filePath}\nTotal productos: ${response.totalProductos}`;
          this.showSuccessModal(response);
        },
        error: (error) => {
          this.loading = false;
          this.message = '❌ Error al generar el archivo. Intente nuevamente.';
          console.error('Error:', error);
        }
      });
  }

  showSuccessModal(data: GenerateFileResponse): void {
    alert(`El archivo se generó/actualizó correctamente.\n\nArchivo: ${data.fileName}\nUbicación: ${data.filePath}`);
  }
}
```

---

## 🧪 Testing

### Ejemplo de Test con Jest

```typescript
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import GenerateFileModule from './GenerateFileModule';

describe('GenerateFileModule', () => {
  beforeEach(() => {
    // Mock localStorage
    Storage.prototype.getItem = jest.fn(() => 'fake-token');
  });

  test('debe renderizar el componente', () => {
    render(<GenerateFileModule />);
    expect(screen.getByText('Generar Archivo')).toBeInTheDocument();
  });

  test('debe cargar periodos al iniciar', async () => {
    global.fetch = jest.fn(() =>
      Promise.resolve({
        json: () => Promise.resolve([
          { id: 1, name: 'Enero 2026' },
          { id: 2, name: 'Febrero 2026' }
        ])
      })
    ) as jest.Mock;

    render(<GenerateFileModule />);

    await waitFor(() => {
      expect(screen.getByText('Enero 2026')).toBeInTheDocument();
    });
  });

  test('debe generar archivo exitosamente', async () => {
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          fileName: 'Existencias_Enero2026.txt',
          filePath: 'C:\\Sistemas\\SIGMA\\Documentos\\Existencias_Enero2026.txt',
          totalProductos: 100,
          mensaje: 'Archivo generado exitosamente'
        })
      })
    ) as jest.Mock;

    render(<GenerateFileModule />);
    
    const button = screen.getByText('Generar Archivo');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(/✅/)).toBeInTheDocument();
    });
  });
});
```

---

## 📝 Notas de Implementación

### ✅ Validaciones Importantes

1. **Verificar token:** Siempre incluir el token de autenticación en los headers
2. **Validar periodo:** Asegurarse de que un periodo esté seleccionado antes de enviar
3. **Manejo de estados:** Mostrar estados de carga durante el proceso
4. **Feedback visual:** Informar al usuario del éxito o error de la operación
5. **Timeout:** Considerar un timeout para la petición (recomendado: 30 segundos)

### ⚠️ Consideraciones

- El archivo se genera en el **servidor**, no se descarga al navegador
- La ubicación del archivo es **fija**: `C:\Sistemas\SIGMA\Documentos\`
- Si el archivo ya existe, será **sobrescrito**
- El tiempo de generación depende del número de productos en el periodo

---

## 🔗 Enlaces Relacionados

- [Documentación Técnica](./DOCUMENTACION-GENERAR-ARCHIVO-TXT.md)
- [Manual de Usuario](./MANUAL-USUARIO-GENERAR-ARCHIVO.md)
- [API de Periodos](./API-PERIODOS.md)

---

**© 2026 Tokai - Sistema SIGMA V2**
