/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: '#0D0D0D', // Deep Black
        surface: '#1A1A1A',   // Dark Gray
        primary: '#00FF41',   // Neon Green (Matrix Style)
        secondary: '#008F11', // Darker Green
        accent: '#D00000',    // Red for errors/accents
        text: '#E0E0E0',      // Light Gray for text
      },
      fontFamily: {
        mono: ['"Courier New"', 'Courier', 'monospace'], // Matrix terminal feel
        sans: ['"Inter"', 'sans-serif'],
      },
      animation: {
        'glitch': 'glitch 1s linear infinite',
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
  plugins: [],
}
