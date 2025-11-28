# 📡 Documentación de APIs REST - AulaViva Backend

Esta documentación describe todos los endpoints REST disponibles en el backend Spring Boot.

## 🔗 Base URL

```
http://localhost:8080/api
```

**Producción:** `https://tu-dominio.com/api`

---

## 🔐 Autenticación

Todos los endpoints (excepto `/api/auth/**`) requieren un token JWT en el header:

```
Authorization: Bearer {token}
```

El token se obtiene al hacer **login** o **register**.

---

## 📋 Endpoints Disponibles

### 🔑 Autenticación

#### `POST /api/auth/register`
Registrar nuevo usuario

**Request Body:**
```json
{
  "email": "usuario@example.com",
  "password": "password123",
  "rol": "docente"  // o "alumno"
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "usuario@example.com",
    "rol": "docente"
  },
  "message": "Usuario registrado exitosamente"
}
```

**Errores:**
- `400`: Email inválido, contraseña muy corta, rol inválido
- `409`: Email ya registrado

---

#### `POST /api/auth/login`
Iniciar sesión

**Request Body:**
```json
{
  "email": "usuario@example.com",
  "password": "password123"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "usuario@example.com",
    "rol": "docente"
  },
  "message": "Login exitoso"
}
```

**Errores:**
- `401`: Credenciales inválidas

---

#### `GET /api/auth/me`
Obtener información del usuario actual

**Headers:**
```
Authorization: Bearer {token}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "usuario@example.com",
    "rol": "docente",
    "nombre": "usuario"
  }
}
```

---

### 📚 Asignaturas

#### `POST /api/asignaturas`
Crear nueva asignatura (solo docentes)

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "nombre": "Programación Móvil",
  "descripcion": "Curso de desarrollo Android con Kotlin"
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Programación Móvil",
    "codigoAcceso": "PRO2025-A1B2",
    "docenteId": "550e8400-e29b-41d4-a716-446655440000",
    "descripcion": "Curso de desarrollo Android con Kotlin",
    "createdAt": "2025-12-10T10:00:00Z",
    "updatedAt": "2025-12-10T10:00:00Z"
  },
  "message": "Asignatura creada exitosamente"
}
```

---

#### `GET /api/asignaturas`
Obtener todas las asignaturas del docente actual

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": "...",
      "nombre": "Programación Móvil",
      "codigoAcceso": "PRO2025-A1B2",
      ...
    }
  ]
}
```

---

#### `GET /api/asignaturas/{id}`
Obtener asignatura por ID

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "...",
    "nombre": "Programación Móvil",
    ...
  }
}
```

---

#### `PUT /api/asignaturas/{id}`
Actualizar asignatura

**Request Body:**
```json
{
  "nombre": "Programación Móvil Avanzada",
  "descripcion": "Curso avanzado..."
}
```

**Response 200:**
```json
{
  "success": true,
  "data": { ... },
  "message": "Asignatura actualizada exitosamente"
}
```

**Errores:**
- `403`: No eres el dueño de la asignatura
- `404`: Asignatura no encontrada

---

#### `DELETE /api/asignaturas/{id}`
Eliminar asignatura

**Response 200:**
```json
{
  "success": true,
  "data": null,
  "message": "Asignatura eliminada exitosamente"
}
```

---

#### `POST /api/asignaturas/{id}/generar-codigo`
Generar nuevo código de acceso para la asignatura

**Response 200:**
```json
{
  "success": true,
  "data": {
    "codigo": "PRO2025-X9Y8"
  },
  "message": "Código generado exitosamente"
}
```

---

### 📖 Clases

#### `POST /api/clases`
Crear nueva clase (solo docentes)

**Request Body:**
```json
{
  "nombre": "Introducción a Kotlin",
  "descripcion": "Fundamentos del lenguaje Kotlin",
  "fecha": "Lunes 4 de Noviembre, 14:00hrs",
  "archivoPdfUrl": "https://...",
  "archivoPdfNombre": "clase1.pdf",
  "asignaturaId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": "...",
    "nombre": "Introducción a Kotlin",
    "descripcion": "...",
    "fecha": "Lunes 4 de Noviembre, 14:00hrs",
    "archivoPdfUrl": "https://...",
    "archivoPdfNombre": "clase1.pdf",
    "creador": "...",
    "asignaturaId": "...",
    "createdAt": "2025-12-10T10:00:00Z",
    "updatedAt": "2025-12-10T10:00:00Z"
  },
  "message": "Clase creada exitosamente"
}
```

---

#### `GET /api/clases`
Obtener clases

**Query Parameters:**
- `asignaturaId` (opcional): Filtrar por asignatura

**Ejemplo:**
```
GET /api/clases?asignaturaId=550e8400-e29b-41d4-a716-446655440000
```

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": "...",
      "nombre": "Introducción a Kotlin",
      ...
    }
  ]
}
```

