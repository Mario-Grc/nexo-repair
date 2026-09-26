import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, InputTextModule, PasswordModule, ButtonModule, CardModule],
  template: `
    <div class="login-screen">
      <p-card>
        <div class="login-brand">
          <h1>Nexo</h1>
        </div>
        <form [formGroup]="form" (ngSubmit)="login()" class="login-form">
          <label for="login-email"
            >Email<input id="login-email" pInputText formControlName="email" type="email" autocomplete="username"
          /></label>
          <label for="login-password"
            >Password<p-password
              id="login-password"
              formControlName="password"
              [feedback]="false"
              autocomplete="current-password"
              [style]="{ width: '100%' }"
              [inputStyle]="{ width: '100%' }"
            />
          </label>
          @if (errorMessage() != null) {
            <p class="error" role="alert">{{ errorMessage() }}</p>
          }
          <button pButton type="submit" label="Sign in" [disabled]="form.invalid || isSubmitting()" class="login-submit"></button>
        </form>
      </p-card>
    </div>
  `,
  styles: [
    `
      .login-screen {
        max-width: 400px;
        margin: 12vh auto;
        padding: 0 1rem;
      }
      .login-brand {
        text-align: center;
        margin-bottom: 1.25rem;
      }
      .login-brand h1 {
        margin: 0 0 0.25rem;
      }
      .login-form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
      .login-form label {
        display: flex;
        flex-direction: column;
        gap: 0.375rem;
        font-weight: 500;
      }
      .login-form input {
        width: 100%;
      }
      .login-form .error {
        margin: 0;
        font-size: 0.875rem;
        color: var(--p-red-500, #dc2626);
      }
      .login-form .login-submit {
        width: 100%;
      }
    `,
  ],
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  errorMessage = signal<string | null>(null);
  isSubmitting = signal(false);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  login(): void {
    if (this.form.invalid || this.isSubmitting()) return;
    const { email, password } = this.form.getRawValue();
    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.auth.login(email.trim(), password).subscribe({
      next: () => this.router.navigateByUrl('/tickets'),
      error: () => {
        this.isSubmitting.set(false);
        this.errorMessage.set('Incorrect email or password');
      },
    });
  }
}
