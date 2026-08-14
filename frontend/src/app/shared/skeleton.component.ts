import { Component, Input, OnDestroy, OnInit, signal } from '@angular/core';

/**
 * Placeholder rows sized like the content they stand in for.
 * <p>
 * If the wait runs long, an explanation appears. The API is hosted on a free
 * tier that suspends after inactivity and can take the better part of a minute
 * to start, and an unexplained wait of that length reads as a broken page.
 */
@Component({
  selector: 'co-skeleton',
  standalone: true,
  template: `
    <div class="wrap">
      @for (row of rows(); track $index) {
        <div class="skeleton" [style.height.px]="height" [style.width.%]="row"></div>
      }
      @if (slow()) {
        <p class="hint">
          Still loading — the demo API sleeps when idle and can take up to a minute
          to start. It stays fast once it is awake.
        </p>
      }
    </div>
  `,
  styles: [`
    .wrap { padding: 18px; display: flex; flex-direction: column; gap: 10px; }
    .hint {
      margin-top: 6px;
      font-size: 12.5px;
      color: var(--ink-500);
      max-width: 52ch;
      line-height: 1.5;
    }
  `],
})
export class SkeletonComponent implements OnInit, OnDestroy {
  @Input() count = 4;
  @Input() height = 14;

  /** How long to wait before admitting this is taking a while. */
  private static readonly SLOW_AFTER_MS = 4_000;

  readonly slow = signal(false);
  private timer?: ReturnType<typeof setTimeout>;

  // Varying widths read as text rather than as loading bars.
  private readonly widths = [92, 78, 85, 70, 88, 74];

  ngOnInit(): void {
    this.timer = setTimeout(() => this.slow.set(true), SkeletonComponent.SLOW_AFTER_MS);
  }

  ngOnDestroy(): void {
    clearTimeout(this.timer);
  }

  rows(): number[] {
    return Array.from({ length: this.count }, (_, i) => this.widths[i % this.widths.length]);
  }
}
