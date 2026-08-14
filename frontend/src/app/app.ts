import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { ApiService } from './core/api.service';
import { SearchBoxComponent } from './shared/search-box.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, SearchBoxComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly api = inject(ApiService);

  /** Null while unknown, so the banner only appears once we have a definite failure. */
  readonly dbReachable = signal<boolean | null>(null);

  constructor() {
    this.api
      .health()
      .pipe(
        map((health) => health.databaseReachable),
        catchError(() => of(false)),
      )
      .subscribe((reachable) => this.dbReachable.set(reachable));
  }
}
