import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'side-bar',
  imports: [RouterLinkActive, RouterLink],
  templateUrl: './side-bar.html',
  styleUrl: './side-bar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SideBar {

  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  /**
   * Logs out the current user and redirects to login page.
   */
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
