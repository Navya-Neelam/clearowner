import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, of, switchMap, catchError } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../core/api.service';
import { SearchResult } from '../core/models';

/**
 * Type-ahead over companies and people. Debounced so a burst of keystrokes
 * results in one request, and selecting a result routes straight to it.
 */
@Component({
  selector: 'co-search',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="wrap">
      <span class="material-symbols-outlined icon">search</span>
      <input
        #box
        type="search"
        [(ngModel)]="term"
        (ngModelChange)="onType($event)"
        (focus)="open.set(true)"
        [placeholder]="placeholder"
        autocomplete="off"
        aria-label="Search companies and people"
      />
      @if (open() && term.trim().length > 0) {
        <div class="panel">
          @if (loading()) {
            <div class="hint">Searching…</div>
          } @else if (results().length === 0) {
            <div class="hint">No company or person matches “{{ term }}”.</div>
          } @else {
            @for (r of results(); track r.type + r.id) {
              <button class="item" (click)="go(r)">
                <span class="material-symbols-outlined ico">
                  {{ r.type === 'Company' ? 'apartment' : 'person' }}
                </span>
                <span class="txt">
                  <span class="nm">{{ r.name }}</span>
                  <span class="sub">{{ r.subtitle || r.type }}</span>
                </span>
                <span class="mono id">{{ r.id }}</span>
              </button>
            }
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .wrap { position: relative; width: 100%; }
    .icon {
      position: absolute; left: 11px; top: 50%; transform: translateY(-50%);
      font-size: 19px; color: var(--ink-400); pointer-events: none;
    }
    input {
      width: 100%; padding: 9px 12px 9px 36px; font: inherit; font-size: 13.5px;
      border: 1px solid var(--ink-200); border-radius: var(--radius-sm);
      background: var(--surface); color: var(--ink-900); outline: none;
    }
    input:focus { border-color: var(--brand-600); box-shadow: 0 0 0 3px var(--brand-50); }
    input::-webkit-search-cancel-button { cursor: pointer; }
    .panel {
      position: absolute; top: calc(100% + 6px); left: 0; right: 0; z-index: 60;
      background: var(--surface); border: 1px solid var(--ink-200);
      border-radius: var(--radius); box-shadow: var(--shadow-md);
      max-height: 340px; overflow-y: auto; padding: 5px;
    }
    .hint { padding: 14px; color: var(--ink-500); font-size: 13px; }
    .item {
      display: flex; align-items: center; gap: 10px; width: 100%;
      padding: 8px 10px; background: none; border: 0; border-radius: var(--radius-sm);
      cursor: pointer; text-align: left; font: inherit;
    }
    .item:hover { background: var(--ink-100); }
    .ico { font-size: 18px; color: var(--ink-400); flex: none; }
    .txt { display: flex; flex-direction: column; min-width: 0; flex: 1; }
    .nm { font-weight: 500; font-size: 13.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .sub { font-size: 11.5px; color: var(--ink-500); }
    .id { color: var(--ink-400); flex: none; }
  `],
})
export class SearchBoxComponent {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly typed = new Subject<string>();

  readonly placeholder = 'Search a company or person…';
  readonly results = signal<SearchResult[]>([]);
  readonly loading = signal(false);
  readonly open = signal(false);
  term = '';

  constructor() {
    this.typed
      .pipe(
        debounceTime(220),
        distinctUntilChanged(),
        switchMap((q) => {
          if (q.trim().length < 2) {
            this.loading.set(false);
            return of<SearchResult[]>([]);
          }
          this.loading.set(true);
          return this.api.search(q.trim(), 8).pipe(catchError(() => of<SearchResult[]>([])));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((results) => {
        this.results.set(results);
        this.loading.set(false);
      });
  }

  onType(value: string): void {
    this.open.set(true);
    this.typed.next(value);
  }

  go(result: SearchResult): void {
    const path = result.type === 'Company' ? '/companies' : '/persons';
    this.router.navigate([path, result.id]);
    this.term = '';
    this.results.set([]);
    this.open.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }
}
