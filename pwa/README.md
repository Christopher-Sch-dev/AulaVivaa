# 🚀 Aula Viva AI - PWA (v2.0)

> **Plataforma Educativa Inteligente Offline-First**
> *Port Web de la App Android nativa "Aula Viva", potenciado por React, IA Generativa y capacidades PWA.*

![Aula Viva Banner](https://via.placeholder.com/1200x400/0F0E16/C4B5FD?text=AULA+VIVA+AI+PWA)

## 📖 Descripción

**Aula Viva AI** es una Progressive Web App (PWA) diseñada para transformar la experiencia educativa mediante el uso de Inteligencia Artificial contextual. Permite a docentes gestionar asignaturas y a estudiantes interactuar con un **Tutor IA** que analiza documentos PDF en tiempo real, todo bajo una arquitectura segura, privada y funcional sin conexión a internet constante.

Esta versión es un **port moderno y estético** de una aplicación académica original en Kotlin, llevada a la web con estándares de rendimiento y diseño "Futuristic/Cyberpunk".

## ✨ Características Principales

*   **🧠 Tutor IA Contextual**: Integración directa con **Google Gemini 1.5 Flash**. Sube un PDF y chatea con él (resúmenes, quizzes, explicaciones).
*   **⚡ Offline-First**: Persistencia de datos completa en el navegador usando **IndexedDB (Dexie.js)**. Funciona sin red.
*   **🎨 UI Futurista**: Interfaz inmersiva con efectos *Glassmorphism*, animaciones *Framer Motion*, fondos *Matrix* y tipografía moderna.
*   **🔒 Privacidad Total**: Arquitectura **P2P (Peer-to-Peer)**. Tu API Key y tus documentos nunca tocan un servidor intermedio; van directo de tu navegador a Google.
*   **📱 PWA Instalable**: Se instala como una app nativa en Android, iOS y Desktop.

## 🛠️ Stack Tecnológico

*   **Core**: React 18, TypeScript, Vite.
*   **Estilos**: Tailwind CSS, PostCSS.
*   **Animaciones**: Framer Motion, Canvas Confetti.
*   **Base de Datos Local**: Dexie.js (IndexedDB wrapper).
*   **IA**: Google Generative AI SDK (Gemini).
*   **Manejo de PDF**: PDF.js.
*   **Iconos**: Lucide React.

## 🚀 Instalación y Despliegue

### Requisitos previos
*   Node.js 18+
*   NPM o Bun

### Desarrollo Local

```bash
# 1. Clonar el repositorio
git clone https://github.com/usuario/aulaviva-pwa.git

# 2. Instalar dependencias
npm install

# 3. Iniciar servidor de desarrollo
npm run dev
```

### Compilación para Producción

```bash
npm run build
# Los archivos estáticos se generarán en /dist
```

## ☁️ Despliegue en Vercel

Este proyecto está configurado para desplegarse instantáneamente en Vercel.

1. Has fork de este repositorio.
2. Importa el proyecto en Vercel.
3. El *Framework Preset* se detectará automáticamente como **Vite**.
4. ¡Listo! Vercel detectará el archivo `vercel.json` para manejar las rutas.

[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2Fusuario%2Faulaviva-pwa)


## 🔐 Seguridad y Reglas de Oro

Este proyecto sigue estrictos protocolos de desarrollo (**Clean Code**):
*   **Validación Estricta**: Todos los formularios blindados contra datos incorrectos.
*   **Arquitectura por Capas**: Separación clara entre Vistas, Componentes UI (Atomic), y Servicios de Lógica (Auth/Data/AI).
*   **Cero Emojis**: Uso exclusivo de grafismos vectoriales (SVG) para una apariencia profesional.

## 👤 Autor

**Christopher Schiefelbein**
*   [Portafolio Web](https://portafolio-devchris.vercel.app/)
*   *Desarrollador Full Stack / Mobile / AI Integration*

---
*Hecho con 💜 y código limpio.*
