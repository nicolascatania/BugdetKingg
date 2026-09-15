import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** One external link in the footer's author column. */
interface SocialLink {
  readonly label: string;
  readonly href: string;
  readonly icon: string;
}

/**
 * Site-wide footer for the public screens (landing, terms, sign-in).
 *
 * Two variants share one template:
 * - `full` (default): brand block, product links and author links in columns.
 * - `compact`: a single row with the legal line and the author links, for the
 *   auth screens where a tall footer would push the card off-centre.
 *
 * Links are data so both variants render from the same list.
 */
@Component({
  selector: 'app-site-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './site-footer.html',
  // The host is a custom element (inline by default); it must span its parent for the layout classes to apply.
  styles: [':host { display: block; width: 100%; }'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SiteFooterComponent {
  /** `compact` collapses the columns into one row. */
  readonly variant = input<'full' | 'compact'>('full');

  readonly year = new Date().getFullYear();

  readonly repositoryUrl = 'https://github.com/nicolascatania/BugdetKingg';

  readonly socialLinks: readonly SocialLink[] = [
    { label: 'GitHub', href: 'https://github.com/nicolascatania', icon: 'fa-brands fa-github' },
    { label: 'Website', href: 'https://nicolascatania.dev/', icon: 'fa-solid fa-globe' },
    { label: 'LinkedIn', href: 'https://www.linkedin.com/in/ncatania1', icon: 'fa-brands fa-linkedin-in' },
  ];
}
