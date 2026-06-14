import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  effect,
  inject,
  computed,
  signal,
} from '@angular/core';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import {
  FormGroup,
  ReactiveFormsModule,
  FormBuilder,
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { CategoryDTO } from '../../interfaces/CategoryDTO.interface';
import { CategoryService } from '../../service/category-service';
import { NotificationService } from '../../../../core/services/NotificationService';
import { CATEGORY_ICONS } from '../../../icons/interfaces/iconsenum.interace';

@Component({
  selector: 'app-edit-category',
  standalone: true,
  imports: [UiModalComponent, ReactiveFormsModule, CommonModule],
  templateUrl: './edit-category.html',
  styleUrl: './edit-category.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditCategory {
  private categoryService = inject(CategoryService);
  private fb = inject(FormBuilder);
  private ns = inject(NotificationService);

  category = input<CategoryDTO | null>(null);
  submitEvent = output<boolean>();

  form: FormGroup;
  protected iconOptions = CATEGORY_ICONS;

  searchQuery = signal('');

  filteredIcons = computed(() => {
    const query = this.searchQuery().toLowerCase();
    return CATEGORY_ICONS.filter((icon) =>
      icon.label.toLowerCase().includes(query),
    );
  });

  constructor() {
    this.form = this.fb.group({
      id: [''],
      name: ['', [Validators.required, Validators.maxLength(100)]],
      icon: ['fa-tags', [Validators.required]],
    });

    effect(() => {
      const cat = this.category();
      if (cat) {
        this.form.patchValue({
          id: cat.id,
          name: cat.name,
          icon: cat.icon || 'fa-tags',
        });
      } else {
        this.form.reset({ icon: 'fa-tags' });
      }
    });
  }

  submit() {
    if (this.form.invalid) return;

    this.categoryService.save(this.form.value).subscribe({
      next: () => {
        this.submitEvent.emit(true);
        this.ns.success('Category saved successfully');
      },
      error: (err) => {
        this.ns.error(err?.error?.message ?? 'Error saving category');
        this.submitEvent.emit(false);
      },
    });
  }

  close() {
    this.form.reset({ icon: 'fa-tags' });
    this.submitEvent.emit(false);
  }
}
