# 📋 Informe Técnico: Aula Viva AI (PWA)

**Versión:** 2.0 (Stable Demo)  
**Fecha:** Diciembre 2025  
**Desarrollador:** Christopher Schiefelbein

---

## 1. Resumen Ejecutivo
**Aula Viva AI** es una solución tecnológica educativa que democratiza el acceso a tutores inteligentes personalizados. A diferencia de las plataformas SaaS tradicionales, Aula Viva opera bajo un modelo **"Local-First"**, garantizando privacidad, velocidad y funcionamiento sin conexión.

Este proyecto demuestra la capacidad de portar lógica de negocio compleja (originalmente en Kotlin/Android) a una arquitectura web moderna, escalable y visualmente impactante.

---

## 2. Arquitectura del Sistema

El sistema sigue una arquitectura modular basada en **Clean Architecture** adaptada al frontend:

### 2.1 Capas de la Aplicación
1.  **Capa de Presentación (UI)**:
    *   Componentes "Atómicos" (`components/ui`): Botones, Inputs, Cards. Reutilizables y puramente visuales.
    *   Vistas (`pages/`): Gestionan el estado de la página y orquestan componentes.
    *   Layouts: Estructuras maestras que manejan navegación y temas.

2.  **Capa de Lógica de Negocio (Services)**:
    *   `AuthService`: Maneja sesiones, registro y simulación de latencia de red.
    *   `DataService`: Abstracción completa sobre la base de datos. Permite cambiar el motor de persistencia sin tocar la UI.
    *   `AIService`: Cliente HTTP optimizado para la API de Google Gemini. Maneja reintentos, prompts de sistema y manejo de errores.

3.  **Capa de Persistencia (Data)**:
    *   **IndexedDB (vía Dexie.js)**: Base de datos NoSQL transaccional en el navegador. Almacena usuarios, asignaturas, clases y logs.

### 2.2 Diagrama de Flujo de Datos
```mermaid
graph TD
    User[Usuario] <--> UI[Interfaz React]
    UI <--> Services[Servicios (AI/Auth/Data)]
    Services <--> DB[(IndexedDB Local)]
    Services -.-> |Solo cuando hay red| Gemini[Google Gemini API]
```

---

## 3. Decisiones Técnicas Clave

### 3.1 ¿Por qué Offline-First (LocalDB)?
Para garantizar que estudiantes en zonas con conectividad intermitente puedan seguir accediendo a sus materiales y notas pre-cargadas. La sincronización es irrelevante para el estudio individual inmediato.

### 3.2 Privacidad P2P en la IA
En lugar de crear un backend proxy (Python/Node) que intercepte y almacene los prompts de los usuarios, la aplicación conecta el navegador del cliente directamente con Google.
*   **Ventaja**: Costo de servidor = $0. Privacidad máxima (el desarrollador no ve los datos).
*   **Seguridad**: La API Key se almacena en `localStorage` del usuario, no en el código.

### 3.3 Diseño "Futuristic Academy"
Se eligió una estética oscura (Dark Mode) con acentos neón (Violeta/Teal) y efectos de cristal (Glassmorphism) para:
*   Reducir fatiga visual en sesiones largas de estudio.
*   Diferenciarse de los LMS (Learning Management Systems) corporativos aburridos.
*   Evocar modernidad e innovación tecnológica.

---

## 4. Retos Superados

1.  **Lectura de PDF en Cliente**: Implementar `pdf.js` para extraer texto masivo sin bloquear el hilo principal de JS (Main Thread).
2.  **Gestión de Contexto de IA**: Optimizar los prompts para que Gemini 1.5 Flash pueda "leer" clases enteras sin alucinar información.
3.  **Animaciones Performantes**: Uso de `Framer Motion` con aceleración por hardware (GPU) para mantener 60fps incluso con fondos animados.

---

## 5. Conclusión
Aula Viva AI PWA no es solo un formulario web; es una aplicación compleja de gestión de estado y persistencia que rivaliza con aplicaciones nativas de escritorio, demostrando un dominio avanzado del ecosistema React y la integración de IA Generativa.
