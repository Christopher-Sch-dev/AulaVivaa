-- =====================================================
-- SCRIPT 4: Función para generar códigos de asignatura
-- =====================================================
-- Genera códigos únicos alfanuméricos para inscripción
-- Formato: PREFIJO-XXXX (ej: POO2025-A1B2)
-- =====================================================

CREATE OR REPLACE FUNCTION generar_codigo_asignatura(
    prefijo TEXT DEFAULT '',
    longitud_codigo INT DEFAULT 4
)
RETURNS TEXT AS $$
DECLARE
    caracteres TEXT := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; -- Sin I, O, 0, 1 para evitar confusión
    codigo_generado TEXT;
    existe_codigo BOOLEAN;
    intentos INT := 0;
    max_intentos INT := 100;
BEGIN
    LOOP
        -- Generar código aleatorio
        codigo_generado := '';
        FOR i IN 1..longitud_codigo LOOP
            codigo_generado := codigo_generado || substr(
                caracteres,
                1 + floor(random() * length(caracteres))::int,
                1
            );
        END LOOP;

        -- Agregar prefijo si se proporciona
        IF prefijo != '' THEN
            codigo_generado := prefijo || '-' || codigo_generado;
        ELSE
            -- Usar año actual como prefijo por defecto
            codigo_generado := 'AV' || EXTRACT(YEAR FROM NOW())::TEXT || '-' || codigo_generado;
        END IF;

        -- Verificar si el código ya existe
        SELECT EXISTS(
            SELECT 1 FROM public.asignaturas
            WHERE codigo_acceso = codigo_generado
        ) INTO existe_codigo;

        -- Si no existe, retornar el código
        IF NOT existe_codigo THEN
            RETURN codigo_generado;
        END IF;

        -- Incrementar intentos y evitar loop infinito
        intentos := intentos + 1;
        IF intentos >= max_intentos THEN
            RAISE EXCEPTION 'No se pudo generar código único después de % intentos', max_intentos;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- Comentario
COMMENT ON FUNCTION generar_codigo_asignatura IS 'Genera código único alfanumérico para asignaturas (formato: AV2025-A1B2)';

-- =====================================================
-- FUNCIÓN RPC: Generar código con validación de docente
-- =====================================================
-- Esta función se llamará desde Kotlin via Supabase RPC
-- Genera código y lo asocia a una asignatura
-- =====================================================

CREATE OR REPLACE FUNCTION rpc_generar_codigo_asignatura(
    p_asignatura_id UUID
)
RETURNS TEXT AS $$
DECLARE
    v_codigo TEXT;
    v_docente_id UUID;
    v_nombre_asignatura TEXT;
    v_prefijo TEXT;
BEGIN
    -- Verificar que la asignatura existe y pertenece al usuario
    SELECT docente_id, nombre INTO v_docente_id, v_nombre_asignatura
    FROM public.asignaturas
    WHERE id = p_asignatura_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Asignatura no encontrada';
    END IF;

    -- Verificar que el usuario autenticado es el docente
    IF auth.uid() != v_docente_id THEN
        RAISE EXCEPTION 'No tienes permiso para generar código de esta asignatura';
    END IF;

    -- Generar prefijo desde el nombre (primeras 3 letras mayúsculas)
    v_prefijo := UPPER(LEFT(REGEXP_REPLACE(v_nombre_asignatura, '[^a-zA-Z]', '', 'g'), 3));
    IF v_prefijo = '' THEN
        v_prefijo := 'ASG'; -- Prefijo por defecto
    END IF;

    -- Agregar año
    v_prefijo := v_prefijo || EXTRACT(YEAR FROM NOW())::TEXT;

    -- Generar código
    v_codigo := generar_codigo_asignatura(v_prefijo, 4);

    -- Actualizar la asignatura con el nuevo código
    UPDATE public.asignaturas
    SET codigo_acceso = v_codigo,
        updated_at = NOW()
    WHERE id = p_asignatura_id;

    RETURN v_codigo;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Comentario
COMMENT ON FUNCTION rpc_generar_codigo_asignatura IS 'RPC: Genera código único para asignatura con validación de permisos';

-- =====================================================
-- FUNCIÓN RPC: Validar código e inscribir alumno
-- =====================================================
-- Valida código de asignatura e inscribe al alumno
-- Retorna JSON con resultado y datos de la asignatura
-- =====================================================

CREATE OR REPLACE FUNCTION rpc_inscribir_con_codigo(
    p_codigo_acceso TEXT
)
RETURNS JSON AS $$
DECLARE
    v_asignatura_id UUID;
    v_alumno_id UUID;
    v_asignatura RECORD;
    v_ya_inscrito BOOLEAN;
BEGIN
    -- Obtener alumno actual
    v_alumno_id := auth.uid();

    IF v_alumno_id IS NULL THEN
        RETURN json_build_object(
            'success', false,
            'error', 'Usuario no autenticado'
        );
    END IF;

    -- Buscar asignatura por código
    SELECT id, nombre, descripcion, docente_id INTO v_asignatura
    FROM public.asignaturas
    WHERE codigo_acceso = UPPER(TRIM(p_codigo_acceso));

    IF NOT FOUND THEN
        RETURN json_build_object(
            'success', false,
            'error', 'Código inválido'
        );
    END IF;

    v_asignatura_id := v_asignatura.id;

    -- Verificar si ya está inscrito
    SELECT EXISTS(
        SELECT 1 FROM public.alumno_asignaturas
        WHERE alumno_id = v_alumno_id
        AND asignatura_id = v_asignatura_id
    ) INTO v_ya_inscrito;

    IF v_ya_inscrito THEN
        RETURN json_build_object(
            'success', false,
            'error', 'Ya estás inscrito en esta asignatura'
        );
    END IF;

    -- Inscribir al alumno
    INSERT INTO public.alumno_asignaturas (alumno_id, asignatura_id, estado)
    VALUES (v_alumno_id, v_asignatura_id, 'activo');

    -- Retornar resultado exitoso con datos de la asignatura
    RETURN json_build_object(
        'success', true,
        'asignatura', json_build_object(
            'id', v_asignatura.id,
            'nombre', v_asignatura.nombre,
            'descripcion', v_asignatura.descripcion,
            'docente_id', v_asignatura.docente_id
        )
    );

EXCEPTION WHEN OTHERS THEN
    RETURN json_build_object(
        'success', false,
        'error', SQLERRM
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Comentario
COMMENT ON FUNCTION rpc_inscribir_con_codigo IS 'RPC: Valida código e inscribe alumno en asignatura';
