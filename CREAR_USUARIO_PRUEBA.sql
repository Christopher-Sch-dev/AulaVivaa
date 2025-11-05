-- ============================================
-- SOLUCIÓN: CONFIRMAR USUARIO EXISTENTE
-- ============================================

-- PASO 1: CONFIRMAR el email del usuario d1@d1.com
UPDATE auth.users
SET email_confirmed_at = NOW()
WHERE email = 'd1@d1.com'
AND email_confirmed_at IS NULL;

-- PASO 2: INSERTAR en la tabla usuarios
INSERT INTO public.usuarios (id, email, rol, nombre)
VALUES (
  '6967cf7e-48e8-4516-930e-83cc75586f3a'::uuid,
  'd1@d1.com',
  'docente',
  'd1'
)
ON CONFLICT (id) DO UPDATE SET
  email = EXCLUDED.email,
  rol = EXCLUDED.rol,
  nombre = EXCLUDED.nombre;

-- PASO 3: VERIFICAR que todo está correcto
SELECT
    u.id,
    u.email,
    u.rol,
    u.nombre,
    au.email_confirmed_at as confirmado_en,
    CASE WHEN au.email_confirmed_at IS NULL THEN '❌ NO' ELSE '✅ SÍ' END as confirmado
FROM public.usuarios u
JOIN auth.users au ON u.id = au.id
WHERE u.email = 'd1@d1.com';

-- ============================================
-- RESULTADO ESPERADO:
-- Debes ver 1 fila con:
-- - email: d1@d1.com
-- - rol: docente
-- - confirmado: ✅ SÍ
-- ============================================
