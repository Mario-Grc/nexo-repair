import { Component, computed, inject, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-nav-links',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="nav-links">
      <a routerLink="/tickets" routerLinkActive="active" (click)="navigated.emit()">Tickets</a>
      <a routerLink="/customers" routerLinkActive="active" (click)="navigated.emit()">Customers</a>
      @if (isAdmin()) {
        <a routerLink="/employees" routerLinkActive="active" (click)="navigated.emit()">Employees</a>
      }
    </nav>
  `,
  styles: [
    `
      .nav-links {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        flex: 1;
      }
      .nav-links a {
        padding: 0.5rem 0.75rem;
        border-radius: 6px;
        text-decoration: none;
        color: inherit;
        min-height: 44px;
        display: flex;
        align-items: center;
      }
      .nav-links a:hover {
        background: var(--nexo-border, #d9d5c8);
      }
      .nav-links a.active {
        background: var(--p-primary-100, #eee);
        font-weight: 600;
      }
    `,
  ],
})
export class NavLinksComponent {
  private auth = inject(AuthService);

  isAdmin = computed(() => this.auth.current()?.role === 'ADMIN');

  navigated = output<void>();
}
