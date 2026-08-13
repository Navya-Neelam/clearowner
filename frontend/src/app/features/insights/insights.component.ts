import { AsyncPipe, DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AsyncState, toState } from '../../core/async-state';
import { AddressCluster, CircularStructure, TopController } from '../../core/models';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state.component';
import { SkeletonComponent } from '../../shared/skeleton.component';

@Component({
  selector: 'co-insights',
  standalone: true,
  imports: [
    AsyncPipe,
    DecimalPipe,
    RouterLink,
    SkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent,
  ],
  templateUrl: './insights.component.html',
  styleUrl: './insights.component.scss',
})
export class InsightsComponent {
  private readonly api = inject(ApiService);

  circular$: Observable<AsyncState<CircularStructure[]>> = toState(this.api.circularStructures(25));
  addresses$: Observable<AsyncState<AddressCluster[]>> = toState(this.api.sharedAddresses(4, 20));
  controllers$: Observable<AsyncState<TopController[]>> = toState(this.api.topControllers(40, 20));

  reloadCircular(): void {
    this.circular$ = toState(this.api.circularStructures(25));
  }
  reloadAddresses(): void {
    this.addresses$ = toState(this.api.sharedAddresses(4, 20));
  }
  reloadControllers(): void {
    this.controllers$ = toState(this.api.topControllers(40, 20));
  }
}
