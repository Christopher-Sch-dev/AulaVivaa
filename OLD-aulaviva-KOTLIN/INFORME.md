# INFORME TÉCNICO - AULAVIVA

**Proyecto:** Sistema Educativo Móvil con IA  
**Estudiante:** Christopher Schiefelbein  
**Institución:** Duoc UC - Ingeniería en Informática  
**Fecha:** Diciembre 2025

---

## 1. DESCRIPCIÓN DEL SISTEMA

Desarrollé AulaViva como una aplicación Android completa para gestión de clases con un asistente de Inteligencia Artificial integrado (Google Gemini). La idea es conectar a docentes y alumnos de manera fluida, permitiendo crear asignaturas, subir material en PDF, y usar IA para analizar y estudiar el contenido.

**Arquitecturas aplicadas:**
- **App Android:** MVVM + Clean Architecture
- **Backend:** Spring Boot + Clean Architecture (desplegado en Google Cloud Run)

---

## 2. TECNOLOGÍAS UTILIZADAS

### App Móvil (Frontend)

| Tecnología | Uso |
|------------|-----|
| Kotlin 2.1.0 | Lenguaje principal |
| Jetpack Compose | UI declarativa moderna |
| Material Design 3 | Sistema de diseño visual |
| Room 2.6.1 | Base de datos local (SQLite) |
| Retrofit 2.11.0 | Cliente HTTP REST |
| Kotlin Coroutines 1.9.0 | Programación asíncrona |
| Coil 2.5.0 | Carga de imágenes |
| PDFBox-Android 2.0.27 | Procesamiento de PDFs |
| Markwon 4.6.2 | Renderizado de Markdown |
| Google Gemini API | Inteligencia artificial |

### Backend (Microservicio)

| Tecnología | Uso |
|------------|-----|
| Spring Boot 3.2.0 | Framework REST API |
| Kotlin | Lenguaje del backend |
| PostgreSQL | Base de datos (Supabase) |
| Spring Security | Autenticación JWT |
| JPA/Hibernate | ORM para persistencia |
| Google Cloud Run | Hosting del servidor |

### Servicios Externos

- **Google Gemini 3 Flash Preview:** Modelo de IA generativa (con fallback a 2.5 Flash Lite)
- **Supabase:** PostgreSQL + Storage + Auth
- **Google Cloud Run:** Servidor backend en producción

---

## 3. INTERFAZ DE USUARIO (JETPACK COMPOSE)

Toda la UI está construida con **Jetpack Compose** usando un tema personalizado estilo "cyberpunk" que le da una identidad visual única a la app.

### Estructura de Pantallas

```
app/presentation/ui/
├── auth/          # Login, Register, Welcome
├── clases/        # Detalle de clase, Crear/Editar
├── common/        # Componentes reutilizables
├── components/    # Botones y elementos custom
├── effects/       # Efectos visuales especiales
├── ia/            # Pantalla de resultados IA
├── main/          # Paneles principales (Docente/Alumno)
├── navigation/    # Transiciones animadas
├── theme/         # Colores, tipografía, tema
└── utils/         # Utilidades de UI
```

### Componentes Visuales Destacados

| Componente | Descripción |
|------------|-------------|
| `MatrixBackground` | Fondo animado estilo "Matrix" con caracteres cayendo |
| `GlitchText` | Texto con efecto glitch/distorsión animado |
| `CyberLoading` | Spinner de carga con rotación y arcos |
| `AulaVivaBootScreen` | Pantalla de arranque estilo terminal con fade-in |
| `CyberComponents` | Botones con variantes (Primary, Secondary, Outline, Ghost) |
| `AulaVivaScreenFrame` | Frame común para todas las pantallas |
| `MarkdownText` | Renderiza respuestas de IA en formato Markdown |

### Animaciones Implementadas

Usé la API de animaciones de Compose para darle vida a la app:

