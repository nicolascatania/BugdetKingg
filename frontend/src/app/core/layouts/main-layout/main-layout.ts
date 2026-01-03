import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SideBar } from '../../../shared/components/side-bar/side-bar';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-main-layout',
  imports: [SideBar, RouterOutlet],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayout { }
