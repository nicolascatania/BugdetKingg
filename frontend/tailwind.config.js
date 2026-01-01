/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {
      colors: {
        primary: "#0f172a", // slate-900
        secondary: "#1e293b", // slate-800
        card: "#334155", // slate-700
        border: "#475569", // slate-600

        accent: {
          blue: "#3b82f6",
          green: "#10b981",
          red: "#f43f5e",
        },
      },
      fontFamily: {
        sans: ["Inter", "sans-serif"],
      },
    },
  },
  plugins: [],
};
