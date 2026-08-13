import { DecimalPipe } from '@angular/common';
import { Component, Input } from '@angular/core';

/**
 * A percentage with a proportional bar. Anything at or above the 25% control
 * threshold is emphasised, so the significant stakes stand out when scanning.
 */
@Component({
  selector: 'co-pct',
  standalone: true,
  template: `
    <div class="pct">
      <div class="pct-track">
        <div class="pct-fill" [class.strong]="value >= 25" [style.width.%]="clamped"></div>
      </div>
      <span class="pct-value">{{ value | number: '1.2-2' }}%</span>
    </div>
  `,
  imports: [DecimalPipe],
})
export class PctBarComponent {
  @Input({ required: true }) value = 0;

  get clamped(): number {
    return Math.max(0, Math.min(100, this.value));
  }
}
