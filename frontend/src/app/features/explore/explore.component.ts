import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, filter, map, switchMap, take } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AsyncState, toState } from '../../core/async-state';
import { CompanyDetail, GraphView, SearchResult } from '../../core/models';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state.component';
import { SkeletonComponent } from '../../shared/skeleton.component';
import { GraphCanvasComponent } from './graph-canvas.component';

@Component({
  selector: 'co-explore',
  standalone: true,
  imports: [
    AsyncPipe,
    GraphCanvasComponent,
    SkeletonComponent,
    EmptyStateComponent,
    ErrorStateComponent,
  ],
  templateUrl: './explore.component.html',
  styleUrl: './explore.component.scss',
})
export class ExploreComponent {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly depths = [1, 2, 3, 4, 5, 6];
  private readonly depth$ = new BehaviorSubject<number>(3);
  readonly selectedDepth$ = this.depth$.asObservable();

  private readonly companyId$ = new BehaviorSubject<string>('');
  readonly selectedId$ = this.companyId$.asObservable();

  /** Suggested starting points so the screen is never an empty canvas. */
  readonly suggestions$: Observable<SearchResult[]> = this.api.search('Holdings', 6);

  readonly company$: Observable<AsyncState<CompanyDetail>> = this.companyId$.pipe(
    filter((id) => !!id),
    switchMap((id) => toState(this.api.company(id))),
  );

  readonly graph$: Observable<AsyncState<GraphView>> = combineLatest([
    this.companyId$.pipe(filter((id) => !!id)),
    this.depth$,
  ]).pipe(switchMap(([id, depth]) => toState(this.api.companyGraph(id, depth))));

  constructor() {
    this.route.queryParamMap.pipe(take(1), map((p) => p.get('company'))).subscribe((id) => {
      if (id) this.companyId$.next(id);
    });
  }

  select(id: string): void {
    this.companyId$.next(id);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { company: id },
      replaceUrl: true,
    });
  }

  setDepth(value: number): void {
    this.depth$.next(value);
  }

  onNode(event: { id: string; type: string }): void {
    if (event.type === 'Person') {
      this.router.navigate(['/persons', event.id]);
    } else {
      this.select(event.id);
    }
  }

  openCompany(id: string): void {
    this.router.navigate(['/companies', id]);
  }
}
