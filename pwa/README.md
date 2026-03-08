# 🎓 Aula Viva AI

> **La clase no puede depender de si el profe subió bien el PDF.**

*Progressive Web App | React + Vite + Gemini | Christopher Schiefelbein*

---

## ¿Por qué existe esto?

Estudié en Duoc UC y viví el problema de primera mano: los profesores a veces muestran PDFs en clase que nunca suben a la plataforma, o los suben mal. El contenido del día se pierde. Los alumnos no prestan atención, no toman buenas notas, y al momento de estudiar no tienen nada.

Aula Viva reemplaza y agiliza ese proceso completo, para los dos lados del aula.

El docente puede subir el material de la clase, organizar su contenido por asignatura y fecha, y tener apoyos de IA para preparar su clase. El alumno, al estar inscrito en la misma asignatura, accede al material directamente, puede hacer repasos inteligentes, pedir resúmenes, hacer preguntas sobre el contenido, y aprender de forma mucho más activa.

Todo organizado por asignatura, por fecha y por orden estudiantil. Nada se pierde.

---

## ¿Cómo funciona?

El núcleo de la app es un pipeline de **RAG (Retrieval-Augmented Generation)** que corre completamente en el navegador:

1. El docente sube un PDF a la clase.
2. `pdf.js` extrae el texto en un Web Worker (sin bloquear la UI).
3. Si el PDF es una imagen escaneada, Tesseract.js aplica OCR automáticamente.
4. El texto extraído se inyecta en el contexto de **Gemini 2.5 Flash** junto con el historial de chat.
5. El alumno o docente interactúa con la IA, que responde basándose en el contenido real del material.

La IA adapta su respuesta según el rol del usuario:
- **Docente**: enfoque didáctico, planificación, taxonomía de Bloom.
- **Alumno**: explicaciones simples, analogías, mnemotecnia, modo repaso.

---

## Filosofía técnica

Tomé la decisión de hacer todo **Local-First**: sin backend propio, sin base de datos en servidor, sin costos de infraestructura.

- **Persistencia**: IndexedDB vía Dexie.js — base de datos transaccional completa en el navegador.
- **API Key**: modelo BYOK (el usuario trae su propia clave de Gemini), nunca transmitida a ningún servidor mío.
- **Deploy**: Vercel, dominio público, sin fricción de instalación.

Fue la decisión correcta para un proyecto de portafolio que tiene que funcionar al instante para cualquier evaluador.

---

## Stack

| Capa | Tecnología |
|------|------------|
| UI | React 18, TypeScript |
| Build | Vite |
| Estado | Zustand, React Router v7 |
| DB Local | Dexie.js (IndexedDB) |
| Estilos | Tailwind CSS, Framer Motion |
| IA | Google Gemini 2.5 Flash (vía `@google/generative-ai`) |
| PDF | pdf.js (Web Worker), Tesseract.js (OCR) |
| Deploy | Vercel |

---

## Ejecutar localmente

```bash
# Instalar dependencias
npm install

# Iniciar servidor de desarrollo
npm run dev

# Compilar para producción
npm run build
```

> Necesitas una API Key de Google Gemini. Puedes obtener una gratis en [aistudio.google.com](https://aistudio.google.com). La app te la pide al inicio, se guarda solo en tu `localStorage`.

---

## Seguridad

- La API key nunca sale del navegador.
- Las respuestas de IA pasan por `react-markdown` con sanitización para prevenir XSS.
- Roles estrictos: los alumnos no acceden a controles de edición de docentes.

---

*Desarrollado por Christopher Schiefelbein — 2025*
