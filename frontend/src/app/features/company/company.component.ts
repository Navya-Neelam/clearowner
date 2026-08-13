import { AsyncPipe, TitleCasePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, map, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AsyncState, toState } from '../../core/async-state';
import {
  BeneficialOwner,
  CompanyDetail,
  DirectOwner,
  Directorship,
  RiskSignals,
} from '../../core/models';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state.component';
import { PctBarComponent } from '../../shared/pct-bar.component';
import { SkeletonComponent } from '../../shared/skeleton.component';

@Component({
  selector: 'co-company',
  standalone: true,
  imports: [
    AsyncPipe,
    TitleCasePipe,
    RouterLink,
    SkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent,
    PctBarComponent,
  ],
  templateUrl: './company.component.html',
  styleUrl: './company.component.scss',
})
export class CompanyComponent {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  /** 25% is the EU AMLD / FATF reporting threshold; the others let an analyst look deeper. */
  readonly thresholds = [25, 10, 1];
  private readonly threshold$ = new BehaviorSubject<number>(25);
  readonly selectedThreshold$ = this.threshold$.asObservable();

  private readonly id$: Observable<string> = this.route.paramMap.pipe(
    map((params) => params.get('id') ?? ''),
  );

  readonly company$: Observable<AsyncState<CompanyDetail>> = this.id$.pipe(
    switchMap((id) => toState(this.api.company(id))),
  );

  readonly directOwners$: Observable<AsyncState<DirectOwner[]>> = this.id$.pipe(
    switchMap((id) => toState(this.api.directOwners(id))),
  );

  readonly beneficialOwners$: Observable<AsyncState<BeneficialOwner[]>> = combineLatest([
    this.id$,
    this.threshold$,
  ]).pipe(switchMap(([id, threshold]) => toState(this.api.beneficialOwners(id, 6, threshold))));

  readonly signals$: Observable<AsyncState<RiskSignals>> = this.id$.pipe(
    switchMap((id) => toState(this.api.riskSignals(id))),
  );

  readonly directors$: Observable<AsyncState<Directorship[]>> = this.id$.pipe(
    switchMap((id) => toState(this.api.companyDirectors(id))),
  );

  setThreshold(value: number): void {
    this.threshold$.next(value);
  }
}
