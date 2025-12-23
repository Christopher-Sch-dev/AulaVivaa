/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Much Darker Base
        background: '#050409', // Almost pure black with hint of purple
        surface: '#0F0E16',    // Dark grey-violet

        // Vibrant Neon Accents (High Contrast)
        primary: '#7C3AED',    // Deep Violet
        primaryGlow: '#8B5CF6',

        secondary: '#0D9488',  // Darker Teal
        accent: '#E11D48',     // Deep Rose

        // Text Levels
        text: '#F8FAFC',       // Slate 50 (White-ish)
        muted: '#94A3B8',      // Slate 400
      },
      fontFamily: {
        mono: ['"JetBrains Mono"', '"Fira Code"', 'monospace'],
        sans: ['"Inter"', 'sans-serif'],
      },
      animation: {
        'glitch': 'glitch 1s linear infinite',
        'pulse-slow': 'pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
      keyframes: {
        glitch: {
          '2%, 64%': { transform: 'translate(2px,0) skew(0deg)' },
          '4%, 60%': { transform: 'translate(-2px,0) skew(0deg)' },
          '62%': { transform: 'translate(0,0) skew(5deg)' },
        }
      }
    },
  },
  plugins: [
    require('@tailwindcss/typography'),
  ],
}
