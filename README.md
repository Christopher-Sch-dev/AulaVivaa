# 🎓 Aula Viva AI

**Unified Educational Management Ecosystem | PWA & Mobile Architecture**

*Developed by **Christopher Schiefelbein** | Computer Engineering Portfolio*

---

## 🚀 Executive Summary

I designed **Aula Viva AI** to bridge the gap between traditional educational management and modern artificial intelligence. This repository documents the evolution of my project from a native Android application (v1.0) to a sophisticated, Local-First Progressive Web App (v2.0).

My primary goal was to create a system that empowers both teachers and students with AI-driven tools for content generation and comprehension, while maintaining strict data privacy and offline capabilities.

---

## 🌐 Project Structure

This repository is organized into two distinct architectural phases:

1.  **Current Production (Root)**: The v2.0 Progressive Web App (React/Vite). This is the active, modern iteration of the project.
2.  **Legacy Archive (`OLD-aulaviva-KOTLIN`)**: The original v1.0 Native Android Application. Preserved for historical context and code reference.

---

## ✨ Aula Viva v2.0 (PWA)

### Technical Philosophy: "Thick Client & Local-First"
For the web migration, I made a deliberate architectural choice to adopt a **Local-First** approach. Instead of relying on heavy backend infrastructure, I engineered the application to perform complex logic and data persistence directly in the user's browser.

*   **Zero Latency**: Interactions are immediate as they don't await server round-trips.
*   **Privacy by Design**: Data lives on the user's device, not in a central cloud database.
*   **Cost Efficiency**: Eliminates the need for expensive always-on backend servers for a portfolio demonstration.

### The Stack
I built the PWA using a modern, performance-oriented stack:

*   **Core**: React 18, TypeScript, Vite.
*   **State & Logic**: Zustand (State Management), React Router v7.
*   **Persistence**: Dexie.js (IndexedDB wrapper) for a robust client-side SQL-like database.
*   **UI/UX**: Tailwind CSS with a custom "Cyberpunk/Neon" aesthetic, Framer Motion for orchestral animations.
*   **AI Engine**: Google Gemini API (Flash 1.5) integrated via a custom RAG (Retrieval-Augmented Generation) pipeline running locally.

### Key Features
*   **Client-Side RAG**: Users upload PDFs, and I use `pdf.js` (in Web Workers) to extract text and feed it into the Gemini context window entirely within the browser session.
*   **Role-Based AI**: The AI adapts its persona based on the logged-in user (Didactic planner for Teachers vs. Study guide for Students).
*   **Smart Fallbacks**: Includes Tesseract.js for OCR when PDFs are image-based.

### How to Run (PWA)
1.  **Install Dependencies**:
    ```bash
    npm install
    ```
2.  **Start Development Server**:
    ```bash
    npm run dev
    ```
3.  **Build for Production**:
    ```bash
    npm run build
    ```

---

## 📱 Legacy Archive: Aula Viva v1.0 (Android)

*Location: `/OLD-aulaviva-KOTLIN`*

This folder contains the original native implementation. I developed this version using **Kotlin** and **Jetpack Compose**, following clean architecture principles (MVVM).

### Legacy Stack highlights:
*   **Android**: Kotlin, Jetpack Compose, Room Database, Retrofit.
*   **Backend**: Spring Boot 3.2 (Java), PostgreSQL, deployed on Google Cloud Run.
*   **Features**: Native PDF rendering, biometric auth integration, and background sync.

*Note: This version is archived and no longer actively maintained.*

---

## 🔒 Security & Integrity

Even as a portfolio project, I implemented professional security standards:

*   **API Key Safety**: Users bring their own API keys (BYOK model), which are stored strictly in `localStorage` and never transmitted to my servers.
*   **Input Sanitization**: All AI outputs are sanitized to prevent XSS attacks.
*   **Strict Typing**: Comprehensive TypeScript interfaces ensure data integrity across the application state.

---

## 👨‍💻 Author

**Christopher Schiefelbein**
*Computer Engineering Student & Full Stack Developer*

I am passionate about building software that feels alive. Aula Viva is a testament to my belief that educational software should be as engaging and well-engineered as the best consumer apps.

---
*© 2025 Christopher Schiefelbein. All Rights Reserved.*
