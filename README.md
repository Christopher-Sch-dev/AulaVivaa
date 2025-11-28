# 🎓 Aula Viva

> Sistema educativo móvil con IA generativa para mejorar comprensión de clases

[![Android](https://img.shields.io/badge/Android-SDK%2024--36-green)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple)]()
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot%203.2-brightgreen)]()
[![Supabase](https://img.shields.io/badge/Database-Supabase%20PostgreSQL-blue)]()
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
- 🔐 Autenticación segura (JWT vía Spring Boot)
- 🔄 Sincronización offline-first (Room + Spring Boot REST API)
- 🎨 Material Design 3

## 🏗️ Stack Tecnológico

**Frontend:**
- Kotlin
- MVVM + Repository Pattern + Clean Architecture
- Material Design 3
- Room Database (persistencia local)
- Kotlin Coroutines (manejo asíncrono)

**Backend:**
- Spring Boot 3.2.0 (Kotlin) - REST API
- Supabase PostgreSQL (base de datos)
- Supabase Auth (autenticación de usuarios)
- Supabase Storage (almacenamiento de PDFs)
- Retrofit 2 + OkHttp (cliente HTTP en Android)

**IA:**
- Google Gemini 2.5 Pro (LLM)
- PDFBox-Android (extracción de PDFs)
- RAG básico (contexto inyectado)

**Seguridad:**
- JWT (autenticación vía Spring Boot)
- ProGuard (ofuscación release)
- Spring Security (protección de endpoints)
- RLS (Row Level Security en Supabase PostgreSQL)

## 📸 Screenshots

[Agregar 3-4 capturas de pantalla]

## 🚀 Instalación

### Requisitos
- Android Studio Hedgehog+ (2023.1.1)
- Android SDK 24-36
- Java 17+ (para Spring Boot)
- Cuenta Supabase
- API Key de Google Gemini

### Setup

#### 1. Clonar el repositorio

```bash
git clone https://github.com/Ch-sch-xxx/AulaVivaa.git
cd AulaVivaa
```

#### 2. Configurar variables de entorno

Crea o edita el archivo `local.properties` en la raíz del proyecto:

```properties
# Supabase (para base de datos y storage)
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu-anon-key

# Google Gemini AI
GEMINI_API_KEY=tu-api-key-gemini

# Spring Boot Backend
# Para desarrollo local (emulador Android):
SPRING_BOOT_URL=http://10.0.2.2:8080/
# Para dispositivo físico (mismo WiFi):
# SPRING_BOOT_URL=http://192.168.1.X:8080/
# Para producción:
# SPRING_BOOT_URL=https://tu-servidor.com/
```

#### 3. Configurar el Backend Spring Boot

Edita `backend/src/main/resources/application.yml` o configura variables de entorno:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres
    username: postgres
    password: tu-password

supabase:
  url: https://xxxxx.supabase.co
  anon-key: tu-anon-key
  service-role-key: tu-service-role-key

jwt:
  secret: tu-secret-key-minimo-256-bits
```

#### 4. Iniciar el Backend

```bash
# Desde la raíz del proyecto
./gradlew :backend:bootRun

# O desde el directorio backend
cd backend
./gradlew bootRun
```

El servidor se iniciará en `http://localhost:8080`

#### 5. Compilar y ejecutar la App Android

```bash
# Compilar
./gradlew assembleDebug

# O ejecutar desde Android Studio
```

## 🔌 Arquitectura Backend + App Móvil

### ¿Cómo funciona la conexión?

1. **Backend Spring Boot**: Servidor REST API que corre en un servidor (local para desarrollo, remoto para producción)
2. **App Android**: Cliente que se conecta al backend vía HTTP/REST usando Retrofit
3. **Base de Datos**: Spring Boot se conecta directamente a Supabase PostgreSQL
4. **Autenticación**: Spring Boot usa Supabase Auth para validar usuarios y genera JWT propios
5. **Storage**: Spring Boot actúa como proxy para subir PDFs a Supabase Storage

### Configuración de URLs

- **Desarrollo local (Emulador Android)**: `http://10.0.2.2:8080/` (10.0.2.2 es el alias del localhost de la máquina host en el emulador)
- **Desarrollo local (Dispositivo físico)**: `http://192.168.1.X:8080/` (IP local de tu máquina en la misma red WiFi)
- **Producción**: `https://tu-servidor.com/` (URL pública del servidor donde está desplegado Spring Boot)

### Despliegue del Backend

Para producción, puedes desplegar Spring Boot en:
- **Railway**: https://railway.app
- **Heroku**: https://heroku.com
- **AWS EC2/Elastic Beanstalk**: https://aws.amazon.com
- **Google Cloud Run**: https://cloud.google.com/run
- **DigitalOcean App Platform**: https://www.digitalocean.com/products/app-platform

Ver más detalles en [backend/README.md](backend/README.md)

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
