-- =====================================================
-- SCRIPT 3: Modificar tabla CLASES
-- =====================================================
-- Agrega columna asignatura_id para vincular clases con asignaturas
-- Las clases existentes quedarán con asignatura_id NULL (migrar manualmente)
-- =====================================================

-- Agregar columna asignatura_id con FK
ALTER TABLE public.clases
ADD COLUMN IF NOT EXISTS asignatura_id UUID REFERENCES public.asignaturas(id) ON DELETE CASCADE;

-- Crear índice para optimizar JOIN clases-asignaturas
CREATE INDEX IF NOT EXISTS idx_clases_asignatura_id ON public.clases(asignatura_id);

-- Comentario
COMMENT ON COLUMN public.clases.asignatura_id IS 'FK a asignaturas - Permite agrupar clases por asignatura/curso';

-- =====================================================
-- ACTUALIZAR POLÍTICAS RLS DE CLASES
-- =====================================================

-- Eliminar políticas antiguas (si existen)
DROP POLICY IF EXISTS "Usuarios autenticados pueden ver todas las clases" ON public.clases;
DROP POLICY IF EXISTS "Usuarios autenticados pueden crear clases" ON public.clases;
DROP POLICY IF EXISTS "Usuarios pueden actualizar sus clases" ON public.clases;
DROP POLICY IF EXISTS "Usuarios pueden eliminar sus clases" ON public.clases;

-- NUEVA Política: Los docentes pueden ver clases de sus asignaturas
CREATE POLICY "Docentes pueden ver clases de sus asignaturas"
    ON public.clases
    FOR SELECT
    USING (
        -- Docente es creador de la clase
        auth.uid() = creador
        OR
        -- Docente es dueño de la asignatura de la clase
        EXISTS (
            SELECT 1 FROM public.asignaturas
            WHERE asignaturas.id = clases.asignatura_id
            AND asignaturas.docente_id = auth.uid()
        )
    );

-- NUEVA Política: Los alumnos pueden ver clases de asignaturas en las que están inscritos
CREATE POLICY "Alumnos pueden ver clases de asignaturas inscritas"
    ON public.clases
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.alumno_asignaturas
            WHERE alumno_asignaturas.asignatura_id = clases.asignatura_id
            AND alumno_asignaturas.alumno_id = auth.uid()
        )
    );

-- NUEVA Política: Los docentes pueden crear clases en sus asignaturas
CREATE POLICY "Docentes pueden crear clases en sus asignaturas"
    ON public.clases
    FOR INSERT
    WITH CHECK (
        auth.uid() = creador
        AND
        -- Verificar que la asignatura pertenece al docente
        (
            asignatura_id IS NULL
            OR
            EXISTS (
                SELECT 1 FROM public.asignaturas
                WHERE asignaturas.id = clases.asignatura_id
                AND asignaturas.docente_id = auth.uid()
            )
        )
    );

-- NUEVA Política: Los docentes pueden actualizar clases de sus asignaturas
CREATE POLICY "Docentes pueden actualizar clases de sus asignaturas"
    ON public.clases
    FOR UPDATE
    USING (
        auth.uid() = creador
        OR
        EXISTS (
            SELECT 1 FROM public.asignaturas
            WHERE asignaturas.id = clases.asignatura_id
            AND asignaturas.docente_id = auth.uid()
        )
    )
    WITH CHECK (
        auth.uid() = creador
        OR
        EXISTS (
            SELECT 1 FROM public.asignaturas
            WHERE asignaturas.id = clases.asignatura_id
            AND asignaturas.docente_id = auth.uid()
        )
    );

-- NUEVA Política: Los docentes pueden eliminar clases de sus asignaturas
CREATE POLICY "Docentes pueden eliminar clases de sus asignaturas"
    ON public.clases
    FOR DELETE
    USING (
        auth.uid() = creador
        OR
        EXISTS (
            SELECT 1 FROM public.asignaturas
            WHERE asignaturas.id = clases.asignatura_id
            AND asignaturas.docente_id = auth.uid()
        )
    );

-- =====================================================
-- NOTA IMPORTANTE: MIGRACIÓN DE DATOS
-- =====================================================
-- Las clases existentes quedarán con asignatura_id = NULL
-- Para migrar, se debe:
-- 1. Crear una asignatura por defecto para cada docente
-- 2. Asignar todas las clases antiguas a esa asignatura
-- Ver script 05_migrate_existing_clases.sql
