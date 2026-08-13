import { AsyncPipe, DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AsyncState, toState } from '../../core/async-state';
import { DashboardSummary } from '../../core/models';
import { ErrorStateComponent } from '../../shared/error-state.component';
import { SkeletonComponent } from '../../shared/skeleton.component';

@Component({
  selector: 'co-dashboard',
  standalone: true,
  imports: [AsyncPipe, DecimalPipe, RouterLink, SkeletonComponent, ErrorStateComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly api = inject(ApiService);

  state$: Observable<AsyncState<DashboardSummary>> = toState(this.api.summary());

  reload(): void {
    this.state$ = toState(this.api.summary());
  }
}
