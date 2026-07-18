import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import {
  AppUserDTO,
  AppUserForListDTO,
} from '../../interfaces/AppUserDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';
import { PaginationComponent } from '../../../../shared/components/PaginationComponent/PaginationComponent';
import {
  createPaginationState,
  PaginationState,
} from '../../../../core/utils/pagination.util';
import { UserService } from '../../services/user-service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [PaginationComponent],
  templateUrl: './user-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserList implements OnInit {
  private userService = inject(UserService);
  private notificationService = inject(NotificationService);

  users = signal<AppUserForListDTO[]>([]);
  loading = signal(true);
  paginationState: PaginationState = createPaginationState(20);
  private loadTrigger = signal(0);

  constructor() {
    effect(() => {
      this.loadTrigger();
      this.loadUsers();
    });
  }

  ngOnInit() {
    this.reloadUsers();
  }

  private loadUsers(): void {
    this.loading.set(true);

    const page = this.paginationState.currentPage();
    const size = this.paginationState.pageSize();

    this.userService.getListForWebsite(page, size).subscribe({
      next: (response) => {
        this.users.set(response.content);
        this.paginationState.updateFromResponse(response);
        this.loading.set(false);
      },
      error: () => {
        this.notificationService.error('Error loading users');
        this.users.set([]);
        this.loading.set(false);
      },
    });
  }

  private reloadUsers(): void {
    this.loadTrigger.update((v) => v + 1);
  }

  onPageChange(newPage: number) {
    this.paginationState.goToPage(newPage);
    this.reloadUsers();
  }

  toggleUserStatus(user: AppUserForListDTO) {
    const previousState = user.enabled;
    user.enabled = !user.enabled;

    this.userService.update({ ...user }).subscribe({
      next: () => {
        this.notificationService.success('Status updated');
      },
      error: (err) => {
        user.enabled = previousState;
        this.notificationService.error('Failed to update status');
      },
    });
  }
}
