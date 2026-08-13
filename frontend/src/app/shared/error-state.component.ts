import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'co-error',
  standalone: true,
  template: `
    <div class="state error">
      <span class="material-symbols-outlined">error</span>
      <h3>{{ title }}</h3>
      <p>{{ message }}</p>
      @if (retryable) {
        <button class="btn" style="margin-top:8px" (click)="retry.emit()">
          <span class="material-symbols-outlined">refresh</span> Try again
        </button>
      }
    </div>
  `,
})
export class ErrorStateComponent {
  @Input() title = 'Could not load this view';
  @Input() message = '';
  @Input() retryable = true;
  @Output() retry = new EventEmitter<void>();
}
