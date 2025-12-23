# Aula Viva AI - Web Edition (PWA)

![Status](https://img.shields.io/badge/Status-Stable%20Demo-success) ![Tech](https://img.shields.io/badge/Stack-React%20%7C%20Vite%20%7C%20Gemini-blueviolet) ![Type](https://img.shields.io/badge/Type-PWA%20Offline-orange)

> **"Tu tutor de bolsillo: personalización real para cada asignatura."**

## 📖 Descripción del Proyecto

**Aula Viva AI** es una plataforma educativa de vanguardia diseñada para democratizar el acceso a la tutoría personalizada mediante Inteligencia Artificial Generativa. Esta versión es un port web completo (PWA) de la aplicación nativa Android original, evolucionada para ofrecer accesibilidad universal sin instalaciones complejas.

La aplicación implementa una arquitectura **"Offline-First"** (Local Web), permitiendo que toda la lógica, bases de datos y análisis de documentos ocurran directamente en el dispositivo del usuario, garantizando privacidad y funcionamiento incluso sin conexión constante (salvo para consultas a la IA).

### 🌟 Misión
Empoderar a docentes y estudiantes convirtiendo cada documento de clase (PDF) en un asistente interactivo capaz de explicar, evaluar y planificar.

---

## 🚀 Características Principales

### 🧠 Tutoría IA Contextual (RAG Client-Side)
No es un chat genérico. Aula Viva **lee tus PDFs de clase** y responde preguntas basándose *estrictamente* en ese contenido.
*   **Para Alumnos**: Explica conceptos complejos, crea resúmenes y genera quizzes de práctica.
*   **Para Docentes**: Sugiere planificaciones de clase, rúbricas y actividades didácticas basadas en el material.

### ⚡ Arquitectura "Edge" & Privacidad
*   **Cero Base de Datos Central**: Toda tu información (usuarios, clases, PDFs) vive en tu navegador (`IndexedDB` vía `Dexie.js`).
*   **API Key Privada**: Tu llave de Google Gemini se almacena encriptada localmente y viaja directo de tu navegador a Google. Nadie más la ve.

### 🎨 Experiencia "Cyberpunk Academy"
Una interfaz inmersiva diseñada para capturar la atención:
*   Diseño **Glassmorphism** y neón "Deep Violet".
*   Animaciones fluidas (Framer Motion).
*   Micro-interacciones gamificadas.

### 📱 PWA Universal
*   Instalable en Android/iOS/Windows.
*   Funciona como app nativa (Full screen, sin barra de navegación).

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología | Función |
| :--- | :--- | :--- |
| **Frontend** | React 18 + TypeScript | Lógica y Componentes UI |
| **Build Tool** | Vite | Empaquetado ultrarrápido |
| **Estilos** | Tailwind CSS | Sistema de diseño utilitario |
| **Animación** | Framer Motion | Transiciones y efectos visuales |
| **Base de Datos** | Dexie.js (IndexedDB) | Persistencia local completa (NoSQL) |
| **IA Engine** | Google Gemini 3 Flash (Preview) | Modelo Multimodal SOTA (State of the Art) |
| **Fallback** | Gemini 2.5 Flash | Respaldo para alta disponibilidad |
| **PDF Engine** | PDF.js | Procesamiento de documentos en cliente |

---

## 🏁 Guía de Instalación (Local)

Para ejecutar este proyecto en tu entorno local:

1.  **Clonar el Repositorio**
    ```bash
    git clone https://github.com/tu-usuario/aulaviva-pwa.git
    cd aulaviva-pwa/pwa
    ```

2.  **Instalar Dependencias**
    ```bash
    npm install
    ```

3.  **Ejecutar Servidor de Desarrollo**
    ```bash
    npm run dev
    ```

4.  **Abrir en Navegador**
    Visita `http://localhost:5173`.

> **Nota**: Para usar las funciones de IA, necesitarás una [API Key gratuita de Google Gemini](https://aistudio.google.com/app/apikey).

---

## 📸 Estructura del Proyecto

```
src/
├── components/      # UI Reutilizable (Cards, Buttons, Inputs)
├── db/              # Esquema de Base de Datos Local (Dexie)
├── pages/           # Vistas Principales (Dashboard, Auth, Details)
├── services/        # Lógica de Negocio (AI, Auth, Data)
├── store/           # Estado Global (Zustand)
└── App.tsx          # Router y Configuración Principal
```

---

## 👤 Autor

**Christopher Schiefelbein**
*   Ingeniería en Informática - Duoc UC
*   [Portafolio Profesional](https://portafolio-devchris.vercel.app/)

---
*Este proyecto es una demostración académica y técnica. La versión de producción podría requerir integración con servicios backend (PostgreSQL/Supabase) para sincronización entre dispositivos.*
