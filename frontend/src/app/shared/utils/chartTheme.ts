/**
 * Chart.js colour helpers bound to the design-system tokens.
 *
 * Chart.js paints to a canvas, so it cannot inherit CSS custom properties the
 * way the DOM does. These helpers read the resolved token values off the
 * document root at render time, which means charts follow the active theme as
 * long as they are re-rendered when the theme changes.
 */

/** Reads a design token and returns it as a usable CSS colour string. */
function token(name: string, alpha = 1): string {
  if (typeof getComputedStyle === 'undefined') {
    return `rgb(0 0 0 / ${alpha})`;
  }
  const channels = getComputedStyle(document.documentElement)
    .getPropertyValue(name)
    .trim();
  return channels ? `rgb(${channels} / ${alpha})` : `rgb(0 0 0 / ${alpha})`;
}

/** Neutral chrome colours (grid, ticks, tooltips, canvas separators). */
export function chartTheme() {
  return {
    text: token('--color-text'),
    muted: token('--color-text-muted'),
    grid: token('--color-line', 0.7),
    surface: token('--color-surface'),
    border: token('--color-line'),
    positive: token('--color-positive'),
    negative: token('--color-negative'),
    brand: token('--color-brand'),
    accent: token('--color-accent'),
  };
}

/**
 * Categorical palette for slices/series.
 *
 * Ordered so that adjacent categories stay distinguishable, and kept to hues
 * that hold enough contrast against both the light and the dark surface.
 */
export function categoricalPalette(): string[] {
  const theme = chartTheme();
  return [
    theme.brand,
    theme.accent,
    token('--color-warning'),
    theme.negative,
    'rgb(99 102 241)',
    'rgb(236 72 153)',
    'rgb(14 165 233)',
    'rgb(168 85 247)',
  ];
}

/** Shared tooltip styling so every chart in the app reads the same way. */
export function chartTooltipStyle() {
  const theme = chartTheme();
  return {
    backgroundColor: theme.surface,
    titleColor: theme.text,
    bodyColor: theme.muted,
    borderColor: theme.border,
    borderWidth: 1,
    padding: 10,
    cornerRadius: 10,
    displayColors: true,
  };
}
