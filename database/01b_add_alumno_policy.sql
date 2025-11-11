-- =====================================================
-- SCRIPT 1B: Agregar política RLS para alumnos
-- =====================================================
-- Este script se ejecuta DESPUÉS de crear alumno_asignaturas
-- Agrega la política que permite a alumnos ver asignaturas inscritas
-- =====================================================

-- Política: Los alumnos pueden ver asignaturas en las que están inscritos
CREATE POLICY "Alumnos pueden ver asignaturas inscritas"
    ON public.asignaturas
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.alumno_asignaturas
            WHERE alumno_asignaturas.asignatura_id = asignaturas.id
            AND alumno_asignaturas.alumno_id = auth.uid()
        )
    );
