-- =====================================================
-- SCRIPT 1: Crear tabla ASIGNATURAS (VERSIÓN CORREGIDA)
-- =====================================================
-- Tabla para gestionar asignaturas/cursos creados por docentes
-- Cada asignatura tiene un código único para inscripción de alumnos
-- =====================================================

CREATE TABLE IF NOT EXISTS public.asignaturas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(200) NOT NULL,
    codigo_acceso VARCHAR(15) UNIQUE NOT NULL,
    docente_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    descripcion TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_asignaturas_docente_id ON public.asignaturas(docente_id);
CREATE INDEX IF NOT EXISTS idx_asignaturas_codigo_acceso ON public.asignaturas(codigo_acceso);
CREATE INDEX IF NOT EXISTS idx_asignaturas_created_at ON public.asignaturas(created_at DESC);

-- Comentarios para documentación
COMMENT ON TABLE public.asignaturas IS 'Asignaturas/cursos creados por docentes con código de acceso único';
COMMENT ON COLUMN public.asignaturas.codigo_acceso IS 'Código único generado para inscripción de alumnos (formato: POO2025-A1B2)';
COMMENT ON COLUMN public.asignaturas.docente_id IS 'UUID del docente creador (FK a auth.users)';

-- Trigger para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_asignaturas_updated_at
    BEFORE UPDATE ON public.asignaturas
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- POLÍTICAS RLS (Row Level Security)
-- =====================================================
ALTER TABLE public.asignaturas ENABLE ROW LEVEL SECURITY;

-- Política: Los docentes solo pueden ver sus propias asignaturas
CREATE POLICY "Docentes pueden ver sus asignaturas"
    ON public.asignaturas
    FOR SELECT
    USING (auth.uid() = docente_id);

-- Política: Los docentes pueden crear asignaturas
CREATE POLICY "Docentes pueden crear asignaturas"
    ON public.asignaturas
    FOR INSERT
    WITH CHECK (auth.uid() = docente_id);

-- Política: Los docentes pueden actualizar sus asignaturas
CREATE POLICY "Docentes pueden actualizar sus asignaturas"
    ON public.asignaturas
    FOR UPDATE
    USING (auth.uid() = docente_id)
    WITH CHECK (auth.uid() = docente_id);

-- Política: Los docentes pueden eliminar sus asignaturas
CREATE POLICY "Docentes pueden eliminar sus asignaturas"
    ON public.asignaturas
    FOR DELETE
    USING (auth.uid() = docente_id);

-- NOTA: La política para alumnos se agregará después de crear la tabla alumno_asignaturas
-- Ver script: 02_create_alumno_asignaturas_table.sql (última sección)
