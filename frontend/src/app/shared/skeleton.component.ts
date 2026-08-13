import { Component, Input } from '@angular/core';

/** Placeholder rows sized like the content they stand in for. */
@Component({
  selector: 'co-skeleton',
  standalone: true,
  template: `
    <div style="padding:18px; display:flex; flex-direction:column; gap:10px">
      @for (row of rows(); track row) {
        <div class="skeleton" [style.height.px]="height" [style.width.%]="row"></div>
      }
    </div>
  `,
})
export class SkeletonComponent {
  @Input() count = 4;
  @Input() height = 14;

  // Varying widths read as text rather than as loading bars.
  private readonly widths = [92, 78, 85, 70, 88, 74];

  rows(): number[] {
    return Array.from({ length: this.count }, (_, i) => this.widths[i % this.widths.length]);
  }
}
