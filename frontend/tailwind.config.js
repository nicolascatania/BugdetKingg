/** @type {import('tailwindcss').Config} */

/**
 * Every colour is exposed as a CSS custom property holding an "R G B" triplet
 * (see `src/styles.css`). Wrapping them in `rgb(var(--token) / <alpha-value>)`
 * keeps Tailwind's opacity modifiers working (`bg-surface/60`) while letting a
 * single markup tree render correctly in both the light and the dark theme:
 * flipping the `.dark` class on <html> swaps the variable values, not the classes.
 */
const token = (name) => `rgb(var(${name}) / <alpha-value>)`;

module.exports = {
  darkMode: "class",
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {
      colors: {
        /* Surfaces */
        app: token("--color-app"),
        surface: token("--color-surface"),
        "surface-2": token("--color-surface-2"),
        "surface-3": token("--color-surface-3"),
        line: token("--color-line"),
        "line-strong": token("--color-line-strong"),

        /* Content */
        body: token("--color-text"),
        muted: token("--color-text-muted"),
        subtle: token("--color-text-subtle"),

        /* Brand & semantics */
        brand: {
          DEFAULT: token("--color-brand"),
          strong: token("--color-brand-strong"),
          soft: token("--color-brand-soft"),
          contrast: token("--color-brand-contrast"),
        },
        accent: token("--color-accent"),
        positive: token("--color-positive"),
        negative: token("--color-negative"),
        warning: token("--color-warning"),
      },
      /* A bare `border` should use the theme line colour, not Tailwind's default gray. */
      borderColor: {
        DEFAULT: token("--color-line"),
      },
      /* Extra steps the design system leans on for tinted surfaces; the default
         scale jumps straight from 10 to 20, which is too coarse here. */
      opacity: {
        2: "0.02",
        4: "0.04",
        6: "0.06",
        8: "0.08",
        12: "0.12",
        15: "0.15",
        18: "0.18",
        35: "0.35",
        45: "0.45",
        55: "0.55",
        65: "0.65",
        85: "0.85",
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
        display: ["Sora", "Inter", "system-ui", "sans-serif"],
      },
      borderRadius: {
        xl: "0.875rem",
        "2xl": "1.125rem",
        "3xl": "1.5rem",
      },
      boxShadow: {
        /* Soft UI: softer than flat, clearer than neumorphism. */
        soft: "0 1px 2px rgb(var(--shadow-color) / 0.06), 0 4px 16px -4px rgb(var(--shadow-color) / 0.10)",
        lifted:
          "0 2px 4px rgb(var(--shadow-color) / 0.06), 0 12px 32px -8px rgb(var(--shadow-color) / 0.18)",
        glow: "0 0 0 1px rgb(var(--color-brand) / 0.25), 0 8px 32px -8px rgb(var(--color-brand) / 0.35)",
      },
      transitionTimingFunction: {
        spring: "cubic-bezier(0.34, 1.56, 0.64, 1)",
        smooth: "cubic-bezier(0.22, 1, 0.36, 1)",
      },
      keyframes: {
        "fade-up": {
          from: { opacity: "0", transform: "translate3d(0, 14px, 0)" },
          to: { opacity: "1", transform: "translate3d(0, 0, 0)" },
        },
        "fade-in": {
          from: { opacity: "0" },
          to: { opacity: "1" },
        },
        "scale-in": {
          from: { opacity: "0", transform: "scale(0.96) translate3d(0, 8px, 0)" },
          to: { opacity: "1", transform: "scale(1) translate3d(0, 0, 0)" },
        },
        "slide-in-left": {
          from: { opacity: "0", transform: "translate3d(-16px, 0, 0)" },
          to: { opacity: "1", transform: "translate3d(0, 0, 0)" },
        },
        shimmer: {
          "100%": { transform: "translate3d(100%, 0, 0)" },
        },
        float: {
          "0%, 100%": { transform: "translate3d(0, 0, 0)" },
          "50%": { transform: "translate3d(18px, -28px, 0)" },
        },
        "aurora-pan": {
          "0%, 100%": { transform: "translate3d(-6%, 0, 0) scale(1)" },
          "50%": { transform: "translate3d(6%, -4%, 0) scale(1.12)" },
        },
        "pulse-ring": {
          "0%": { opacity: "0.6", transform: "scale(0.9)" },
          "70%, 100%": { opacity: "0", transform: "scale(1.9)" },
        },
      },
      animation: {
        "fade-up": "fade-up 480ms cubic-bezier(0.22, 1, 0.36, 1) both",
        "fade-in": "fade-in 320ms ease-out both",
        "scale-in": "scale-in 260ms cubic-bezier(0.34, 1.56, 0.64, 1) both",
        "slide-in-left": "slide-in-left 320ms cubic-bezier(0.22, 1, 0.36, 1) both",
        float: "float 14s ease-in-out infinite",
        "aurora-pan": "aurora-pan 22s ease-in-out infinite",
        "pulse-ring": "pulse-ring 2.4s cubic-bezier(0.22, 1, 0.36, 1) infinite",
      },
    },
  },
  plugins: [],
};
