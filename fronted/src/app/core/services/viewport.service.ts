import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ViewportService {
  readonly isMobile = signal(false);

  constructor() {
    const query = window.matchMedia('(max-width: 767.98px)');
    this.isMobile.set(query.matches);
    query.addEventListener('change', event => this.isMobile.set(event.matches));
  }
}
