# 🚀 AulaViva Backend - Spring Boot API

Backend REST API desarrollado con Spring Boot y Kotlin para la aplicación AulaViva.

## 📋 Características

- ✅ **Spring Boot 3.2.0** con Kotlin
- ✅ **JPA/Hibernate** para acceso a datos
- ✅ **PostgreSQL** (conectado a Supabase)
- ✅ **JWT** para autenticación
- ✅ **CORS** configurado para Android
- ✅ **Validación** de requests
- ✅ **Clean Architecture** (Domain, Application, Infrastructure, Presentation)

## 🏗️ Arquitectura

```
backend/
├── domain/              # Entidades de dominio
├── application/         # Casos de uso y servicios
│   ├── dto/           # DTOs de request/response
│   └── service/       # Servicios de negocio
├── infrastructure/     # Implementaciones técnicas
│   ├── config/        # Configuraciones
│   ├── repository/    # Repositorios JPA
│   └── security/      # Seguridad y JWT
└── presentation/       # Controladores REST
```

## ⚙️ Configuración

### 1. Variables de Entorno

Crea un archivo `.env` o configura las variables en `application.yml`:

```properties
# Supabase Database
SUPABASE_DB_HOST=db.xxxxx.supabase.co
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=tu-password

# Supabase API
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=tu-anon-key
SUPABASE_SERVICE_ROLE_KEY=tu-service-role-key

# JWT
JWT_SECRET=tu-secret-key-minimo-256-bits-cambiar-en-produccion
```

**Cómo obtener las credenciales de Supabase:**

1. Ve a tu proyecto en Supabase Dashboard
2. **Settings → Database** → Copia:
   - Host → `SUPABASE_DB_HOST`
   - Database name → `SUPABASE_DB_NAME`
   - Port → `SUPABASE_DB_PORT`
   - Password → `SUPABASE_DB_PASSWORD` (la que configuraste al crear el proyecto)
3. **Settings → API** → Copia:
   - Project URL → `SUPABASE_URL`
   - anon public → `SUPABASE_ANON_KEY`
   - service_role → `SUPABASE_SERVICE_ROLE_KEY`

### 2. Ejecutar el Backend

```bash
# Desde la raíz del proyecto
cd backend
./gradlew bootRun

# O desde la raíz
./gradlew :backend:bootRun
```

El servidor se iniciará en `http://localhost:8080`

## 📡 Endpoints REST

### Autenticación

#### `POST /api/auth/register`
Registrar nuevo usuario

**Request:**
```json
{
  "email": "usuario@example.com",
  "password": "password123",
  "rol": "docente"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "uuid-del-usuario",
    "email": "usuario@example.com",
    "rol": "docente"
  },
  "message": "Usuario registrado exitosamente"
}
```

#### `POST /api/auth/login`
Iniciar sesión

**Request:**
```json
{
  "email": "usuario@example.com",
  "password": "password123"
}
```

#### `GET /api/auth/me`
Obtener usuario actual (requiere token JWT)

**Headers:**
```
Authorization: Bearer {token}
```

---

### Asignaturas

#### `POST /api/asignaturas`
Crear asignatura (requiere autenticación, rol: docente)

**Headers:**
```
Authorization: Bearer {token}
```

**Request:**
```json
{
  "nombre": "Programación Móvil",
  "descripcion": "Curso de desarrollo Android"
}
```

#### `GET /api/asignaturas`
Obtener asignaturas del docente actual

#### `GET /api/asignaturas/{id}`
Obtener asignatura por ID

#### `PUT /api/asignaturas/{id}`
Actualizar asignatura

#### `DELETE /api/asignaturas/{id}`
Eliminar asignatura

#### `POST /api/asignaturas/{id}/generar-codigo`
Generar código único para asignatura

---

### Clases

#### `POST /api/clases`
Crear clase (requiere autenticación, rol: docente)

