-- =====================================================
-- SCRIPT 2: Crear tabla ALUMNO_ASIGNATURAS
-- =====================================================
-- Tabla de relación N:M entre alumnos y asignaturas
-- Permite que alumnos se inscriban en múltiples asignaturas
-- y cada asignatura tenga múltiples alumnos
-- =====================================================

CREATE TABLE IF NOT EXISTS public.alumno_asignaturas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alumno_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    asignatura_id UUID NOT NULL REFERENCES public.asignaturas(id) ON DELETE CASCADE,
    fecha_inscripcion TIMESTAMPTZ DEFAULT NOW(),
    estado VARCHAR(20) DEFAULT 'activo' CHECK (estado IN ('activo', 'inactivo', 'completado')),

    -- Constraint: Un alumno no puede inscribirse dos veces en la misma asignatura
    CONSTRAINT unique_alumno_asignatura UNIQUE (alumno_id, asignatura_id)
);

-- Índices para optimizar consultas de relaciones
CREATE INDEX IF NOT EXISTS idx_alumno_asignaturas_alumno_id ON public.alumno_asignaturas(alumno_id);
CREATE INDEX IF NOT EXISTS idx_alumno_asignaturas_asignatura_id ON public.alumno_asignaturas(asignatura_id);
CREATE INDEX IF NOT EXISTS idx_alumno_asignaturas_fecha ON public.alumno_asignaturas(fecha_inscripcion DESC);
CREATE INDEX IF NOT EXISTS idx_alumno_asignaturas_estado ON public.alumno_asignaturas(estado);

-- Comentarios para documentación
COMMENT ON TABLE public.alumno_asignaturas IS 'Tabla de relación N:M entre alumnos y asignaturas (inscripciones)';
COMMENT ON COLUMN public.alumno_asignaturas.estado IS 'Estado de la inscripción: activo (cursando), inactivo (retirado), completado (aprobado)';

-- =====================================================
-- POLÍTICAS RLS (Row Level Security)
-- =====================================================
ALTER TABLE public.alumno_asignaturas ENABLE ROW LEVEL SECURITY;

-- Política: Los alumnos pueden ver sus propias inscripciones
CREATE POLICY "Alumnos pueden ver sus inscripciones"
    ON public.alumno_asignaturas
    FOR SELECT
    USING (auth.uid() = alumno_id);

-- Política: Los docentes pueden ver inscripciones de sus asignaturas
CREATE POLICY "Docentes pueden ver inscripciones de sus asignaturas"
    ON public.alumno_asignaturas
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.asignaturas
            WHERE asignaturas.id = alumno_asignaturas.asignatura_id
            AND asignaturas.docente_id = auth.uid()
        )
    );

-- Política: Los alumnos pueden inscribirse en asignaturas (INSERT)
CREATE POLICY "Alumnos pueden inscribirse"
    ON public.alumno_asignaturas
    FOR INSERT
    WITH CHECK (auth.uid() = alumno_id);

-- Política: Los alumnos pueden darse de baja (UPDATE estado)
CREATE POLICY "Alumnos pueden actualizar sus inscripciones"
    ON public.alumno_asignaturas
    FOR UPDATE
    USING (auth.uid() = alumno_id)
    WITH CHECK (auth.uid() = alumno_id);

-- Política: Los alumnos pueden eliminar sus inscripciones
CREATE POLICY "Alumnos pueden eliminar sus inscripciones"
    ON public.alumno_asignaturas
    FOR DELETE
    USING (auth.uid() = alumno_id);

-- Política: Los docentes pueden eliminar inscripciones de sus asignaturas
CREATE POLICY "Docentes pueden eliminar inscripciones de sus asignaturas"
    ON public.alumno_asignaturas
    FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM public.asignaturas
            WHERE asignaturas.id = alumno_asignaturas.asignatura_id
            AND asignaturas.docente_id = auth.uid()
        )
    );
