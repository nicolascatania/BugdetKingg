import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SideBar } from "./shared/components/side-bar/side-bar";
import { Home } from "./features/home/pages/home/home";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SideBar],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'frontend';
}
