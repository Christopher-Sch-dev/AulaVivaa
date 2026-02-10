# 🎓 AulaViva

**Aplicación Android para gestión educativa con asistente de IA**

Desarrollado por **Christopher Schiefelbein** - Duoc UC | Diciembre 2025

---

## 📱 Descripción

App móvil que conecta docentes y alumnos para gestionar asignaturas, clases y material educativo. Incluye un asistente de IA (Google Gemini) que analiza PDFs y ayuda tanto a profesores como estudiantes.

---

## 🛠️ Stack Tecnológico

### App Android
| Tecnología | Versión |
|------------|---------|
| Kotlin | 2.1.0 |
| Jetpack Compose | BOM 2024.11 |
| Room | 2.6.1 |
| Retrofit | 2.11.0 |
| Coil | 2.5.0 |
| PDFBox-Android | 2.0.27 |

### Backend
| Tecnología | Uso |
|------------|-----|
| Spring Boot 3.2 | REST API |
| PostgreSQL | Base de datos (Supabase) |
| Google Cloud Run | Hosting producción |

### Servicios
- **Google Gemini 3 Flash** - IA generativa
- **Supabase** - DB + Storage + Auth

---

## 🎨 UI & Efectos

Interfaz construida con **Jetpack Compose** y tema cyberpunk personalizado:

- `MatrixBackground` - Fondo animado estilo Matrix
- `GlitchText` - Texto con efecto glitch
- `CyberLoading` - Spinner con rotación
- `AulaVivaBootScreen` - Splash estilo terminal
- Animaciones: `tween`, `spring`, `infiniteRepeatable`

---

## 📡 API Endpoints

| Módulo | Endpoints |
|--------|-----------|
| Auth | `/api/auth/register`, `/api/auth/login`, `/api/auth/me` |
| Asignaturas | CRUD + `/generar-codigo` |
| Clases | CRUD completo |
| Alumnos | `/inscribir`, `/asignaturas` |
| Storage | `/upload` (PDFs) |

**Backend URL:** `https://aulaviva-backend-620174961947.southamerica-west1.run.app`

---

## 🤖 Funciones IA

**Docentes:** Ideas, actividades, estructurar clase, guía presentación, ejercicios  
**Alumnos:** Explicaciones, resúmenes, ejercicios prácticos

---

## 🧪 Tests

32 tests unitarios cubriendo ViewModels, Repositories y validaciones.

```bash
./gradlew testDebugUnitTest
```

---

## 📦 APK Firmado

```bash
# Compilar release
$env:JAVA_HOME="C:\Users\Chris\.jdks\ms-17.0.16"
./gradlew clean assembleRelease
```

**Ubicación:** `app/build/outputs/apk/release/app-release.apk`

---

## 🏃 Ejecutar

### Backend (local)
```bash
cd backend
./gradlew bootRun
```

### App
Abrir en Android Studio → Run

---

## 📁 Estructura

```
AulaViva/
├── app/                 # App Android (Kotlin + Compose)
│   ├── src/main/       # Código fuente
│   └── src/test/       # Tests unitarios
├── backend/            # Spring Boot API
└── README.md
```

---

**Recursos Nativos:** Notificaciones (POST_NOTIFICATIONS) + Storage para PDFs

---

*Christopher Schiefelbein - Diciembre 2025*
