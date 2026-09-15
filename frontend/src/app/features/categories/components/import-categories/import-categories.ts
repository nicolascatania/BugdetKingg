import { TranslocoService } from '@jsverse/transloco';
import { TranslocoDirective } from '@jsverse/transloco';
import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UiModalComponent } from '../../../../shared/modal/ui-modal/ui-modal';
import { CategoryImportExportService } from '../../services/category-import-export-service';
import { CategoryImportPreviewDTO } from '../../interfaces/CategoryImportPreviewDTO.interface';
import { NotificationService } from '../../../../core/services/NotificationService';

type Step = 'select' | 'preview' | 'done';

@Component({
  selector: 'app-import-categories',
  standalone: true,
  imports: [UiModalComponent, CommonModule, TranslocoDirective],
  templateUrl: './import-categories.html',
  styleUrl: './import-categories.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportCategories {
  private readonly transloco = inject(TranslocoService);

  private importExportService = inject(CategoryImportExportService);
  private ns = inject(NotificationService);

  closed = output<boolean>();

  step = signal<Step>('select');
  loading = signal(false);
  selectedFile = signal<File | null>(null);
  preview = signal<CategoryImportPreviewDTO | null>(null);

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile.set(file);
  }

  runPreview(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.loading.set(true);
    this.importExportService.previewImport(file).subscribe({
      next: (result) => {
        this.preview.set(result);
        this.step.set('preview');
        this.loading.set(false);
      },
      error: (err) => {
        this.ns.error(err?.error?.message ?? this.transloco.translate('csvImport.readError'));
        this.loading.set(false);
      },
    });
  }

  commit(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.loading.set(true);
    this.importExportService.commitImport(file).subscribe({
      next: (result) => {
        this.preview.set(result);
        this.step.set('done');
        this.loading.set(false);
        this.ns.success(this.transloco.translate('csvImport.categories.done', { count: result.validRows }));
      },
      error: (err) => {
        this.ns.error(err?.error?.message ?? this.transloco.translate('csvImport.categories.importError'));
        this.loading.set(false);
      },
    });
  }

  backToSelect(): void {
    this.step.set('select');
    this.preview.set(null);
  }

  close(): void {
    this.closed.emit(this.step() === 'done');
  }
}
