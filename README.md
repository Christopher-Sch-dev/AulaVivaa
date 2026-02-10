# 🎓 Aula Viva AI

**Ecosistema Unificado de Gestión Educativa | Arquitectura PWA & Móvil**

*Desarrollado por **Christopher Schiefelbein** | Portafolio de Ingeniería Informática*

---

## 🚀 Resumen Ejecutivo

Diseñé **Aula Viva AI** para conectar la gestión educativa tradicional con el potencial de la inteligencia artificial moderna. Este repositorio documenta la evolución de mi proyecto desde una aplicación nativa Android (v1.0) hacia una Progressive Web App (PWA) sofisticada y con enfoque "Local-First" (v2.0).

Mi objetivo principal fue crear un sistema que realmente empodere a docentes y alumnos con herramientas de IA para generar contenido y mejorar la comprensión, manteniendo siempre una estricta privacidad de datos y capacidad de funcionamiento offline.

---

## 🌐 Estructura del Proyecto

Organicé este repositorio en dos fases arquitectónicas distintas:

1.  **Producción Actual (Raíz)**: La Progressive Web App v2.0 (React/Vite). Esta es la iteración activa y moderna del proyecto.
2.  **Archivo Legado (`OLD-aulaviva-KOTLIN`)**: La aplicación nativa Android v1.0 original. La mantengo aquí únicamente por contexto histórico y referencia de código.

---

## ✨ Aula Viva v2.0 (PWA)

### Mi Filosofía Técnica: "Thick Client & Local-First"
Para la migración a web, tomé la decisión arquitectónica deliberada de adoptar un enfoque **Local-First**. En lugar de depender de una infraestructura backend pesada, diseñé la aplicación para que ejecute la lógica compleja y la persistencia de datos directamente en el navegador del usuario.

*   **Latencia Cero**: Las interacciones son inmediatas porque no esperan viajes de ida y vuelta al servidor.
*   **Privacidad por Diseño**: Los datos viven en el dispositivo del usuario, no en una base de datos central en la nube.
*   **Eficiencia de Costos**: Elimino la necesidad de servidores backend costosos "siempre encendidos" para una demostración de portafolio.

### El Stack Tecnológico
Construí la PWA utilizando un stack moderno orientado al rendimiento:

*   **Core**: React 18, TypeScript, Vite.
*   **Estado y Lógica**: Zustand (Gestión de Estado), React Router v7.
*   **Persistencia**: Dexie.js (wrapper de IndexedDB) para una base de datos tipo SQL robusta en el cliente.
*   **UI/UX**: Tailwind CSS con una estética personalizada "Cyberpunk/Neon" y Framer Motion para orquestar las animaciones.
*   **Motor de IA**: Google Gemini API (Flash 1.5) integrada a través de un pipeline RAG (Retrieval-Augmented Generation) personalizado que corre localmente.

### Características Clave
*   **RAG en el Cliente**: Los usuarios suben PDFs y utilizo `pdf.js` (en Web Workers) para extraer el texto y alimentarlo a la ventana de contexto de Gemini, todo sin salir de la sesión del navegador.
*   **IA Basada en Roles**: La IA adapta su personalidad según el usuario logueado (planificador didáctico para Docentes vs. guía de estudio para Alumnos).
*   **Fallbacks Inteligentes**: Incluí Tesseract.js para hacer OCR cuando los PDFs son imágenes escaneadas.

### Cómo Ejecutar (PWA)
1.  **Instalar Dependencias**:
    ```bash
    npm install
    ```
2.  **Iniciar Servidor de Desarrollo**:
    ```bash
    npm run dev
    ```
3.  **Compilar para Producción**:
    ```bash
    npm run build
    ```

---

## 📱 Archivo Legado: Aula Viva v1.0 (Android)

*Ubicación: `/OLD-aulaviva-KOTLIN`*

Esta carpeta contiene la implementación nativa original. Desarrollé esta versión usando **Kotlin** y **Jetpack Compose**, siguiendo principios de arquitectura limpia (MVVM).

### Highlights del Stack Legado:
*   **Android**: Kotlin, Jetpack Compose, Room Database, Retrofit.
*   **Backend**: Spring Boot 3.2 (Java), PostgreSQL, desplegado en Google Cloud Run.
*   **Funcionalidades**: Renderizado nativo de PDFs, integración de autenticación biométrica y sincronización en segundo plano.

*Nota: Esta versión está archivada y ya no recibe mantenimiento activo.*

---

## 🔒 Seguridad e Integridad

Incluso siendo un proyecto de portafolio, implementé estándares de seguridad profesional:

*   **Seguridad de API Key**: Los usuarios traen su propia llave (modelo BYOK), la cual se guarda estrictamente en el `localStorage` del navegador y nunca se transmite a mis servidores.
*   **Sanitización de Inputs**: Todas las respuestas de la IA son sanitizadas para prevenir ataques XSS.
*   **Tipado Estricto**: Interfaces completas de TypeScript aseguran la integridad de los datos a través de todo el estado de la aplicación.

---

## 👨‍💻 Autor

**Christopher Schiefelbein**
*Estudiante de Ingeniería en Informática & Desarrollador Full Stack*

Me apasiona construir software que se sienta vivo. Aula Viva es un testimonio de mi creencia de que el software educativo debería ser tan atractivo y bien diseñado como las mejores aplicaciones de consumo masivo.

---
*© 2025 Christopher Schiefelbein. Todos los derechos reservados.*
