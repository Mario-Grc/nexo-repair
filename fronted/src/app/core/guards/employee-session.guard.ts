import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { EmployeeSessionService } from '../services/employee-session.service';

export const employeeSessionGuard: CanActivateFn = () => {
  const session = inject(EmployeeSessionService);
  const router = inject(Router);
  return session.isLoggedIn ? true : router.parseUrl('/select-employee');
};