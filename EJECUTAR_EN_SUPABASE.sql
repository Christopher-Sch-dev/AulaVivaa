-- ============================================
-- SCRIPT PARA CREAR TABLA USUARIOS EN SUPABASE
-- Ejecutar en: SQL Editor del Dashboard
-- ============================================

-- 1. CREAR TABLA USUARIOS
CREATE TABLE IF NOT EXISTS public.usuarios (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL UNIQUE,
    rol TEXT NOT NULL CHECK (rol IN ('docente', 'alumno')),
    nombre TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. HABILITAR ROW LEVEL SECURITY
ALTER TABLE public.usuarios ENABLE ROW LEVEL SECURITY;

-- 3. CREAR POLICIES (Permisos)

-- Policy: Los usuarios autenticados pueden leer todos los usuarios
CREATE POLICY "usuarios_select_authenticated" ON public.usuarios
    FOR SELECT TO authenticated
    USING (true);

-- Policy: Los usuarios pueden insertar su propio registro
CREATE POLICY "usuarios_insert_own" ON public.usuarios
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = id);

-- Policy: Los usuarios pueden actualizar su propio registro
CREATE POLICY "usuarios_update_own" ON public.usuarios
    FOR UPDATE TO authenticated
    USING (auth.uid() = id);

-- Policy: Los usuarios pueden eliminar su propio registro
CREATE POLICY "usuarios_delete_own" ON public.usuarios
    FOR DELETE TO authenticated
    USING (auth.uid() = id);

-- 4. VERIFICAR QUE SE CREÓ CORRECTAMENTE
SELECT * FROM public.usuarios LIMIT 5;

-- ============================================
-- Si ya tienes usuarios en auth.users sin registro en usuarios:
-- ============================================

-- Ver usuarios en auth que NO están en la tabla usuarios:
SELECT au.id, au.email, au.created_at
FROM auth.users au
LEFT JOIN public.usuarios u ON au.id = u.id
WHERE u.id IS NULL;

-- ============================================
-- CREAR REGISTRO PARA USUARIO EXISTENTE (ejemplo):
-- ============================================

-- Reemplaza 'USUARIO_ID_AQUI' con el UUID real del usuario
-- INSERT INTO public.usuarios (id, email, rol, nombre)
-- VALUES (
--   'USUARIO_ID_AQUI',
--   'email@ejemplo.com',
--   'docente',
--   'Nombre Usuario'
-- );
