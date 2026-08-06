import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
} from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NgClass } from '@angular/common';
import { AuthService } from '../../../core/services/auth';
import { ThemeToggleComponent } from '../theme-toggle/theme-toggle';

/** A single entry of the primary navigation. */
interface NavItem {
  readonly label: string;
  readonly route: string;
  readonly icon: string;
  /** Only match the route exactly (used for the root-like "Home" entry). */
  readonly exact?: boolean;
  /** Restricts the entry to administrators. */
  readonly adminOnly?: boolean;
}

@Component({
  selector: 'side-bar',
  standalone: true,
  imports: [RouterLinkActive, RouterLink, MatTooltipModule, NgClass, ThemeToggleComponent],
  templateUrl: './side-bar.html',
  styleUrl: './side-bar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SideBar {
  /** Expanded (labels visible) vs. collapsed (icons only) on desktop. */
  readonly expanded = input.required<boolean>();

  /** Whether the off-canvas drawer is showing on small screens. */
  readonly mobileOpen = input<boolean>(false);

  readonly toggleExpandedEvent = output<void>();
  readonly closeMobileEvent = output<void>();

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  /** Navigation model — keeps the template free of repeated markup. */
  private readonly allItems: readonly NavItem[] = [
    { label: 'Home', route: '/home', icon: 'fa-house', exact: true },
    { label: 'Transactions', route: '/transactions', icon: 'fa-arrow-right-arrow-left' },
    { label: 'Dashboard', route: '/dashboard', icon: 'fa-chart-line' },
    { label: 'Accounts', route: '/accounts', icon: 'fa-wallet' },
    { label: 'Categories', route: '/categories', icon: 'fa-tags' },
    { label: 'Users', route: '/users', icon: 'fa-users', adminOnly: true },
  ];

  /** Entries the current user is allowed to see. */
  readonly navItems = computed(() =>
    this.allItems.filter((item) => !item.adminOnly || this.isAdmin),
  );

  toggleExpanded(): void {
    this.toggleExpandedEvent.emit();
  }

  closeMobile(): void {
    this.closeMobileEvent.emit();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }
}
