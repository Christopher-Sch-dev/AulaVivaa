-- =====================================================
-- SCRIPT 1B: Agregar política RLS para alumnos
-- =====================================================
-- Este script se ejecuta DESPUÉS de crear alumno_asignaturas
-- Agrega la política que permite a alumnos ver asignaturas inscritas
-- =====================================================

-- ⚠️ IMPORTANTE: Primero ELIMINAR la política vieja si existe para evitar recursión
DROP POLICY IF EXISTS "Alumnos pueden ver asignaturas inscritas" ON public.asignaturas;

-- Política: Los alumnos pueden ver asignaturas en las que están inscritos
-- ✅ CORREGIDA: Sin recursión infinita usando subconsulta directa
CREATE POLICY "Alumnos pueden ver asignaturas inscritas"
    ON public.asignaturas
    FOR SELECT
    USING (
        -- Solo aplicar si NO es el dueño (docente)
        (auth.uid() != docente_id)
        AND
        -- Verificar inscripción sin crear recursión
        (
            asignaturas.id IN (
                SELECT asignatura_id
                FROM public.alumno_asignaturas
                WHERE alumno_id = auth.uid()
            )
        )
    );
