-- ==================================================================
-- POLÍTICAS RLS PARA ALUMNOS - ACCESO A ASIGNATURAS POR CÓDIGO
-- ==================================================================

-- ✅ POLÍTICA 1: Alumno puede LEER asignaturas (para buscar por código)
-- Sin esta política, el alumno no puede buscar asignaturas por código_acceso
DROP POLICY IF EXISTS "Alumnos pueden leer asignaturas" ON public.asignaturas;
CREATE POLICY "Alumnos pueden leer asignaturas"
ON public.asignaturas
FOR SELECT
TO authenticated
USING (
    -- Cualquier usuario autenticado puede leer asignaturas
    -- Necesario para que los alumnos busquen por código
    true
);

-- ✅ POLÍTICA 2: Alumno puede INSERTAR sus propias inscripciones
DROP POLICY IF EXISTS "Alumnos pueden inscribirse" ON public.alumno_asignaturas;
CREATE POLICY "Alumnos pueden inscribirse"
ON public.alumno_asignaturas
FOR INSERT
TO authenticated
WITH CHECK (
    -- Solo puede insertar inscripciones donde él es el alumno
    auth.uid() = alumno_id
);

-- ✅ POLÍTICA 3: Alumno puede LEER sus propias inscripciones
DROP POLICY IF EXISTS "Alumnos pueden ver sus inscripciones" ON public.alumno_asignaturas;
CREATE POLICY "Alumnos pueden ver sus inscripciones"
ON public.alumno_asignaturas
FOR SELECT
TO authenticated
USING (
    -- Solo ve sus propias inscripciones
    auth.uid() = alumno_id
);

-- ✅ POLÍTICA 4: Alumno puede ACTUALIZAR sus propias inscripciones (para darse de baja)
DROP POLICY IF EXISTS "Alumnos pueden actualizar sus inscripciones" ON public.alumno_asignaturas;
CREATE POLICY "Alumnos pueden actualizar sus inscripciones"
ON public.alumno_asignaturas
FOR UPDATE
TO authenticated
USING (auth.uid() = alumno_id)
WITH CHECK (auth.uid() = alumno_id);

-- ✅ POLÍTICA 5: Alumno puede ELIMINAR sus propias inscripciones
DROP POLICY IF EXISTS "Alumnos pueden eliminar sus inscripciones" ON public.alumno_asignaturas;
CREATE POLICY "Alumnos pueden eliminar sus inscripciones"
ON public.alumno_asignaturas
FOR DELETE
TO authenticated
USING (auth.uid() = alumno_id);

-- ==================================================================
-- VERIFICACIÓN DE POLÍTICAS
-- ==================================================================
-- Para ver las políticas activas:
-- SELECT * FROM pg_policies WHERE tablename IN ('asignaturas', 'alumno_asignaturas');
