# 🎓 AulaViva - Gestión Educativa Móvil

**Aplicación móvil para gestión de clases y asignaturas desarrollada con Kotlin, Jetpack Compose y Spring Boot.**

---

## 📱 Información del Proyecto

| Campo | Detalle |
|-------|---------|
| **Nombre de la App** | AulaViva |
| **Desarrollador** | Christopher Schiefelbein |
| **Asignatura** | DSY1105 - Desarrollo de Aplicaciones Móviles |
| **Institución** | DuocUC |
| **Versión** | 1.1 |

---

## 🎯 Descripción

AulaViva es una aplicación móvil educativa que permite a docentes y alumnos gestionar asignaturas y clases de manera eficiente. La app cuenta con un sistema de roles diferenciado, almacenamiento local y remoto, integración con IA (Gemini) para análisis de contenido, y un backend propio desarrollado con Spring Boot.

### ¿Qué problema resuelve?

En el contexto educativo actual, tanto profesores como estudiantes necesitan una herramienta centralizada para:
- **Docentes**: Crear y gestionar asignaturas, programar clases, compartir material PDF, y ver alumnos inscritos
- **Alumnos**: Inscribirse en asignaturas con código, acceder al material de clases, y usar IA para estudiar

---

## 🚀 Funcionalidades Principales

### Para Docentes
- ✅ Crear, editar y eliminar asignaturas
- ✅ Generar códigos únicos para que alumnos se inscriban
- ✅ Crear clases con descripción y materiales PDF
- ✅ Ver lista de alumnos inscritos por asignatura
- ✅ Subir archivos PDF a la nube (Supabase Storage)

### Para Alumnos
- ✅ Inscribirse en asignaturas usando código del docente
- ✅ Ver clases de las asignaturas inscritas
- ✅ Descargar y visualizar material PDF
- ✅ Usar Gemini AI para analizar contenido de clases
- ✅ Darse de baja de asignaturas

### Sistema de Autenticación
- ✅ Registro de usuarios con selección de rol (docente/alumno)
- ✅ Login con JWT
- ✅ Recuperación de contraseña
- ✅ Sesión persistente (se mantiene al cerrar la app)
- ✅ Modificación de perfil

---

## 🏗️ Arquitectura Técnica

### Frontend (App Android)
```
app/
├── data/               # Capa de datos
│   ├── local/         # Room Database (offline-first)
│   └── remote/        # Retrofit + Spring Boot API
├── domain/            # Modelos de dominio
├── presentation/      # UI Layer
│   ├── viewmodel/     # ViewModels (MVVM)
│   ├── ui/           # Compose Screens
│   └── activity/     # Activities
└── utils/            # Helpers (notificaciones, PDF, etc.)
```

### Backend (Spring Boot)
```
backend/
├── domain/            # Entidades de negocio
├── application/       # Servicios y DTOs
├── infrastructure/    # Repositorios JPA, seguridad
└── presentation/      # Controladores REST
```

### Patrones Implementados
- **MVVM** (Model-View-ViewModel) con LiveData/StateFlow
- **Repository Pattern** para abstracción de datos
- **Clean Architecture** en backend (4 capas)
- **Offline-First** con Room + sincronización remota

---

## 📡 Endpoints del Backend (Microservicios)

### Autenticación
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/register` | Registrar usuario |
| POST | `/api/auth/login` | Iniciar sesión |
| GET | `/api/auth/me` | Obtener usuario actual |

### Asignaturas (CRUD)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/asignaturas` | Crear asignatura |
| GET | `/api/asignaturas` | Obtener asignaturas del docente |
| GET | `/api/asignaturas/{id}` | Obtener por ID |
| PUT | `/api/asignaturas/{id}` | Actualizar |
| DELETE | `/api/asignaturas/{id}` | Eliminar |
| POST | `/api/asignaturas/{id}/generar-codigo` | Generar código único |

### Clases (CRUD)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/clases` | Crear clase |
| GET | `/api/clases` | Obtener clases |
| GET | `/api/clases/{id}` | Obtener por ID |
| PUT | `/api/clases/{id}` | Actualizar |
| DELETE | `/api/clases/{id}` | Eliminar |

### Alumnos
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/alumnos/inscribir` | Inscribirse con código |
| GET | `/api/alumnos/asignaturas` | Ver asignaturas inscritas |
| DELETE | `/api/alumnos/asignaturas/{id}` | Darse de baja |

### Storage
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/storage/upload` | Subir PDF |

---

## 🔌 API Externa Integrada

**Google Gemini AI** - Usada para análisis inteligente de contenido educativo.

- **Modelo primario**: `gemini-3-flash-preview`
- **Modelo fallback**: `gemini-2.5-flash-lite`
- **Uso**: El alumno puede pedir a la IA que analice, resuma o explique el contenido de una clase

La integración se hace via Retrofit a la API de Google Generative AI.

---

## 📱 Recursos Nativos del Dispositivo

### 1. Sistema de Notificaciones
- Usa `NotificationManager` y `NotificationChannel`
- Notificaciones push para: bienvenida, clases creadas, recordatorios
- Permiso `POST_NOTIFICATIONS` (Android 13+)
- Ver: `app/src/main/java/cl/duocuc/aulaviva/utils/NotificationHelper.kt`

