import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { AuthService } from '../core/services/auth.service';
import { ChangePasswordDialog } from '../features/auth/change-password-dialog/change-password-dialog';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonModule, ConfirmDialogModule, ChangePasswordDialog],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.css',
  providers: [ConfirmationService],
})
export class AppShellComponent {
  auth = inject(AuthService);
  private router = inject(Router);
  private confirm = inject(ConfirmationService);

  isAdmin = computed(() => this.auth.current()?.role === 'ADMIN');

  passwordDialogVisible = signal(false);

  openPasswordDialog(): void {
    this.passwordDialogVisible.set(true);
  }

  logout(): void {
    this.confirm.confirm({
      header: 'Log out',
      message: 'Are you sure you want to log out?',
      icon: 'pi pi-sign-out',
      acceptLabel: 'Log out',
      rejectLabel: 'Cancel',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.auth.logout().subscribe(() => this.router.navigateByUrl('/login')),
    });
  }
}
