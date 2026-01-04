import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

export interface MultiSelectOption {
  [key: string]: any;
}

@Component({
  selector: 'app-multiselect',
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './multiselect.html',
  styleUrl: './multiselect.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MultiSelectComponent implements AfterViewInit {

  @Input() options: MultiSelectOption[] = [];      // Lista de objetos
  @Input() labelField: string = 'name';           // Campo a mostrar
  @Input() valueField: string = 'id';             // Campo que identifica
  @Input() placeholder: string = 'Select items...';
  @Input() label?: string;
  @Output() selectedChange = new EventEmitter<MultiSelectOption[]>();

  selectedItems = signal<MultiSelectOption[]>([]);
  dropdownOpen = signal(false);
  searchTerm = signal('');

  ngAfterViewInit() { }

  toggleDropdown() {
    this.dropdownOpen.update(v => !v);
  }

  selectItem(item: MultiSelectOption) {
    const current = this.selectedItems();
    const exists = current.find(i => i[this.valueField] === item[this.valueField]);
    if (!exists) {
      this.selectedItems.set([...current, item]);
    } else {
      this.selectedItems.set(current.filter(i => i[this.valueField] !== item[this.valueField]));
    }
    this.selectedChange.emit(this.selectedItems());
  }

  removeItem(item: MultiSelectOption) {
    this.selectedItems.set(this.selectedItems().filter(i => i[this.valueField] !== item[this.valueField]));
    this.selectedChange.emit(this.selectedItems());
  }

  filteredOptions() {
    const term = this.searchTerm().toLowerCase();
    return this.options.filter(opt => opt[this.labelField].toLowerCase().includes(term));
  }

  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.multi-select-dropdown')) {
      this.dropdownOpen.set(false);
    }
  }

  isSelected(item: MultiSelectOption): boolean {
    return this.selectedItems().some(i => i[this.valueField] === item[this.valueField]);
  }

}