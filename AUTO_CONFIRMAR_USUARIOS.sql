-- ============================================
-- CONFIGURACIÓN AUTOMÁTICA DE CONFIRMACIÓN
-- ============================================

-- OPCIÓN 1: Confirmar TODOS los usuarios existentes que no están confirmados
UPDATE auth.users
SET email_confirmed_at = NOW()
WHERE email_confirmed_at IS NULL;

-- OPCIÓN 2: Crear un trigger para confirmar automáticamente usuarios nuevos
-- (Requiere activar Database Webhooks en Supabase)

-- NOTA: La forma más fácil es deshabilitar "Email Confirmations" en:
-- Dashboard > Authentication > Settings > Enable email confirmations = OFF

-- ============================================
-- SCRIPT PARA SINCRONIZAR USUARIOS
-- ============================================
-- Este script asegura que TODOS los usuarios en auth.users
-- también existan en la tabla usuarios

INSERT INTO public.usuarios (id, email, rol, nombre)
SELECT
  au.id,
  au.email,
  'docente' as rol,  -- Rol por defecto
  COALESCE(SPLIT_PART(au.email, '@', 1), 'Usuario') as nombre
FROM auth.users au
LEFT JOIN public.usuarios u ON au.id = u.id
WHERE u.id IS NULL  -- Solo los que NO están en usuarios
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- VERIFICAR RESULTADO
-- ============================================
SELECT
    au.id,
    au.email,
    CASE WHEN au.email_confirmed_at IS NULL THEN '❌ NO' ELSE '✅ SÍ' END as confirmado,
    CASE WHEN u.id IS NULL THEN '❌ NO' ELSE '✅ SÍ' END as en_tabla_usuarios,
    u.rol
FROM auth.users au
LEFT JOIN public.usuarios u ON au.id = u.id
ORDER BY au.created_at DESC;
