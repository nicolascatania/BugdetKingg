import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth';
import { RevealDirective } from '../../../../shared/directives/reveal.directive';

/** One tile in the features grid. */
interface Feature {
  readonly icon: string;
  readonly title: string;
  readonly description: string;
}

/** One numbered step in the "how it works" strip. */
interface Step {
  readonly title: string;
  readonly description: string;
}

/**
 * Public landing page: what the app does, how to start, and how personal data
 * is treated. Static copy only — every list is data so the template stays
 * a single loop per section.
 */
@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, RevealDirective],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Landing {
  private readonly auth = inject(AuthService);

  /** Signed-in visitors go straight to the app; everyone else to sign-in. */
  readonly ctaRoute = this.auth.isLoggedIn() ? '/home' : '/login';
  readonly ctaLabel = this.auth.isLoggedIn() ? 'Open the app' : 'Sign in with Google';

  readonly repositoryUrl = 'https://github.com/nicolascatania/BugdetKingg';

  /** Mirrors the sidebar's navigation, one tile per feature area. */
  readonly features: readonly Feature[] = [
    {
      icon: 'fa-wallet',
      title: 'Accounts',
      description:
        'Cash, bank, wallet, card — every place your money lives, each with its own balance and icon.',
    },
    {
      icon: 'fa-arrow-right-arrow-left',
      title: 'Transactions',
      description:
        'Income, expenses and transfers between accounts, with category, counterparty and notes. Filter and page through years of history.',
    },
    {
      icon: 'fa-rotate',
      title: 'Recurring moves',
      description:
        'Rent, salary, subscriptions. Set the frequency once, see what is coming up, and post each one with a click when it is due.',
    },
    {
      icon: 'fa-chart-pie',
      title: 'Monthly budgets',
      description:
        'A spending limit per category and month, with a progress bar that tells you how much room is left.',
    },
    {
      icon: 'fa-piggy-bank',
      title: 'Savings goals',
      description:
        'A target amount and date, contributions linked to a real account, and a status that flips to achieved on its own.',
    },
    {
      icon: 'fa-chart-line',
      title: 'Dashboard',
      description:
        'Income versus expenses per month, spending by category, and the official dollar and inflation figures for Argentina.',
    },
    {
      icon: 'fa-file-csv',
      title: 'CSV import & export',
      description:
        'Bring accounts, categories and transactions in from a spreadsheet, preview before committing, and export them back out any time.',
    },
    {
      icon: 'fa-moon',
      title: 'Light & dark, any screen',
      description:
        'One design system, both themes, phone to desktop. Tables become cards on small screens instead of scrolling sideways.',
    },
  ];

  readonly steps: readonly Step[] = [
    {
      title: 'Sign in with Google',
      description:
        'No form, no password to invent. Your account is created on the first sign-in.',
    },
    {
      title: 'Add your accounts and categories',
      description:
        'Or import them from a CSV. A few minutes and the app mirrors how you already think about your money.',
    },
    {
      title: 'Log moves, set limits, watch the dashboard',
      description:
        'Every transaction updates balances, budgets and goals immediately.',
    },
  ];
}