---

#### `GET /api/clases/{id}`
Obtener clase por ID

---

#### `PUT /api/clases/{id}`
Actualizar clase

**Request Body:**
```json
{
  "nombre": "Kotlin Avanzado",
  "descripcion": "...",
  "fecha": "...",
  "archivoPdfUrl": "...",
  "archivoPdfNombre": "..."
}
```

---

#### `DELETE /api/clases/{id}`
Eliminar clase

---

### 🎓 Alumnos

#### `POST /api/alumnos/inscribir`
Inscribirse en asignatura con código (solo alumnos)

**Request Body:**
```json
{
  "codigo": "PRO2025-A1B2"
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "Inscripción exitosa",
    "asignatura": {
      "id": "...",
      "nombre": "Programación Móvil",
      ...
    }
  }
}
```

**Errores:**
- `400`: Código inválido o ya inscrito

---

#### `GET /api/alumnos/asignaturas`
Obtener asignaturas inscritas del alumno actual

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": "...",
      "nombre": "Programación Móvil",
      "codigoAcceso": "PRO2025-A1B2",
      ...
    }
  ]
}
```

---

#### `DELETE /api/alumnos/asignaturas/{asignaturaId}`
Darse de baja de una asignatura

**Response 200:**
```json
{
  "success": true,
  "data": null,
  "message": "Baja exitosa"
}
```

---

#### `GET /api/alumnos/asignaturas/{asignaturaId}/inscripciones`
Obtener lista de alumnos inscritos en una asignatura (para docentes)

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": "...",
      "alumnoId": "...",
      "asignaturaId": "...",
      "fechaInscripcion": "2025-12-10T10:00:00Z",
      "estado": "activo"
    }
  ]
}
```

---

### 📦 Storage

#### `POST /api/storage/upload`
Subir PDF a Supabase Storage

**Headers:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: Archivo PDF (máximo 50MB)
- `nombre`: Nombre del archivo

**Response 201:**
```json
{
  "success": true,
  "data": {
    "url": "https://xxxxx.supabase.co/storage/v1/object/public/clases/user-id/timestamp_nombre.pdf",
    "nombre": "nombre.pdf"
  },
  "message": "PDF subido exitosamente"
}
```

---

## 📊 Códigos de Estado HTTP

- `200 OK`: Operación exitosa
- `201 Created`: Recurso creado exitosamente
- `400 Bad Request`: Error en la petición (validación, datos inválidos)
- `401 Unauthorized`: No autenticado o token inválido
- `403 Forbidden`: No tienes permiso para esta operación
- `404 Not Found`: Recurso no encontrado
- `500 Internal Server Error`: Error interno del servidor

---

## 🔄 Formato de Respuesta

Todas las respuestas siguen este formato:

```json
{
  "success": true|false,
  "data": { ... } | null,
  "message": "Mensaje opcional",
  "error": "Mensaje de error (solo si success=false)"
}
```

---

## 🧪 Ejemplos de Uso

### Flujo Completo: Crear Asignatura y Clase

```bash
# 1. Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"docente@test.com","password":"pass123"}' \
  | jq -r '.data.token')

# 2. Crear asignatura
ASIGNATURA_ID=$(curl -X POST http://localhost:8080/api/asignaturas \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Programación Móvil","descripcion":"Curso Android"}' \
  | jq -r '.data.id')

# 3. Crear clase
curl -X POST http://localhost:8080/api/clases \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"nombre\":\"Introducción a Kotlin\",
    \"descripcion\":\"Fundamentos\",
    \"fecha\":\"Lunes 4 de Noviembre, 14:00hrs\",
    \"asignaturaId\":\"$ASIGNATURA_ID\"
  }"
```

---

**Última actualización:** Diciembre 2024

