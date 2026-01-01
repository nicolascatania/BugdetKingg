import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SideBar } from "./shared/components/side-bar/side-bar";
import { Home } from "./features/home/home/home";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SideBar, Home],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'frontend';
}
