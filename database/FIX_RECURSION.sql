-- =====================================================
-- SCRIPT FIX: Corregir recursión infinita en políticas RLS
-- =====================================================
-- EJECUTAR ESTE SCRIPT EN SUPABASE SQL EDITOR
-- Soluciona: "infinite recursion detected in policy for relation asignaturas"
-- =====================================================

-- 🔴 PASO 1: ELIMINAR POLÍTICA PROBLEMÁTICA
DROP POLICY IF EXISTS "Alumnos pueden ver asignaturas inscritas" ON public.asignaturas;

-- ✅ PASO 2: RECREAR POLÍTICA SIN RECURSIÓN
CREATE POLICY "Alumnos pueden ver asignaturas inscritas"
    ON public.asignaturas
    FOR SELECT
    USING (
        -- Solo aplicar si NO es el dueño (docente)
        (auth.uid() != docente_id)
        AND
        -- Verificar inscripción directamente sin EXISTS anidado
        (
            asignaturas.id IN (
                SELECT asignatura_id
                FROM public.alumno_asignaturas
                WHERE alumno_id = auth.uid()
            )
        )
    );

-- 🔍 PASO 3: VERIFICAR POLÍTICAS ACTIVAS
SELECT
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual,
    with_check
FROM pg_policies
WHERE tablename = 'asignaturas'
ORDER BY policyname;

-- ✅ PASO 4: MENSAJE DE CONFIRMACIÓN
DO $$
BEGIN
    RAISE NOTICE '✅ Política corregida - Recursión eliminada';
END $$;
