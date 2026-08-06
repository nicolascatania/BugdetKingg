import { ChangeDetectionStrategy, Component, HostListener, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SideBar } from '../../../shared/components/side-bar/side-bar';
import { ThemeToggleComponent } from '../../../shared/components/theme-toggle/theme-toggle';

/**
 * Application shell for authenticated routes.
 *
 * Keeps two independent pieces of navigation state:
 * - `sidebarExpanded`: desktop rail showing labels or icons only
 * - `mobileNavOpen`: off-canvas drawer on small screens
 */
@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [SideBar, RouterOutlet, ThemeToggleComponent],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayout {
  readonly sidebarExpanded = signal(true);
  readonly mobileNavOpen = signal(false);

  toggleSidebar(): void {
    this.sidebarExpanded.update((expanded) => !expanded);
  }

  toggleMobileNav(): void {
    this.mobileNavOpen.update((open) => !open);
  }

  closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }

  /** Escape closes the mobile drawer, matching standard dialog behaviour. */
  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeMobileNav();
  }
}
