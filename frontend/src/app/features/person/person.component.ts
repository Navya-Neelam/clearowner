import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, map, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AsyncState, toState } from '../../core/async-state';
import { Directorship, Holding, PersonDetail } from '../../core/models';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state.component';
import { PctBarComponent } from '../../shared/pct-bar.component';
import { SkeletonComponent } from '../../shared/skeleton.component';

@Component({
  selector: 'co-person',
  standalone: true,
  imports: [
    AsyncPipe,
    RouterLink,
    SkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    PctBarComponent,
  ],
  templateUrl: './person.component.html',
  styleUrl: './person.component.scss',
})
export class PersonComponent {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  private readonly id$ = this.route.paramMap.pipe(map((p) => p.get('id') ?? ''));

  readonly person$: Observable<AsyncState<PersonDetail>> = this.id$.pipe(
    switchMap((id) => toState(this.api.person(id))),
  );

  readonly holdings$: Observable<AsyncState<Holding[]>> = this.id$.pipe(
    switchMap((id) => toState(this.api.personHoldings(id, 6, 1))),
  );

  readonly directorships$: Observable<AsyncState<Directorship[]>> = this.id$.pipe(
    switchMap((id) => toState(this.api.personDirectorships(id))),
  );
}
