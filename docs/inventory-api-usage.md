# SIGMAV2 – Guía para consumir las APIs de Inventario

Esta guía explica cómo usar los endpoints de Inventario disponibles bajo el prefijo:

Base URL: /api/sigmav2/inventory

Notas generales:
- Autenticación: el endpoint de importación requiere rol ADMINISTRADOR (Spring Security con @PreAuthorize). Si tu app usa JWT/Bearer, agrega: Authorization: Bearer <token>.
- Fechas de periodo: acepta "MM-yyyy" o "yyyy-MM" (ej. "08-2025" o "2025-08").
- Content-Types: multipart/form-data para importación; application/json para respuestas; el export devuelve application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; el log devuelve text/csv.
- Almacén (warehouseId) es opcional en la mayoría de endpoints.


1) Importar Inventario (carga masiva)
POST /api/sigmav2/inventory/import

Parámetros (multipart/form-data):
- file (obligatorio): Archivo Excel .xlsx o .csv (columnas sugeridas: CVE_ART, DECR, UNI_MED, EXIST, STATUS).
- type (obligatorio): Tipo/canal de inventario (ej. "DEFAULT").
- period (obligatorio): Periodo en formato MM-yyyy o yyyy-MM.
- warehouseId (opcional): Id de almacén.

Respuesta: 200 OK con texto "Importación iniciada correctamente". Se registra un Job de importación que puedes consultar.

Ejemplo curl:
  curl -X POST "http://localhost:8080/api/sigmav2/inventory/import" \
       -H "Authorization: Bearer <token>" \
       -H "Accept: application/json" \
       -F "file=@C:/ruta/INVENTARIO.xlsx" \
       -F "type=DEFAULT" \
       -F "period=08-2025" \
       -F "warehouseId=10"


2) Consultar estado general por tipo
GET /api/sigmav2/inventory/status/{type}

Path param:
- type: tipo/canal de inventario (ej. DEFAULT).

Respuesta: JSON con la última data guardada para ese tipo (estructura libre, incluye rows, headers, etc. si hubo importación).

Ejemplo:
  curl "http://localhost:8080/api/sigmav2/inventory/status/DEFAULT"


3) Consultar estado de una importación
GET /api/sigmav2/inventory/import/{jobId}

Path param:
- jobId: identificador del job de importación (UUID asignado internamente).

Respuesta: JSON con status y message.

Ejemplo:
  curl "http://localhost:8080/api/sigmav2/inventory/import/2e2b3c64-0e54-4aa9-8c7b-2d9fe3f3a1b2"


4) Descargar log de importación (CSV)
GET /api/sigmav2/inventory/import/{jobId}/log

Devuelve un archivo CSV con altas/bajas/actualizaciones detectadas durante la importación.

Ejemplo:
  curl -L -o import_log.csv "http://localhost:8080/api/sigmav2/inventory/import/2e2b3c64-0e54-4aa9-8c7b-2d9fe3f3a1b2/log"


5) Consultar inventario (filtros + paginación)
GET /api/sigmav2/inventory/query

Query params:
- type (opcional, default=DEFAULT): tipo de inventario.
- period (obligatorio): periodo a consultar.
- warehouseId (opcional): filtra por almacén si aplica.
- search (opcional): texto libre; filtra por coincidencia en cualquier columna.
- page (opcional): índice de página (0 por defecto).
- size (opcional): tamaño de página (10 por defecto).

Respuesta: JSON con:
{
  "period": "08-2025",
  "type": "DEFAULT",
  "warehouseId": 10,
  "total": 123,
  "page": 0,
  "size": 25,
  "headers": ["CVE_ART","DECR","UNI_MED","EXIST","STATUS"],
  "items": [["A001","Producto A","PZA","100","ACTIVO"], ...]
}

Ejemplos:
- Página 0, tamaño 25 con búsqueda:
  curl "http://localhost:8080/api/sigmav2/inventory/query?type=DEFAULT&period=08-2025&warehouseId=10&search=tuerca&page=0&size=25"

- Sólo por periodo (sin almacén):
  curl "http://localhost:8080/api/sigmav2/inventory/query?period=2025-08"


6) Exportar inventario a XLSX (respeta filtros)
GET /api/sigmav2/inventory/export

Query params:
- type (opcional, default=DEFAULT)
- period (obligatorio)
- warehouseId (opcional)
- search (opcional)

Comportamiento:
- La exportación SIEMPRE incluye todas las filas que cumplen los filtros (no solo la página visible).
- El archivo se descarga con nombre: Inventario_<periodo>[_almacen_<id>].xlsx

Ejemplo:
  curl -L -o Inventario_08-2025.xlsx "http://localhost:8080/api/sigmav2/inventory/export?type=DEFAULT&period=08-2025&warehouseId=10&search=tuerca"


Consideraciones y validaciones importantes
- period es obligatorio en import y query/export; formato inválido será rechazado.
- Si el periodo existe y está CLOSED/LOCKED, la importación será rechazada.
- search realiza coincidencia en texto (case-insensitive) sobre todas las columnas.
- Paginación: page empieza en 0; size por defecto 10 si no se envía.
- Exportar respeta exactamente los mismos filtros de query.
- Durante importación se genera un log CSV descargable en /import/{jobId}/log.


Ejemplos en Postman (resumen)
- POST /inventory/import: tipo POST con Body -> form-data (file, type, period, warehouseId). Agrega encabezado Authorization si aplica.
- GET /inventory/query: tipo GET con params en la pestaña Params.
- GET /inventory/export: tipo GET que descarga archivo; en Postman verás "Send and Download".
- GET /inventory/import/{jobId} y /{jobId}/log: tipo GET; el segundo descarga CSV.


Preguntas frecuentes
- ¿Qué valores usar para type? DEFAULT es el valor por defecto; si tu negocio maneja otros canales, úsalo consistentemente en import y consultas.
- ¿warehouseId es obligatorio? No; si no lo envías, consulta/guarda a nivel general del tipo/periodo.
- ¿Qué columnas reconoce la importación? La implementación actual persiste filas tal cual y usa la primera fila como headers si existe. Se recomienda: CVE_ART, DECR, UNI_MED, EXIST, STATUS.
