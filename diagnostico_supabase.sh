#!/usr/bin/env bash
# Script de Diagnóstico Completo - Supabase Auth
# Ejecutar desde: C:\Users\Chris\AndroidStudioProjects\AulaViva

echo "=================================="
echo "🔍 DIAGNÓSTICO SUPABASE AUTH"
echo "=================================="
echo ""

# Variables
SUPABASE_URL="https://xcnkcmzfevimydfpyjrr.supabase.co"
ANON_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhjbmtjbXpmZXZpbXlkZnB5anJyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIyOTUxNzYsImV4cCI6MjA3Nzg3MTE3Nn0.0llGaPrB-ThexZ3AbGg9VXYo8b_0ZwgzBIxGAeezFiM"

echo "📡 1. Verificando conectividad con Supabase..."
curl -s -o /dev/null -w "Status: %{http_code}\n" \
  "$SUPABASE_URL/rest/v1/" \
  -H "apikey: $ANON_KEY"

echo ""
echo "🔐 2. Probando endpoint de Auth signup..."
curl -v -X POST "$SUPABASE_URL/auth/v1/signup" \
  -H "apikey: $ANON_KEY" \
  -H "Content-Type: application/json" \
  -d '{"email":"diagnostico@test.com","password":"test123456"}' \
  2>&1 | grep -E "HTTP|email|error|message"

echo ""
echo "📋 3. Verificando tabla usuarios..."
curl -s "$SUPABASE_URL/rest/v1/usuarios?select=id,email,rol&limit=5" \
  -H "apikey: $ANON_KEY" \
  -H "Authorization: Bearer $ANON_KEY" \
  | python -m json.tool 2>/dev/null || echo "Error: tabla no accesible o vacía"

echo ""
echo "🔍 4. Verificando tabla clases..."
curl -s "$SUPABASE_URL/rest/v1/clases?select=id,nombre&limit=3" \
  -H "apikey: $ANON_KEY" \
  -H "Authorization: Bearer $ANON_KEY" \
  | python -m json.tool 2>/dev/null || echo "Error: tabla no accesible o vacía"

echo ""
echo "=================================="
echo "✅ Diagnóstico completado"
echo "=================================="