### 2. Almacenamiento Externo
- Permisos `READ_EXTERNAL_STORAGE` y `WRITE_EXTERNAL_STORAGE` para cache de PDFs
- Queries para intent de visor PDF externo
- Ver: `AndroidManifest.xml` líneas 8-22

---

## 🧪 Pruebas Unitarias

El proyecto incluye **32 tests unitarios** distribuidos en:

| Archivo | Qué prueba |
|---------|-----------|
| `AuthViewModelTest.kt` | Autenticación y registro |
| `PanelPrincipalViewModelTest.kt` | ViewModel principal |
| `RepositoryTest.kt` | Repositorios de datos |
| `ValidacionesUtilsTest.kt` | Funciones de validación |
| `AsignaturasValidacionTest.kt` | Validación de asignaturas |
| `ClaseValidacionTest.kt` | Validación de clases |
| `SpringBootClientTest.kt` | Cliente HTTP |
| `SessionPersistenceTest.kt` | Persistencia de sesión |
| `AlumnoRepositoryDuplicateKeyTest.kt` | Manejo de duplicados |
| `MarkdownPreprocessingTest.kt` | Procesamiento de markdown |

### Ejecutar Tests
```bash
cd app
./gradlew test
```

---

## 📦 APK Firmado

### Ubicación
- **APK Release**: `app/build/outputs/apk/release/app-release.apk`
- **Keystore**: `app/aulaviva-release.jks`
- **Configuración**: `keystore.properties` (no subido por seguridad)

### Generar APK
```bash
./gradlew assembleRelease
```

---

## ⚙️ Instrucciones de Instalación

### Prerrequisitos
- Android Studio Hedgehog o superior
- JDK 17+
- Kotlin 2.1.0
- Gradle 8.x

### 1. Clonar el repositorio
```bash
git clone https://github.com/[usuario]/AulaViva.git
cd AulaViva
```

### 2. Configurar `local.properties`
```properties
# API Keys
GEMINI_API_KEY=tu-api-key-de-gemini
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_ANON_KEY=tu-anon-key

# Backend (para desarrollo local)
SPRING_BOOT_URL=http://10.0.2.2:8080/

# Para producción usar:
# SPRING_BOOT_URL=https://aulaviva-backend-620174961947.southamerica-west1.run.app/
```

### 3. Configurar `keystore.properties` (para APK release)
```properties
KEYSTORE_FILE=aulaviva-release.jks
KEYSTORE_PASSWORD=tu-password
KEY_ALIAS=aulaviva-key
KEY_PASSWORD=tu-password
```

### 4. Ejecutar Backend (opcional para desarrollo)
```bash
cd backend
./gradlew bootRun
```

### 5. Ejecutar la App
- Abrir proyecto en Android Studio
- Sync Gradle
- Run en emulador o dispositivo físico

---

## 🔧 Stack Tecnológico

### App Móvil
- **Lenguaje**: Kotlin 2.1.0
- **UI**: Jetpack Compose + Material 3
- **Arquitectura**: MVVM
- **Base de datos local**: Room 2.6.1
- **HTTP Client**: Retrofit 2.11.0
- **Imágenes**: Coil
- **Corrutinas**: Kotlin Coroutines 1.9.0
- **PDF**: PDFBox Android

### Backend
- **Framework**: Spring Boot 3.2.0
- **Lenguaje**: Kotlin
- **ORM**: JPA/Hibernate
- **Base de datos**: PostgreSQL (Supabase)
- **Auth**: JWT
- **Deploy**: Google Cloud Run

---

## 📂 Estructura del Repositorio

```
AulaViva/
├── app/                        # Código de la app Android
│   ├── src/main/              # Código fuente
│   ├── src/test/              # Tests unitarios
│   ├── build.gradle.kts       # Configuración del módulo
│   └── aulaviva-release.jks   # Keystore para firma
├── backend/                    # Código del backend Spring Boot
│   ├── src/                   # Código fuente
│   ├── build.gradle.kts       # Configuración
│   └── README.md              # Documentación del backend
├── database/                   # Scripts SQL (si aplica)
├── build.gradle.kts           # Configuración raíz
├── settings.gradle.kts        # Settings de Gradle
└── README.md                  # Este archivo
```

---

## 👤 Información del Autor

**Christopher Schiefelbein**
- GitHub: Evidencia en commits del repositorio
- Rol: Desarrollador Full Stack (trabajé solo en este proyecto)

---

## 📝 Notas Finales

Este proyecto fue desarrollado completamente por mí durante el semestre. Representó un desafío grande integrar todas las tecnologías (Compose, Room, Retrofit, Spring Boot, Supabase, Gemini AI), pero estoy satisfecho con el resultado final.

La app está funcionando en producción con el backend desplegado en Google Cloud Run. El diseño visual tiene una estética "cyberpunk" que quise darle un toque único.

Cualquier duda sobre la implementación, estaré preparado para explicarla en la defensa técnica.

---

**Última actualización:** Diciembre 2024