**Request:**
```json
{
  "nombre": "Introducción a Kotlin",
  "descripcion": "Fundamentos del lenguaje",
  "fecha": "Lunes 4 de Noviembre, 14:00hrs",
  "archivoPdfUrl": "",
  "archivoPdfNombre": "",
  "asignaturaId": "uuid-de-asignatura"
}
```

#### `GET /api/clases`
Obtener clases del docente actual

**Query params:**
- `asignaturaId` (opcional): Filtrar por asignatura

#### `GET /api/clases/{id}`
Obtener clase por ID

#### `PUT /api/clases/{id}`
Actualizar clase

#### `DELETE /api/clases/{id}`
Eliminar clase

---

### Alumnos

#### `POST /api/alumnos/inscribir`
Inscribirse en asignatura con código (requiere autenticación, rol: alumno)

**Request:**
```json
{
  "codigo": "POO2025-A1B2"
}
```

#### `GET /api/alumnos/asignaturas`
Obtener asignaturas inscritas del alumno actual

#### `DELETE /api/alumnos/asignaturas/{asignaturaId}`
Darse de baja de una asignatura

#### `GET /api/alumnos/asignaturas/{asignaturaId}/inscripciones`
Obtener inscripciones de una asignatura (para docentes)

---

### Storage

#### `POST /api/storage/upload`
Subir PDF (requiere autenticación)

**Headers:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Form Data:**
- `file`: Archivo PDF
- `nombre`: Nombre del archivo

**Response:**
```json
{
  "success": true,
  "data": {
    "url": "https://xxxxx.supabase.co/storage/v1/object/public/clases/...",
    "nombre": "archivo.pdf"
  }
}
```

---

## 🔐 Autenticación

Todos los endpoints (excepto `/api/auth/**`) requieren autenticación JWT.

**Formato del header:**
```
Authorization: Bearer {token}
```

El token se obtiene al hacer login o registro.

---

## 🧪 Probar los Endpoints

### Usando cURL

```bash
# 1. Registrar usuario
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "docente@test.com",
    "password": "password123",
    "rol": "docente"
  }'

# 2. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "docente@test.com",
    "password": "password123"
  }'

# 3. Obtener asignaturas (con token)
curl -X GET http://localhost:8080/api/asignaturas \
  -H "Authorization: Bearer {token}"
```

### Usando Postman

1. Importa la colección de Postman (si está disponible)
2. Configura la variable `baseUrl` = `http://localhost:8080`
3. Haz login y copia el token
4. Configura la variable `token` con el JWT obtenido
5. Prueba los demás endpoints

---

## 🐛 Solución de Problemas

### Error: "Connection refused"
- Verifica que el servidor esté corriendo en el puerto 8080
- Verifica que no haya otro proceso usando el puerto

### Error: "Unable to connect to database"
- Verifica las credenciales de Supabase en `application.yml`
- Verifica que la IP de tu máquina esté en la whitelist de Supabase (Settings → Database → Connection Pooling)

### Error: "Invalid JWT token"
- Verifica que el token no haya expirado (24 horas)
- Verifica que estés enviando el header `Authorization: Bearer {token}`

### Error: "Access denied"
- Verifica que el usuario tenga el rol correcto para la operación
- Verifica que el token sea válido

---

## 📝 Notas Importantes

1. **Base de Datos**: Spring Boot se conecta directamente a PostgreSQL de Supabase usando JDBC
2. **Autenticación**: Usa Supabase Auth para registro/login, pero genera JWT propio para las APIs
3. **Storage**: Los PDFs se suben a Supabase Storage usando la API de Supabase
4. **CORS**: Configurado para permitir requests desde la app Android
5. **Seguridad**: Todas las operaciones validan permisos según el rol del usuario

---

## 🚀 Despliegue

Para producción:

1. Configura variables de entorno en el servidor
2. Cambia `JWT_SECRET` por una clave segura (mínimo 256 bits)
3. Configura HTTPS
4. Ajusta `CORS` para permitir solo tu dominio de producción
5. Configura logs apropiados

---

**Última actualización:** Diciembre 2024

