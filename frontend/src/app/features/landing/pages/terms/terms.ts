import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** One entry in the table of contents; `id` matches a section anchor in the template. */
interface TermsSection {
  readonly id: string;
  readonly title: string;
}

/**
 * Terms of use and privacy notice, one page with anchored sections.
 *
 * The copy describes what the code actually does: which Google claims are
 * read (`GoogleTokenInfoDTO`), what is persisted (`AppUser` and the entities
 * that hang off it), what lives in `localStorage`, and which third parties
 * are involved. Keep it in sync when any of those change.
 */
@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './terms.html',
  styleUrl: './terms.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Terms {
  /** Shown at the top; bump whenever the text changes. */
  readonly lastUpdated = 'September 14, 2026';

  readonly sections: readonly TermsSection[] = [
    { id: 'about', title: 'About this service' },
    { id: 'account', title: 'Your account' },
    { id: 'data-collected', title: 'What data we collect' },
    { id: 'data-use', title: 'How we use it' },
    { id: 'storage', title: 'Where it is stored' },
    { id: 'third-parties', title: 'Third-party services' },
    { id: 'browser-storage', title: 'Cookies and local storage' },
    { id: 'your-rights', title: 'Your rights' },
    { id: 'acceptable-use', title: 'Acceptable use' },
    { id: 'disclaimer', title: 'No warranty, no advice' },
    { id: 'changes', title: 'Changes to these terms' },
    { id: 'contact', title: 'Contact' },
  ];
}
