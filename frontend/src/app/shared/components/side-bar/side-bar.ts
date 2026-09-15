import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NgClass } from '@angular/common';
import { AuthService } from '../../../core/services/auth';
import { ThemeToggleComponent } from '../theme-toggle/theme-toggle';
import { TranslocoDirective } from '@jsverse/transloco';
import { LanguageSwitcherComponent } from '../language-switcher/language-switcher';

/** A single entry of the primary navigation. */
interface NavItem {
  /** Translation key of the visible label (`nav.*`). */
  readonly labelKey: string;
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
  imports: [
    RouterLinkActive,
    RouterLink,
    MatTooltipModule,
    NgClass,
    ThemeToggleComponent,
    TranslocoDirective,
    LanguageSwitcherComponent,
  ],
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

  /** The signed-in user's profile, shown as a pill below the brand. */
  readonly currentUser = this.authService.currentUser;

  constructor() {
    this.authService.ensureCurrentUserLoaded();
  }

  /** Navigation model — keeps the template free of repeated markup. */
  private readonly allItems: readonly NavItem[] = [
    { labelKey: 'nav.home', route: '/home', icon: 'fa-house', exact: true },
    { labelKey: 'nav.transactions', route: '/transactions', icon: 'fa-arrow-right-arrow-left' },
    { labelKey: 'nav.recurring', route: '/recurring-transactions', icon: 'fa-rotate' },
    { labelKey: 'nav.budgets', route: '/budgets', icon: 'fa-chart-pie' },
    { labelKey: 'nav.savingsGoals', route: '/savings-goals', icon: 'fa-piggy-bank' },
    { labelKey: 'nav.dashboard', route: '/dashboard', icon: 'fa-chart-line' },
    { labelKey: 'nav.accounts', route: '/accounts', icon: 'fa-wallet' },
    { labelKey: 'nav.categories', route: '/categories', icon: 'fa-tags' },
    { labelKey: 'nav.users', route: '/users', icon: 'fa-users', adminOnly: true },
    { labelKey: 'nav.settings', route: '/settings', icon: 'fa-gear' },
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
    // AuthService reloads the app to drop every service's session state.
    this.authService.logout();
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  /** Initials shown when the user has no Google profile photo. */
  userInitials(): string {
    const user = this.currentUser();
    if (!user) return '';
    return `${user.name.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  }
}
