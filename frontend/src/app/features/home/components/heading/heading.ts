import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'heading',
  imports: [],
  templateUrl: './heading.html',
  styleUrl: './heading.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Heading { }
