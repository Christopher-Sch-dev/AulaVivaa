@echo off
REM Script de Diagnóstico Completo - Supabase Auth (Windows)
REM Ejecutar desde: C:\Users\Chris\AndroidStudioProjects\AulaViva

echo ==================================
echo 🔍 DIAGNÓSTICO SUPABASE AUTH
echo ==================================
echo.

set SUPABASE_URL=https://xcnkcmzfevimydfpyjrr.supabase.co
set ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhjbmtjbXpmZXZpbXlkZnB5anJyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIyOTUxNzYsImV4cCI6MjA3Nzg3MTE3Nn0.0llGaPrB-ThexZ3AbGg9VXYo8b_0ZwgzBIxGAeezFiM

echo 📡 1. Verificando conectividad con Supabase...
curl -s -o nul -w "Status: %%{http_code}" "%SUPABASE_URL%/rest/v1/" -H "apikey: %ANON_KEY%"
echo.
echo.

echo 🔐 2. Probando endpoint de Auth signup...
curl -X POST "%SUPABASE_URL%/auth/v1/signup" -H "apikey: %ANON_KEY%" -H "Content-Type: application/json" -d "{\"email\":\"diagnostico@test.com\",\"password\":\"test123456\"}"
echo.
echo.

echo 📋 3. Verificando tabla usuarios (primeros 5)...
curl -s "%SUPABASE_URL%/rest/v1/usuarios?select=id,email,rol&limit=5" -H "apikey: %ANON_KEY%" -H "Authorization: Bearer %ANON_KEY%"
echo.
echo.

echo 🔍 4. Verificando tabla clases (primeras 3)...
curl -s "%SUPABASE_URL%/rest/v1/clases?select=id,nombre&limit=3" -H "apikey: %ANON_KEY%" -H "Authorization: Bearer %ANON_KEY%"
echo.
echo.

echo 🔑 5. Verificando configuración de Email Auth en Supabase...
echo Nota: Debes verificar manualmente en el Dashboard:
echo - Authentication ^> Settings ^> Enable Email provider
echo - Email Templates configurados
echo - Disable email confirmations (para testing)
echo.

echo ==================================
echo ✅ Diagnóstico completado
echo ==================================
echo.
echo 📝 Siguiente paso:
echo Ve al Supabase Dashboard y verifica:
echo 1. Authentication ^> Settings ^> Email Auth habilitado
echo 2. Table Editor ^> usuarios (debe existir)
echo 3. Table Editor ^> clases (debe existir)
echo.
pause
