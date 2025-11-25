# 🎓 Aula Viva

> Sistema educativo móvil con IA generativa para mejorar comprensión de clases

[![Android](https://img.shields.io/badge/Android-SDK%2024--36-green)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple)]()
[![Supabase](https://img.shields.io/badge/Backend-Supabase-blue)]()
[![Gemini](https://img.shields.io/badge/IA-Gemini%202.5%20Pro-orange)]()

## 📱 ¿Qué es Aula Viva?

App móvil Android que conecta docentes y alumnos con inteligencia artificial para mejorar la enseñanza y comprensión de clases.

### Problema que resuelve
- **Alumnos:** No entienden contenido de clases actuales o pasadas
- **Docentes:** Necesitan ayuda para estructurar mejor sus clases y contenidos

### Solución
- **Para alumnos:** Chat+Asistente que repasa y explica contenido de forma personalizada
- **Para docentes:** Chat+Asistente IA que ayuda a preparar clases y generar contenido educativo

## ✨ Características principales

- 📚 Gestión completa de clases (docente)
- 📝 Actividades y contenidos (subida de PDFs)
- 🤖 Chat IA con contexto de la clase (Gemini 2.5 Pro)
- 📄 Extracción de texto de PDFs (RAG básico)
- 🔐 Autenticación segura (JWT)
- 🔄 Sincronización offline-first (Room + Supabase Delta Sync)
- 🎨 Material Design 3

## 🏗️ Stack Tecnológico

**Frontend:**
- Kotlin
- MVVM + Repository Pattern + Clean Architecture
- Material Design 3
- Room Database (persistencia local)
- Kotlin Coroutines (manejo asíncrono)

**Backend:**
- Supabase (PostgreSQL + Auth + RLS)
- Retrofit 2 + OkHttp (networking)

**IA:**
- Google Gemini 2.5 Pro (LLM)
- PDFBox-Android (extracción de PDFs)
- RAG básico (contexto inyectado)

**Seguridad:**
- JWT (autenticación)
- ProGuard (ofuscación release)
- RLS (Row Level Security en Supabase)

## 📸 Screenshots

[Agregar 3-4 capturas de pantalla]

## 🚀 Instalación

### Requisitos
- Android Studio Hedgehog+ (2023.1.1)
- Android SDK 24-36
- Cuenta Supabase
- API Key de Google Gemini

### Setup

1. Clona el repo:
git clone https://github.com/Ch-sch-xxx/AulaVivaa.git
cd AulaVivaa

text

2. Configura las API keys en `local.properties`:
SUPABASE_URL=tu_url_supabase
SUPABASE_KEY=tu_anon_key
GEMINI_API_KEY=tu_api_key_gemini

text

3. Sincroniza Gradle y corre:

./gradlew assembleDebug

## 📦 Descarga

[Link a APK en GitHub Releases cuando esté]

## 🎓 Contexto

Proyecto académico - Duoc UC (Desarrollo de Aplicaciones Móviles, 4º semestre, 2025)

Construido usando desarrollo asistido por IA (GitHub Copilot, Claude Sonnet 4.5) para acelerar implementación, mientras mantuve control de arquitectura, diseño y validación funcional.

## 📝 Licencia

MIT License - Ver [LICENSE](LICENSE)

## 👤 Autor

**Christopher Schiefelbein**
- LinkedIn: [christopher-schiefelbein](https://www.linkedin.com/in/christopher-schiefelbein-8292b4160/)
- GitHub: [@Ch-sch-xxx](https://github.com/Ch-sch-xxx)
- Email: ch.sch.informatico@gmail.com

---

⭐ Si te gustó el proyecto, dame una estrella en GitHub!