- **`animateFloatAsState`** - Transiciones suaves de opacidad
- **`infiniteRepeatable`** - Animaciones en loop (loading spinners, glitch)
- **`tween`** - Curvas de animación personalizadas
- **`spring`** - Animaciones con física de resorte
- **`keyframes`** - Animaciones complejas por fotogramas
- **`BreakcoreTransitions`** - Transiciones de navegación personalizadas

### Sistema de Colores (Tema Cyberpunk)

La paleta está basada en tonos oscuros con acentos neón:
- **Primary:** Verde neón (#00FF41)
- **Background:** Negro profundo (#0D0D0D)
- **Surface:** Gris oscuro con transparencia
- **Accent:** Cyan/Magenta para efectos

---

## 4. ARQUITECTURA DEL SISTEMA

### Diagrama General

```
┌──────────────────┐     ┌─────────────────┐     ┌──────────────┐
│   App Android    │     │  Spring Boot    │     │   Supabase   │
│    (Kotlin)      │ HTTP│   REST API      │ JDBC│  PostgreSQL  │
│                  │◄────►│                 │◄────►│              │
│ Jetpack Compose  │     │  (Cloud Run)    │     │   Storage    │
│ Room + Retrofit  │     │   JWT Auth      │  API│     Auth     │
│  Gemini SDK      │◄────────────────────────────►│              │
└──────────────────┘     └─────────────────┘     └──────────────┘
```

### Capas de la App Android

**Presentation (UI):**
- Activities con Jetpack Compose
- ViewModels (StateFlow/LiveData)
- Screens y componentes reutilizables

**Domain (Lógica):**
- 13 UseCases para IA
- Interfaces de repositorio
- Modelos de dominio

**Data (Persistencia):**
- Repositorios (implementaciones)
- Room Database (DAOs, Entities)
- Retrofit API services

### Capas del Backend Spring Boot

**Presentation:**
- Controllers REST (AuthController, AsignaturaController, ClaseController, AlumnoController)

**Application:**
- Services (lógica de negocio)
- DTOs (Request/Response)

**Infrastructure:**
- Repositorios JPA
- Configuración (Security, CORS, Database)

**Domain:**
- Entidades (Usuario, Asignatura, Clase, AlumnoAsignatura)

---

## 5. BASE DE DATOS

### Tablas Principales (Supabase PostgreSQL)

**usuarios**
- id (UUID, PK)
- email (VARCHAR, UNIQUE)
- password_hash (VARCHAR)
- rol (VARCHAR: 'docente' o 'alumno')
- created_at (TIMESTAMP)

**asignaturas**
- id (UUID, PK)
- nombre (VARCHAR)
- descripcion (TEXT)
- codigo_unico (VARCHAR, UNIQUE) — ej: "POO2025-A1B2"
- docente_id (UUID, FK → usuarios)
- created_at (TIMESTAMP)

**clases**
- id (UUID, PK)
- nombre (VARCHAR)
- descripcion (TEXT)
- fecha (VARCHAR)
- archivo_pdf_url (VARCHAR)
- archivo_pdf_nombre (VARCHAR)
- asignatura_id (UUID, FK → asignaturas)
- created_at (TIMESTAMP)

**alumno_asignaturas** (tabla intermedia)
- alumno_id (UUID, FK → usuarios)
- asignatura_id (UUID, FK → asignaturas)
- fecha_inscripcion (TIMESTAMP)

### Room Database (Local - App Android)

**ChatSessionEntity** (chat temporal IA)
- id (INT, PK, autoincrement)
- claseNombre (VARCHAR)
- archivoNombre (VARCHAR)
- analisisPdf (TEXT)
- createdAt (LONG)

**ChatMessageEntity**
- id (INT, PK, autoincrement)
- sessionId (INT, FK → ChatSessionEntity)
- sender (VARCHAR: 'user' o 'ai')
- message (TEXT)
- createdAt (LONG)

---

## 6. API REST (Backend Spring Boot)

### Endpoints Principales

**Autenticación (/api/auth)**
```
POST /api/auth/register     # Registrar usuario
POST /api/auth/login        # Login (retorna JWT)
GET  /api/auth/me           # Usuario actual (requiere JWT)
```

**Asignaturas (/api/asignaturas)**
```
POST   /api/asignaturas                    # Crear (docente)
GET    /api/asignaturas                    # Listar (docente)
GET    /api/asignaturas/{id}               # Ver detalle
PUT    /api/asignaturas/{id}               # Actualizar
DELETE /api/asignaturas/{id}               # Eliminar
POST   /api/asignaturas/{id}/generar-codigo # Generar código inscripción
```

**Clases (/api/clases)**
```
POST   /api/clases           # Crear clase (docente)
GET    /api/clases           # Listar clases (query: asignaturaId)
GET    /api/clases/{id}      # Ver detalle
PUT    /api/clases/{id}      # Actualizar
DELETE /api/clases/{id}      # Eliminar
```

**Alumnos (/api/alumnos)**
```
POST   /api/alumnos/inscribir                      # Inscribirse con código
GET    /api/alumnos/asignaturas                    # Listar asignaturas inscritas
DELETE /api/alumnos/asignaturas/{id}               # Dar de baja
GET    /api/alumnos/asignaturas/{id}/inscripciones # Ver inscritos (docente)
```

**Storage (/api/storage)**
```
POST /api/storage/upload    # Subir PDF (multipart/form-data)
```

### Autenticación JWT

**Header requerido:**
```
Authorization: Bearer {token}
```

**Flujo:**
1. Usuario hace login → Backend valida con Supabase Auth
2. Backend genera JWT propio (HS256, 24h validez)
3. App guarda token en SharedPreferences
4. Todas las llamadas siguientes incluyen el token

---

## 7. INTELIGENCIA ARTIFICIAL (Google Gemini)

### Modelo Utilizado

| Campo | Valor |
|-------|-------|
| **Nombre Primario** | gemini-3-flash-preview |
| **Fallback** | gemini-2.5-flash-lite |
| **Proveedor** | Google AI |
| **Tipo** | LLM multimodal (texto) |
| **Contexto máx** | ~30,000 caracteres |

### Técnica RAG (Retrieval-Augmented Generation)

**Flujo:**
1. Usuario sube PDF de clase → Guardado en Supabase Storage
2. App descarga PDF a caché local
3. **Extracción:** PDFBox extrae texto completo + metadata (título, autor)
4. **Chunking:** Texto limitado a 30k chars para evitar límites de contexto
5. **Prompt engineering:** Se inyecta texto del PDF + historial + instrucciones
6. **Generación:** Gemini genera respuesta contextualizada
7. **Persistencia:** Respuesta guardada en Room (ChatMessageEntity)

### Funciones IA Implementadas

**Para Docentes:**
- Generar ideas para clase
- Sugerir actividades interactivas
- Estructurar clase por bloques de tiempo
- Generar guía de presentación
- Crear ejercicios con soluciones
- Resumir contenido del PDF
- Generar preguntas tipo test

**Para Alumnos:**
- Explicar conceptos simplificados
- Generar resumen de estudio
- Crear ejercicios prácticos

**Adicionales:**
- Chat contextual temporal (máx 3 mensajes)
- Reanálisis de PDF bajo demanda
- Cierre automático de sesión (onStop)

---

## 8. DESPLIEGUE EN PRODUCCIÓN

### Google Cloud Run

El backend está desplegado en **Google Cloud Run** como contenedor Docker:

- **URL:** `https://aulaviva-backend-620174961947.southamerica-west1.run.app`
- **Región:** southamerica-west1 (Santiago, Chile)
- **Escalado:** Automático (0 a N instancias)
- **Dockerfile:** Multi-stage build con Gradle + Java 17

### Supabase

- **PostgreSQL:** Base de datos relacional
- **Storage:** Almacenamiento de PDFs
- **Auth:** Autenticación de usuarios (backup)
- **Región:** South America

---

## 9. SEGURIDAD

### App Android

- **ProGuard/R8:** Ofuscación de código en release
- **JWT Storage:** Token almacenado en SharedPreferences (cifrado del sistema)
- **HTTPS:** Comunicación encriptada con backend
- **Permisos mínimos:** INTERNET, POST_NOTIFICATIONS, READ/WRITE_EXTERNAL_STORAGE

### Backend Spring Boot

- **Spring Security:** Protección de endpoints por rol
- **JWT:** HS256, secret de 256+ bits, expiración 24h
- **CORS:** Configurado para permitir requests desde Android
- **Input validation:** DTOs validados con Jakarta Validation

### Supabase

- **RLS (Row Level Security):** Políticas de acceso por usuario/rol
- **Password hashing:** bcrypt
- **API Keys:** anon key (público), service_role key (privado)

---

## 10. PRUEBAS UNITARIAS

### Tests Implementados (32 tests)

| Archivo | Qué prueba |
|---------|-----------|
| AuthViewModelTest.kt | Autenticación y registro |
| PanelPrincipalViewModelTest.kt | ViewModel principal |
| RepositoryTest.kt | Repositorios de datos |
| ValidacionesUtilsTest.kt | Funciones de validación |
| AsignaturasValidacionTest.kt | Validación de asignaturas |
| ClaseValidacionTest.kt | Validación de clases |
| SpringBootClientTest.kt | Cliente HTTP |
| SessionPersistenceTest.kt | Persistencia de sesión |
| AlumnoRepositoryDuplicateKeyTest.kt | Manejo de duplicados |
| MarkdownPreprocessingTest.kt | Procesamiento de markdown |

**Ejecutar tests:**
```bash
./gradlew testDebugUnitTest
```

---

## 11. APK FIRMADO

### Ubicación

- **APK Release:** `app/build/outputs/apk/release/app-release.apk`
- **Keystore:** `app/aulaviva-release.jks`

### Generar APK

```bash
$env:JAVA_HOME="C:\Users\Chris\.jdks\ms-17.0.16"
./gradlew clean assembleRelease
```

---

## 12. RECURSOS NATIVOS DEL DISPOSITIVO

### 1. Sistema de Notificaciones

- Usa `NotificationManager` y `NotificationChannel`
- Notificaciones push para: bienvenida, clases creadas, recordatorios
- Permiso `POST_NOTIFICATIONS` (Android 13+)
- Implementación: `utils/NotificationHelper.kt`

### 2. Almacenamiento Externo

- Permisos `READ_EXTERNAL_STORAGE` y `WRITE_EXTERNAL_STORAGE` para cache de PDFs
- Intent para abrir PDFs con visor externo

---

## 13. CONCLUSIONES

### Objetivos Cumplidos:
- Sistema funcional docente/alumno con CRUD completo
- Integración IA generativa (Gemini) con contexto de PDFs
- Backend REST API con autenticación JWT desplegado en Cloud Run
- Persistencia offline-first con sincronización
- Arquitectura limpia y mantenible (MVVM + Clean)
- UI moderna con Jetpack Compose y animaciones

### Principales Desafíos Resueltos:
- Extracción confiable de PDFs (PDFBox fallback)
- Chat stateful con persistencia temporal
- Sincronización Room ↔ Backend
- Metadata automática de PDFs
- Seguridad JWT end-to-end
- ProGuard rules para evitar ofuscación de DTOs

### Aprendizajes Clave:
- Clean Architecture facilita testing y escalabilidad
- RAG básico es efectivo para contexto educativo
- Offline-first mejora UX en conexiones inestables
- Spring Boot simplifica desarrollo de APIs REST
- Jetpack Compose hace la UI más declarativa y mantenible
- Google Cloud Run es excelente para microservicios con escalado automático

---

**Desarrollado por Christopher Schiefelbein - Diciembre 2025**