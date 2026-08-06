import {
  Directive,
  ElementRef,
  OnDestroy,
  OnInit,
  inject,
  input,
} from '@angular/core';

/**
 * Reveals an element the first time it scrolls into view.
 *
 * Adds the `reveal` class immediately (which hides the element and arms the
 * transition defined in `styles.css`) and swaps in `is-visible` once the
 * IntersectionObserver fires. The observer is disconnected after the first
 * reveal so scrolling back up never replays the animation.
 *
 * Degrades safely: if IntersectionObserver is unavailable, or the user asked
 * for reduced motion, the element is shown straight away.
 *
 * Usage: `<section appReveal [revealDelay]="120">`
 */
@Directive({
  selector: '[appReveal]',
  standalone: true,
})
export class RevealDirective implements OnInit, OnDestroy {
  /** Delay in milliseconds before this element animates in. */
  readonly revealDelay = input<number>(0);

  /** How much of the element must be visible before revealing (0-1). */
  readonly revealThreshold = input<number>(0.12);

  private readonly host = inject(ElementRef<HTMLElement>);
  private observer?: IntersectionObserver;

  ngOnInit(): void {
    const element = this.host.nativeElement as HTMLElement;

    const prefersReducedMotion =
      typeof window !== 'undefined' &&
      window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

    if (prefersReducedMotion || typeof IntersectionObserver === 'undefined') {
      element.classList.add('reveal', 'is-visible');
      return;
    }

    element.classList.add('reveal');
    element.style.setProperty('--reveal-delay', `${this.revealDelay()}ms`);

    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            element.classList.add('is-visible');
            this.disconnect();
          }
        }
      },
      { threshold: this.revealThreshold(), rootMargin: '0px 0px -40px 0px' },
    );

    this.observer.observe(element);
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  private disconnect(): void {
    this.observer?.disconnect();
    this.observer = undefined;
  }
}
