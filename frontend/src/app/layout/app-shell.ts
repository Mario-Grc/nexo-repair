import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { DrawerModule } from 'primeng/drawer';
import { ToastModule } from 'primeng/toast';
import { AuthService } from '../core/services/auth.service';
import { ViewportService } from '../core/services/viewport.service';
import { ChangePasswordDialog } from '../features/auth/change-password-dialog/change-password-dialog';
import { NavLinksComponent } from './nav-links';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, ButtonModule, ConfirmDialogModule, DrawerModule, ToastModule, ChangePasswordDialog, NavLinksComponent],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.css',
  providers: [ConfirmationService],
})
export class AppShellComponent {
  auth = inject(AuthService);
  viewport = inject(ViewportService);
  private router = inject(Router);
  private confirm = inject(ConfirmationService);

  isAdmin = computed(() => this.auth.current()?.role === 'ADMIN');

  passwordDialogVisible = signal(false);
  drawerVisible = signal(false);

  openPasswordDialog(): void {
    this.drawerVisible.set(false);
    this.passwordDialogVisible.set(true);
  }

  openMenu(): void {
    this.drawerVisible.set(true);
  }

  closeMenu(): void {
    this.drawerVisible.set(false);
  }

  logout(): void {
    this.drawerVisible.set(false);
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
