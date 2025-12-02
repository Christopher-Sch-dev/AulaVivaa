# 🎓 AULAVIVA - Sistema Educativo Móvil con IA

> Aplicación Android para docentes y alumnos con asistente de IA generativa (Google Gemini 2.5 Pro)

[![Android](https://img.shields.io/badge/Android-SDK%2024--36-green)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple)]()
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot%203.2-brightgreen)]()
[![Supabase](https://img.shields.io/badge/Database-Supabase%20PostgreSQL-blue)]()
[![Gemini](https://img.shields.io/badge/IA-Gemini%202.5%20Pro-orange)]()

---

## 📋 INFORMACIÓN DEL PROYECTO

### Nombre del Proyecto
**AULAVIVA**

### Integrante
**Christopher Schiefelbein**
- Email: ch.sch.informatico@gmail.com
- GitHub: [@Ch-sch-xxx](https://github.com/Ch-sch-xxx)
- LinkedIn: [christopher-schiefelbein](https://www.linkedin.com/in/christopher-schiefelbein-8292b4160/)

### Contexto Académico
- **Institución:** Duoc UC
- **Carrera:** Ingeniería en Informática
- **Asignatura:** Desarrollo de Aplicaciones Móviles
- **Semestre:** 4º (2024-2025)
- **Fecha:** Diciembre 2024

---

## 📱 ¿QUÉ ES AULAVIVA?

Sistema educativo móvil Android que conecta docentes y alumnos mediante inteligencia artificial para mejorar la enseñanza y comprensión de contenidos académicos.

### Problema que Resuelve
- **Alumnos:** Dificultad para comprender contenido de clases (actuales o pasadas), necesitan reforzamiento personalizado
- **Docentes:** Requieren herramientas para estructurar clases, generar material educativo y organizar contenidos

### Solución Implementada
- **Para Alumnos:** Asistente IA que explica conceptos, genera resúmenes y crea ejercicios personalizados basados en PDFs de clase
- **Para Docentes:** Asistente IA que ayuda a planificar clases, generar actividades interactivas, guías y evaluar contenido pedagógico


---

## ✨ FUNCIONALIDADES DEL SISTEMA

### 🔐 Autenticación y Roles
- **Registro de usuarios** (Docente / Alumno)
- **Login seguro** con JWT (generado por Spring Boot backend)
- **Persistencia de sesión** (token almacenado localmente en Room)
- **Cierre de sesión** (limpieza de token y datos locales)

### 👨‍🏫 Funcionalidades para DOCENTES

#### Gestión de Asignaturas
- ✅ **Crear asignaturas** (nombre, descripción)
- ✅ **Editar asignaturas** existentes
- ✅ **Eliminar asignaturas** (con confirmación)
- ✅ **Generar código único** para inscripción de alumnos (formato: `POO2025-A1B2`)
- ✅ **Ver lista de alumnos inscritos** por asignatura

#### Gestión de Clases
- ✅ **Crear clases** (nombre, descripción, fecha, asignatura asociada)
- ✅ **Subir PDFs** de material de clase (vía Supabase Storage)
- ✅ **Editar clases** existentes
- ✅ **Eliminar clases** (con confirmación)
- ✅ **Visualizar listado** de clases por asignatura

#### Asistente IA para Docentes (Gemini 2.5 Pro)
- 🤖 **Generar ideas para clases** basadas en PDF analizado
- 🤖 **Sugerir actividades interactivas** según el contenido
- 🤖 **Estructurar clase** (objetivos, desarrollo, evaluación)
- 🤖 **Generar guía de presentación** (diapositivas estructuradas)
- 🤖 **Crear ejercicios** con soluciones para alumnos
- 🤖 **Chat contextual temporal** (mantiene contexto del PDF mientras está abierto, máx 3 mensajes)
- 🤖 **Reanálisis de PDF** bajo demanda (actualiza análisis y contexto)
- 📄 **Extracción automática de metadatos** del PDF (título, autor, resumen)
- ✨ **Respuestas estructuradas** con detalles del PDF analizado + resultado + tagline "Este fue gemini real bro 🚬😶‍🌫️"

### 👨‍🎓 Funcionalidades para ALUMNOS

#### Gestión de Inscripciones
- ✅ **Inscribirse a asignatura** mediante código único
- ✅ **Ver asignaturas inscritas**
- ✅ **Darse de baja** de asignaturas

#### Acceso a Clases y Material
- ✅ **Ver clases disponibles** de asignaturas inscritas
- ✅ **Descargar PDFs** de material de clase
- ✅ **Visualizar detalles** de cada clase (nombre, descripción, fecha)

#### Asistente IA para Alumnos (Gemini 2.5 Pro)
- 🤖 **Explicar conceptos** del PDF de forma simplificada
- 🤖 **Generar resumen de estudio** personalizado
- 🤖 **Crear ejercicios prácticos** para reforzar aprendizaje
- 🤖 **Chat contextual temporal** (mantiene contexto del PDF mientras está abierto)
- 🤖 **Reanálisis de PDF** si el alumno necesita profundizar
- 📄 **Extracción automática de metadatos** del PDF (título, autor, resumen)
- ✨ **Respuestas estructuradas** con contexto completo del documento

### 🔄 Funcionalidades Técnicas
- 💾 **Persistencia offline-first** con Room Database (SQLite)
- 🌐 **Sincronización** con backend Spring Boot REST API
- 🔒 **Seguridad:** JWT, ProGuard (ofuscación), HTTPS, Spring Security
- 📊 **Base de datos relacional** con Supabase PostgreSQL
- 📁 **Almacenamiento de archivos** con Supabase Storage
- 🎨 **Material Design 3** con tema personalizado
- ⚡ **Manejo asíncrono** con Kotlin Coroutines y Flow
- 🧪 **Tests unitarios** incluidos (domain/usecases)

---

## 🏗️ ARQUITECTURA DEL SISTEMA

### Stack Tecnológico Completo

#### **Frontend (App Android)**
- **Lenguaje:** Kotlin 1.9+
- **Arquitectura:** MVVM + Clean Architecture (Domain, Data, Presentation)
- **UI:** Material Design 3, ViewBinding, RecyclerView
- **Persistencia local:** Room Database (SQLite)
- **Networking:** Retrofit 2 + OkHttp 4
- **Asincronía:** Kotlin Coroutines + Flow
- **DI:** Service Locator (RepositoryProvider) — preparado para Hilt
- **PDF processing:** PDFBox-Android 2.0.27
- **Seguridad:** ProGuard (ofuscación release)

#### **Backend (Microservicio REST API)**
- **Framework:** Spring Boot 3.2.0 (Kotlin)
- **Base de datos:** Supabase PostgreSQL 15+ (JDBC directo)
- **Autenticación:** Supabase Auth + JWT propio (HS256)
- **Storage:** Supabase Storage (proxy vía Spring Boot)
- **Seguridad:** Spring Security + CORS configurado
- **Arquitectura:** Clean Architecture (Domain, Application, Infrastructure, Presentation)

#### **Inteligencia Artificial**
- **Modelo:** Google Gemini 2.5 Pro (`gemini-2.0-flash-exp` preview)
- **SDK:** `google-generativeai:0.8.0`
- **Técnica:** RAG básico (Retrieval-Augmented Generation)
  - Extracción de texto completo del PDF (PDFBox)
  - Chunking de contexto (límite 30,000 caracteres)
  - Inyección de contexto en prompts
  - Chat stateful con historial persistente (Room)
- **Funciones:** 10+ funciones especializadas (ideas, actividades, explicaciones, ejercicios, guías, etc.)

### Diagrama de Flujo

```
┌─────────────────┐          ┌──────────────────┐          ┌─────────────────┐
│   App Android   │  HTTP    │  Spring Boot     │  JDBC    │    Supabase     │
│   (Kotlin)      │ ◄─────► │  REST API        │ ◄─────► │   PostgreSQL    │
│                 │  REST    │  (Kotlin)        │          │                 │
│  - Room DB      │          │                  │          │  - Auth         │
│  - Retrofit     │          │  - JWT           │  API     │  - Storage      │
│  - Gemini SDK   │ ◄────────────────────────────────────► │  - Database     │
└─────────────────┘          └──────────────────┘          └─────────────────┘
        │                              │
        │  Gemini API (AI)             │
        └──────────────────────────────┘
               Google Cloud
```

---

## 🔌 ENDPOINTS Y APIS UTILIZADAS

### 1️⃣ API Externa: Google Gemini 2.5 Pro

**URL Base:** `https://generativelanguage.googleapis.com/`

**Autenticación:** API Key (header `x-goog-api-key`)

**Endpoints Consumidos:**
- `POST /v1beta/models/gemini-2.0-flash-exp:generateContent`
  - Generación de texto con contexto inyectado
  - Streaming opcional (no usado actualmente)

**Uso en la App:**
- Llamadas directas desde `IARepository.kt`
- Contexto inyectado: texto del PDF + historial de chat + prompt específico
- Límite de tokens: ~30,000 caracteres de contexto

### 2️⃣ Microservicio Backend: Spring Boot REST API

**URL Base (Desarrollo):** `http://10.0.2.2:8080/` (emulador) o `http://192.168.1.X:8080/` (dispositivo físico)

**URL Base (Producción):** `https://tu-servidor.com/` (Railway, Heroku, AWS, etc.)

#### **Autenticación** (`/api/auth`)

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Registrar nuevo usuario | No |
| POST | `/api/auth/login` | Iniciar sesión | No |
| GET | `/api/auth/me` | Obtener usuario actual | JWT |

**Request Login:**
```json
{
  "email": "docente@test.com",
  "password": "password123"
}
```

**Response Login:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "uuid-del-usuario",
    "email": "docente@test.com",
    "rol": "docente"
  },
  "message": "Login exitoso"
}
```

#### **Asignaturas** (`/api/asignaturas`)

| Método | Endpoint | Descripción | Rol Requerido |
|--------|----------|-------------|---------------|
| POST | `/api/asignaturas` | Crear asignatura | Docente |
| GET | `/api/asignaturas` | Listar asignaturas del docente | Docente |
| GET | `/api/asignaturas/{id}` | Obtener asignatura por ID | Docente |
| PUT | `/api/asignaturas/{id}` | Actualizar asignatura | Docente |
| DELETE | `/api/asignaturas/{id}` | Eliminar asignatura | Docente |
| POST | `/api/asignaturas/{id}/generar-codigo` | Generar código único | Docente |

#### **Clases** (`/api/clases`)

| Método | Endpoint | Descripción | Rol Requerido |
|--------|----------|-------------|---------------|
| POST | `/api/clases` | Crear clase | Docente |
| GET | `/api/clases` | Listar clases (query: `asignaturaId`) | Docente/Alumno |
| GET | `/api/clases/{id}` | Obtener clase por ID | Docente/Alumno |
| PUT | `/api/clases/{id}` | Actualizar clase | Docente |
| DELETE | `/api/clases/{id}` | Eliminar clase | Docente |

#### **Alumnos** (`/api/alumnos`)

| Método | Endpoint | Descripción | Rol Requerido |
|--------|----------|-------------|---------------|
| POST | `/api/alumnos/inscribir` | Inscribirse con código | Alumno |
| GET | `/api/alumnos/asignaturas` | Listar asignaturas inscritas | Alumno |
| DELETE | `/api/alumnos/asignaturas/{id}` | Darse de baja | Alumno |
| GET | `/api/alumnos/asignaturas/{id}/inscripciones` | Ver inscritos (docente) | Docente |

#### **Storage** (`/api/storage`)

| Método | Endpoint | Descripción | Rol Requerido |
|--------|----------|-------------|---------------|
| POST | `/api/storage/upload` | Subir PDF | Docente/Alumno |

**Request Upload:**
- `Content-Type: multipart/form-data`
- `file`: archivo PDF
- `nombre`: nombre del archivo

**Response Upload:**
```json
{
  "success": true,
  "data": {
    "url": "https://xxxxx.supabase.co/storage/v1/object/public/clases/...",
    "nombre": "clase1.pdf"
  }
}
```

### 3️⃣ Supabase Services

**Base de Datos:** PostgreSQL (accedido vía JDBC desde Spring Boot)
- Host: `db.xxxxx.supabase.co:5432`
- Database: `postgres`

**Storage:** Supabase Storage (accedido vía API REST desde Spring Boot)
- URL: `https://xxxxx.supabase.co/storage/v1`
- Bucket: `clases` (público)

**Auth:** Supabase Auth (validación de credenciales desde Spring Boot)
- URL: `https://xxxxx.supabase.co/auth/v1`

---

## 🚀 PASOS PARA EJECUTAR EL PROYECTO


---

## 🚀 PASOS PARA EJECUTAR EL PROYECTO

### Requisitos Previos

#### Software Necesario
- **Android Studio** Hedgehog+ (2023.1.1 o superior)
- **JDK** 17 o superior (para Spring Boot backend)
- **Gradle** 8.0+ (incluido en el proyecto)
- **Git** para clonar el repositorio

#### Cuentas y Credenciales
- **Cuenta Supabase** (base de datos + storage + auth)
  - Crear proyecto gratuito en [supabase.com](https://supabase.com)
- **Google Gemini API Key** (IA generativa)
  - Obtener en [ai.google.dev](https://ai.google.dev)

---

### PASO 1: Clonar el Repositorio

```bash
git clone https://github.com/Ch-sch-xxx/AulaVivaa.git
cd AulaVivaa
```

---

### PASO 2: Configurar Variables de Entorno

#### 2.1. Configurar App Android

Crea o edita el archivo `local.properties` en la **raíz del proyecto**:

```properties
# Supabase (base de datos y storage)
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Google Gemini AI
GEMINI_API_KEY=AIzaSy...

# Spring Boot Backend
# Para emulador Android (10.0.2.2 = localhost del host):
SPRING_BOOT_URL=http://10.0.2.2:8080/

# Para dispositivo físico (misma red WiFi):
# SPRING_BOOT_URL=http://192.168.1.X:8080/

# Para producción:
# SPRING_BOOT_URL=https://tu-servidor.com/
```

**¿Dónde obtener las credenciales de Supabase?**
1. Ve a tu proyecto en [Supabase Dashboard](https://app.supabase.com)
2. **Settings → API** → Copia:
   - `Project URL` → `SUPABASE_URL`
   - `anon public` → `SUPABASE_ANON_KEY`

#### 2.2. Configurar Backend Spring Boot

Edita `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres
    username: postgres
    password: TU_PASSWORD_SUPABASE

supabase:
  url: https://xxxxx.supabase.co
  anon-key: eyJhbGciOiIsInR5cCI6IkpXVCJ9...
  service-role-key: eyJhbGciOiJIUzI16IXVCJ9...

jwt:
  secret: CAMBIA_ESTE_SECRET_MINIMO_32_CARACTERES
  expiration: 86400000  # 24 horas en ms
```

**¿Dónde obtener las credenciales de Supabase para el backend?**
1. **Settings → Database** → Copia:
   - `Host` → parte de `datasource.url`
   - `Database name` → `postgres`
   - `Password` → `datasource.password`
2. **Settings → API** → Copia:
   - `service_role` → `service-role-key`

---

### PASO 3: Ejecutar el Backend (Spring Boot)

#### Opción A: Desde la raíz del proyecto

```powershell
.\gradlew.bat :backend:bootRun
```

#### Opción B: Desde el directorio backend

```powershell
cd backend
.\gradlew.bat bootRun
```

**Verificar que el backend está corriendo:**
- Abre un navegador y ve a: `http://localhost:8080/api/auth/login`
- Deberías ver un error 405 (Method Not Allowed) — esto significa que el servidor está activo (espera un POST, no un GET)

**Logs esperados:**
```
Started AulaVivaBackendApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

---

### PASO 4: Compilar y Ejecutar la App Android

#### 4.1. Abrir en Android Studio

1. Abre **Android Studio**
2. **File → Open** → Selecciona la carpeta `AulaVivaa`
3. Espera a que Gradle sincronice (primera vez puede tardar 3-5 minutos)

#### 4.2. Compilar el Proyecto

Desde PowerShell en la raíz del proyecto:

```powershell
# Limpiar y compilar
.\gradlew.bat clean assembleDebug

# Ejecutar tests unitarios
.\gradlew.bat testDebugUnitTest
```

**Resultado esperado:**
```
BUILD SUCCESSFUL in XXs
```

#### 4.3. Ejecutar en Emulador o Dispositivo

**Opción A: Desde Android Studio**
1. Selecciona un dispositivo/emulador en el dropdown
2. Click en el botón **Run** (▶️) o presiona `Shift + F10`

**Opción B: Desde terminal (con dispositivo conectado)**

```powershell
# Verificar dispositivos conectados
adb devices

# Instalar APK debug
.\gradlew.bat installDebug
```

---

### PASO 5: Probar el Sistema

#### 5.1. Registro y Login

1. **Abrir la app** en el emulador/dispositivo
2. **Registrar un usuario docente:**
   - Email: `docente@test.com`
   - Password: `password123`
   - Rol: **Docente**
3. **Login** con las credenciales creadas

#### 5.2. Flujo Docente

1. **Crear Asignatura:**
   - Nombre: `Programación Orientada a Objetos`
   - Descripción: `Curso de POO con Java`
2. **Generar código de inscripción** (ej: `POO2025-A1B2`)
3. **Crear Clase:**
   - Nombre: `Introducción a Herencia`
   - Descripción: `Conceptos básicos de herencia en POO`
   - Fecha: `Lunes 2 de Diciembre, 14:00hrs`
   - Subir un PDF de contenido
4. **Usar Asistente IA:**
   - Click en la clase creada → **Ver PDF**
   - Click en **Asistente IA**
   - Probar funciones:
     - ✅ Generar ideas para clase
     - ✅ Sugerir actividades
     - ✅ Estructurar clase
     - ✅ Generar guía de presentación
     - ✅ Crear ejercicios
   - **Chat contextual:** escribir preguntas sobre el PDF
   - **Reanálisis:** click en botón de reanálisis si cambió el PDF

#### 5.3. Flujo Alumno

1. **Registrar un usuario alumno** (email: `alumno@test.com`, rol: Alumno)
2. **Login** con las credenciales del alumno
3. **Inscribirse a asignatura:**
   - Ingresar el código generado por el docente (ej: `POO2025-A1B2`)
4. **Ver clases disponibles** de la asignatura
5. **Usar Asistente IA:**
   - Click en una clase → **Ver PDF**
   - Click en **Asistente IA**
   - Probar funciones:
     - ✅ Explicar conceptos
     - ✅ Generar resumen de estudio
     - ✅ Crear ejercicios prácticos
   - **Chat contextual:** hacer preguntas sobre el PDF

---

## 📦 GENERACIÓN DEL APK FIRMADO

### Configuración del Keystore

#### 1. Crear Keystore (si no existe)

Desde PowerShell en la raíz del proyecto:

```powershell
keytool -genkey -v -keystore aulaviva-release.jks `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias aulaviva-key
```

**Datos solicitados (ejemplo):**
- Password del keystore: `tu-password-seguro`
- Nombre: `Christopher Schiefelbein`
- Organización: `Duoc UC`
- Ciudad: `Santiago`
- Región: `RM`
- País: `CL`

**Resultado:** Se creará el archivo `aulaviva-release.jks` en la raíz del proyecto.

#### 2. Configurar Gradle para Firma

Crea o edita el archivo `keystore.properties` en la **raíz del proyecto**:

```properties
storeFile=aulaviva-release.jks
storePassword=tu-password-seguro
keyAlias=aulaviva-key
keyPassword=tu-password-seguro
```

**⚠️ Importante:** Añade `keystore.properties` al `.gitignore` para no subir contraseñas a Git.

#### 3. Compilar APK Firmado

Desde PowerShell:

```powershell
# Compilar versión release firmada
.\gradlew.bat assembleRelease
```

**Ubicación del APK:**
```
app/build/outputs/apk/release/app-release.apk
```

#### 4. Verificar Firma del APK

```powershell
# Verificar certificado del APK
keytool -printcert -jarfile app\build\outputs\apk\release\app-release.apk
```

**Salida esperada:**
```
Signer #1:
Signature:
Owner: CN=Christopher Schiefelbein, O=Duoc UC, L=Santiago, ST=RM, C=CL
Issuer: CN=Christopher Schiefelbein, O=Duoc UC, L=Santiago, ST=RM, C=CL
Serial number: xxxxxxxx
Valid from: ... until: ...
Certificate fingerprints:
  SHA1: XX:XX:XX:...
  SHA256: XX:XX:XX:...
```

### Archivos Generados

| Archivo | Ubicación | Descripción |
|---------|-----------|-------------|
| `app-release.apk` | `app/build/outputs/apk/release/` | APK firmado listo para distribuir |
| `aulaviva-release.jks` | Raíz del proyecto | Keystore con clave privada (NO subir a Git) |
| `keystore.properties` | Raíz del proyecto | Configuración de firma (NO subir a Git) |

### Capturas del Proceso

**Captura 1: Comando keytool para crear .jks**
```powershell
PS C:\Users\Chris\AndroidStudioProjects\AulaViva> keytool -genkey -v -keystore aulaviva-release.jks ...
Generating 2,048 bit RSA key pair and self-signed certificate (SHA256withRSA) with a validity of 10,000 days
        for: CN=Christopher Schiefelbein, O=Duoc UC, L=Santiago, ST=RM, C=CL
[Storing aulaviva-release.jks]
```

**Captura 2: Build release exitoso**
```powershell
PS C:\Users\Chris\AndroidStudioProjects\AulaViva> .\gradlew.bat assembleRelease
> Task :app:packageRelease
BUILD SUCCESSFUL in 45s
```

**Captura 3: Verificación del APK firmado**
```powershell
PS C:\Users\Chris\AndroidStudioProjects\AulaViva> keytool -printcert -jarfile app\build\outputs\apk\release\app-release.apk
Signer #1:
Owner: CN=Christopher Schiefelbein, O=Duoc UC...
```

---

## 🧪 TESTS Y CALIDAD

### Tests Unitarios

**Ejecutar todos los tests:**
```powershell
.\gradlew.bat testDebugUnitTest
```

**Ubicación de los tests:**
```
app/src/test/java/cl/duocuc/aulaviva/
├── domain/usecase/
│   ├── GenerarIdeasUseCaseTest.kt
│   ├── SugerirActividadesUseCaseTest.kt
│   └── ...
└── presentation/viewmodel/
    └── IAViewModelTest.kt
```

**Cobertura:** Tests de lógica de dominio (UseCases) con mocks de repositorio.

### ProGuard / R8

**Configuración de ofuscación para release:**
- Archivo: `app/proguard-rules.pro`
- Mantiene clases de Retrofit, Room, Gemini SDK, PDFBox
- Ofusca código de la app para protección

---

## 📚 DOCUMENTACIÓN ADICIONAL

### Estructura del Proyecto

```
AulaViva/
├── app/                          # App Android
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/cl/duocuc/aulaviva/
│   │   │   │   ├── data/              # Capa de datos
│   │   │   │   │   ├── local/        # Room DB (entities, DAOs)
│   │   │   │   │   └── repository/   # Implementaciones de repositorios
│   │   │   │   ├── domain/            # Capa de dominio
│   │   │   │   │   ├── model/        # Modelos de dominio
│   │   │   │   │   ├── repository/   # Interfaces de repositorios
│   │   │   │   │   └── usecase/      # Casos de uso (lógica de negocio)
│   │   │   │   └── presentation/      # Capa de presentación
│   │   │   │       ├── ui/           # Activities, Fragments, Adapters
│   │   │   │       └── viewmodel/    # ViewModels (MVVM)
│   │   │   └── res/                  # Recursos (layouts, strings, etc.)
│   │   └── test/                     # Tests unitarios
│   ├── build.gradle.kts              # Configuración de Gradle (app)
│   └── proguard-rules.pro            # Reglas de ofuscación
├── backend/                      # Backend Spring Boot
│   ├── src/main/kotlin/cl/duocuc/aulaviva/
│   │   ├── domain/                   # Entidades de dominio
│   │   ├── application/              # Servicios y DTOs
│   │   ├── infrastructure/           # Repositorios JPA, Config, Security
│   │   └── presentation/             # Controladores REST
│   └── build.gradle.kts              # Configuración de Gradle (backend)
├── database/                     # Scripts SQL (Supabase)
│   ├── 01_create_asignaturas_table_FIXED.sql
│   ├── 02_create_alumno_asignaturas_table.sql
│   └── ...
├── gradle/                       # Wrapper de Gradle
├── local.properties              # Variables de entorno (NO subir a Git)
├── keystore.properties           # Configuración de firma (NO subir a Git)
├── aulaviva-release.jks          # Keystore (NO subir a Git)
└── README.md                     # Este archivo
```

### Documentación del Backend

Ver [backend/README.md](backend/README.md) para:
- Detalles de los endpoints REST
- Configuración de Supabase
- Despliegue en Railway/Heroku/AWS
- Solución de problemas

### Base de Datos (Supabase PostgreSQL)

**Tablas principales:**
- `usuarios` (id, email, password_hash, rol, created_at)
- `asignaturas` (id, nombre, descripcion, codigo_unico, docente_id, created_at)
- `clases` (id, nombre, descripcion, fecha, archivo_pdf_url, archivo_pdf_nombre, asignatura_id, created_at)
- `alumno_asignaturas` (alumno_id, asignatura_id, fecha_inscripcion)
- `chat_sessions` (id, clase_nombre, archivo_nombre, analisis_pdf, created_at)
- `chat_messages` (id, session_id, sender, message, created_at)

**RLS (Row Level Security):** Activado para proteger datos por usuario/rol.

---

## 🐛 SOLUCIÓN DE PROBLEMAS

### Backend no se conecta

**Error:** `Connection refused` o `Unable to connect to database`

**Solución:**
1. Verifica que el backend esté corriendo: `http://localhost:8080`
2. Verifica credenciales de Supabase en `application.yml`
3. Verifica que tu IP esté en la whitelist de Supabase (Settings → Database → Connection Pooling)

### App no conecta con el backend

**Error:** `Failed to connect to /10.0.2.2:8080`

**Solución:**
1. Si usas **emulador:** URL debe ser `http://10.0.2.2:8080/`
2. Si usas **dispositivo físico:** URL debe ser tu IP local `http://192.168.1.X:8080/`
3. Verifica firewall de Windows (puerto 8080 debe estar abierto)
4. Verifica CORS en `SecurityConfig.kt` del backend

### Gemini API retorna error

**Error:** `API key not valid` o `Rate limit exceeded`

**Solución:**
1. Verifica que `GEMINI_API_KEY` en `local.properties` sea correcta
2. Verifica cuota de API en [Google AI Studio](https://ai.google.dev)
3. Si excediste el límite gratuito, espera 24h o actualiza a plan de pago

### Room Database versión incorrecta

**Error:** `Expected schema version X, but database is version Y`

**Solución:**
1. Desinstala la app del emulador/dispositivo
2. Reinstala: `.\gradlew.bat installDebug`
3. Si persiste, limpia datos de la app: Settings → Apps → AulaViva → Clear Data

---

## 📝 NOTAS FINALES

### Características Técnicas Destacables

1. **Clean Architecture:** Separación clara entre capas (Domain, Data, Presentation)
2. **MVVM:** ViewModels con LiveData para UI reactiva
3. **Offline-First:** Room DB como fuente de verdad, sincronización con backend
4. **Chat Stateful:** Historial persistente con Room, rehidratación del chat SDK
5. **RAG básico:** Extracción de texto PDF + inyección en contexto de prompts
6. **Metadata automática:** Extracción de título/autor del PDF vía PDFBox
7. **Seguridad:** JWT, ProGuard, HTTPS, Spring Security, RLS en DB

### Limitaciones Conocidas

1. **Chat temporal:** Máximo 3 mensajes (configurable en `IARepository.MAX_MESSAGES`)
2. **Contexto limitado:** 30,000 caracteres máx (límite de Gemini 2.5 Pro)
3. **Sin streaming:** Respuestas completas (no token-by-token)
4. **Sin caché:** Cada llamada IA consume cuota de API
5. **Room migration:** Actualmente usa `fallbackToDestructiveMigration()` (borra datos en cambio de schema)

### Mejoras Futuras

- [ ] Migración a **Hilt** (Dependency Injection)
- [ ] **Streaming** de respuestas IA (token por token)
- [ ] **Caché** de análisis de PDFs (evitar re-análisis)
- [ ] **Paginación** de clases y asignaturas
- [ ] **Notificaciones push** (Firebase Cloud Messaging)
- [ ] **Modo oscuro** completo
- [ ] **Tests de integración** (Espresso UI tests)
- [ ] **CI/CD** (GitHub Actions para builds automáticos)
- [ ] **Analytics** (Firebase Analytics)
- [ ] **Crash reporting** (Firebase Crashlytics)

---

## 📄 LICENCIA

MIT License

Copyright (c) 2024 Christopher Schiefelbein

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

## 📧 CONTACTO

**Christopher Schiefelbein**
- 📧 Email: ch.sch.informatico@gmail.com
- 💼 LinkedIn: [christopher-schiefelbein](https://www.linkedin.com/in/christopher-schiefelbein-8292b4160/)
- 🐙 GitHub: [@Ch-sch-xxx](https://github.com/Ch-sch-xxx)

---

**⭐ Si te gustó el proyecto, dame una estrella en GitHub!**

**🎓 Proyecto Académico - Duoc UC - Desarrollo de Aplicaciones Móviles - 2024**
