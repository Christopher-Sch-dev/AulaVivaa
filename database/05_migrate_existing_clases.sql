-- =====================================================
-- SCRIPT 5: Migración de clases existentes
-- =====================================================
-- Este script migra las clases existentes (sin asignatura_id)
-- creando una asignatura por defecto para cada docente
-- =====================================================

DO $$
DECLARE
    v_docente RECORD;
    v_asignatura_id UUID;
    v_codigo TEXT;
    v_count_clases INT;
BEGIN
    RAISE NOTICE 'Iniciando migración de clases existentes...';

    -- Iterar sobre cada docente que tiene clases sin asignatura
    FOR v_docente IN
        SELECT DISTINCT creador
        FROM public.clases
        WHERE asignatura_id IS NULL
    LOOP
        -- Contar clases del docente
        SELECT COUNT(*) INTO v_count_clases
        FROM public.clases
        WHERE creador = v_docente.creador
        AND asignatura_id IS NULL;

        RAISE NOTICE 'Procesando docente % con % clases', v_docente.creador, v_count_clases;

        -- Generar código único para la asignatura por defecto
        v_codigo := generar_codigo_asignatura('MIG2025', 4);

        -- Crear asignatura por defecto para el docente
        INSERT INTO public.asignaturas (
            nombre,
            codigo_acceso,
            docente_id,
            descripcion
        ) VALUES (
            'Clases Generales ' || TO_CHAR(NOW(), 'YYYY'),
            v_codigo,
            v_docente.creador,
            'Asignatura creada automáticamente durante la migración. Contiene clases existentes antes de la implementación del sistema de asignaturas.'
        )
        RETURNING id INTO v_asignatura_id;

        RAISE NOTICE 'Asignatura creada con ID: % y código: %', v_asignatura_id, v_codigo;

        -- Migrar todas las clases del docente a la nueva asignatura
        UPDATE public.clases
        SET asignatura_id = v_asignatura_id
        WHERE creador = v_docente.creador
        AND asignatura_id IS NULL;

        RAISE NOTICE 'Migradas % clases a asignatura %', v_count_clases, v_asignatura_id;
    END LOOP;

    RAISE NOTICE 'Migración completada exitosamente';
END $$;

-- =====================================================
-- VERIFICACIÓN POST-MIGRACIÓN
-- =====================================================

-- Contar clases sin asignatura (debería ser 0)
DO $$
DECLARE
    v_clases_sin_asignatura INT;
BEGIN
    SELECT COUNT(*) INTO v_clases_sin_asignatura
    FROM public.clases
    WHERE asignatura_id IS NULL;

    IF v_clases_sin_asignatura > 0 THEN
        RAISE WARNING 'Atención: Quedan % clases sin asignatura', v_clases_sin_asignatura;
    ELSE
        RAISE NOTICE '✅ Todas las clases tienen asignatura asignada';
    END IF;
END $$;

-- Mostrar resumen de asignaturas creadas
SELECT
    a.id,
    a.nombre,
    a.codigo_acceso,
    a.docente_id,
    COUNT(c.id) AS total_clases
FROM public.asignaturas a
LEFT JOIN public.clases c ON c.asignatura_id = a.id
WHERE a.nombre LIKE 'Clases Generales%'
GROUP BY a.id, a.nombre, a.codigo_acceso, a.docente_id
ORDER BY a.created_at DESC;
