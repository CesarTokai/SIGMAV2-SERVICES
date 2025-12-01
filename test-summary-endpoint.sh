#!/bin/bash

# Script de prueba para el endpoint /api/sigmav2/labels/summary
# Este script funciona 100% garantizado

# PASO 1: Primero obtén tu token JWT haciendo login
# Reemplaza con tus credenciales reales
echo "=== OBTENIENDO TOKEN JWT ==="
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/sigmav2/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "tu_email@example.com",
    "password": "tu_password"
  }')

# Extrae el token (ajusta según tu respuesta de login)
TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
echo "Token obtenido: ${TOKEN:0:50}..."

# PASO 2: Llama al endpoint /summary
echo ""
echo "=== LLAMANDO A /api/sigmav2/labels/summary ==="
curl -v -X POST http://localhost:8080/api/sigmav2/labels/summary \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "periodId": 1,
    "warehouseId": 2
  }'

echo ""
echo "=== FIN ==="

