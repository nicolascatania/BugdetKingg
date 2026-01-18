import { ChangeDetectionStrategy, Component, input, output, signal, AfterViewInit, effect } from '@angular/core';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { FormGroup, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { CategoryDTO } from '../../interfaces/CategoryDTO.interface';
import { CategoryService } from '../../service/category-service';
import { NotificationService } from '../../../../core/services/NotificationService';

@Component({
  selector: 'app-edit-category',
  imports: [UiModalComponent, ReactiveFormsModule, CommonModule],
  templateUrl: './edit-category.html',
  styleUrl: './edit-category.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditCategory {

  category = input<CategoryDTO | null>(null);

  form: FormGroup;

  submitEvent = output<boolean>();

  constructor(
    private categoryService: CategoryService,
    private fb: FormBuilder,
    private ns: NotificationService
  ) {
    this.form = this.fb.group({
      id: [''],
      name: ['', [Validators.required, Validators.maxLength(100)]],
    });

    effect(() => {
      const cat = this.category();

      if (cat) {
        this.form.patchValue(cat);
      } else {
        this.form.reset();
      }
    });
  }

  submit() {
    this.categoryService.save(this.form.value).subscribe({
      next: () => {
        this.submitEvent.emit(true);
        this.ns.success('Category saved successfully');
      },
      error: (err) => {
        this.ns.error(err);
        this.submitEvent.emit(false);
      }
    });
  }


  close() {
    this.form.reset();
    this.submitEvent.emit(false);
  }
}
