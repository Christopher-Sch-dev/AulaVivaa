# Aula Viva: Identidad Visual y Stack Tecnológico

Este documento detalla los efectos visuales, librerías y géneros estéticos aplicados en el sistema **Aula Viva**.

## 1. Género Visual y Estética
El sistema adopta una estética denominada **"Cyber-Academic (Matrix Edition)"**. Combina elementos futuristas de alta tecnología con la sobriedad necesaria para un entorno educativo.

- **Estilo Principal:** Cyberpunk / Futurista / Terminal.
- **Modo Visual:** **Dark-First Enforced**. El sistema ignora el modo claro del dispositivo para mantener la inmersión y el contraste "Cyber".
- **Optimización:** Diseñado específicamente para pantallas **OLED**, utilizando negros puros (`#000000`) para maximizar el ahorro de batería y el contraste.

## 2. Paleta de Colores (Cyber-Academic Palette)
| Elemento | Color | Hexadecimal | Descripción |
| :--- | :--- | :--- | :--- |
| **Primario** | Terminal Green | `#00FF41` | Color de acción principal, inspirado en terminales clásicas. |
| **Secundario** | Matrix Dark Green | `#003B00` | Tonos profundos para fondos secundarios. |
| **Acentos** | Glitch Red / Blue | `#FF003C` / `#04D9FF` | Efectos visuales de "glitch" y estados de error/alerta. |
| **Fondo** | Cyber Black | `#000000` | Negro puro optimizado para OLED. |
| **Superficie** | Surface Dark | `#0A0A0A` | Gris ultra oscuro para tarjetas y contenedores. |
| **Texto Primario** | Off-white / Silver | `#E0E0E0` | Alta legibilidad sobre fondo negro. |
| **Texto Secundario** | Matrix Green | `#008F11` | Texto de apoyo con estética de código. |

## 3. Tipografía
La tipografía se divide en dos familias para equilibrar estilo y legibilidad:
- **Encabezados (Headers):** `Monospace`. Proporciona un aire técnico, futurista y de "consola de comandos".
- **Cuerpo (Body):** `Sans-Serif (Default)`. Prioriza la legibilidad absoluta para el contenido educativo y lecturas prolongadas.

## 4. Librerías Visuales y de UI
El proyecto utiliza un stack moderno basado en **Jetpack Compose**:

- **Jetpack Compose (Material 3):** El motor principal de la interfaz de usuario, utilizando los estándares más recientes de Google.
- **Material Icons Extended:** Amplio catálogo de iconos para una navegación intuitiva.
- **Coil (Compose):** Librería para la carga eficiente y asíncrona de imágenes.
- **Markwon:** Implementación profesional para renderizar contenido en **Markdown** (tablas, listas de tareas, imágenes, etc.) con estilo académico.
- **SwipeRefreshLayout:** Integración de gestos "pull-to-refresh" con estética Material.
- **PDFBox Android:** Motor para la visualización y manejo de documentos PDF dentro de la aplicación.

## 5. Efectos y Detalles Visuales
- **Gradients:** Uso de degradados entre `MatrixGreen` y `MatrixDarkGreen` para elementos destacados.
- **Transparencias:** Uso de `MatrixGreenAlpha` (33% de opacidad) para contenedores de énfasis.
- **Status Bar:** Sincronizada con el fondo oscuro para una experiencia "edge-to-edge" total.
