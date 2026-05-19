# Ejemplo de petición correcta al endpoint /summary

## CURL (Copia y pega esto en tu terminal)
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/summary \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer REEMPLAZA_CON_TU_TOKEN_JWT" \
  -d '{"periodId":1,"warehouseId":2}'
```

## Postman - Configuración paso a paso
1. **Method**: POST
2. **URL**: `http://localhost:8080/api/sigmav2/labels/summary`
3. **Headers**:
   - `Content-Type`: `application/json`
   - `Authorization`: `Bearer TU_TOKEN_JWT`
4. **Body**:
   - Selecciona: **raw**
   - Tipo: **JSON** (importante, no Text)
   - Contenido:
   ```json
   {
     "periodId": 1,
     "warehouseId": 2
   }
   ```

## Thunder Client (VS Code) - Configuración
1. **Method**: POST
2. **URL**: `http://localhost:8080/api/sigmav2/labels/summary`
3. **Auth**: Bearer Token → Pega tu JWT
4. **Body**:
   - Selecciona: **JSON**
   - Contenido:
   ```json
   {
     "periodId": 1,
     "warehouseId": 2
   }
   ```

## JavaScript/Fetch (Frontend)
```javascript
const token = 'TU_TOKEN_JWT';

fetch('http://localhost:8080/api/sigmav2/labels/summary', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    periodId: 1,
    warehouseId: 2
  })
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

## Respuesta esperada (cuando esté implementado correctamente)
```json
[
  {
    "productId": 123,
    "claveProducto": "PROD-001",
    "nombreProducto": "Producto de ejemplo",
    "claveAlmacen": "ALM-01",
    "nombreAlmacen": "Almacén Central",
    "foliosSolicitados": 10,
    "foliosExistentes": 8,
    "estado": "GENERADO",
    "existencias": 150
  }
]
```

---

## ⚠️ ERROR COMÚN
Si ves este error:
```
Content-Type 'application/octet-stream' is not supported
```

**Causa**: Tu cliente está enviando el body como binario en lugar de JSON.

**Solución**:
- En Postman: Body → raw → **JSON** (dropdown)
- En Thunder Client: Body → **JSON** (no Text)
- En curl: Asegúrate de usar `-H "Content-Type: application/json"`

