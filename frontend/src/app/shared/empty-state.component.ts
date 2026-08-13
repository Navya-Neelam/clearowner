import { Component, Input } from '@angular/core';

@Component({
  selector: 'co-empty',
  standalone: true,
  template: `
    <div class="state">
      <span class="material-symbols-outlined">{{ icon }}</span>
      <h3>{{ title }}</h3>
      <p>{{ message }}</p>
    </div>
  `,
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() title = 'Nothing to show';
  @Input() message = '';
}
