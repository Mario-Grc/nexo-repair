import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { EmployeeSessionService } from '../core/services/employee-session.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonModule],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.css'
})
export class AppShellComponent {
  session = inject(EmployeeSessionService);
  private router = inject(Router);

  isAdmin = computed(() => this.session.current()?.role === 'ADMIN');

  logout(): void {
    this.session.clear();
    this.router.navigateByUrl('/select-employee');
  }
}