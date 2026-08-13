import { HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, of, startWith } from 'rxjs';

/**
 * Every screen renders one of four things: loading, an error, an empty result,
 * or data. Wrapping requests in this shape means each template handles all four
 * rather than each component inventing its own flags.
 */
export interface AsyncState<T> {
  loading: boolean;
  data?: T;
  error?: string;
}

export function messageOf(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the ClearOwner API. It may still be starting up.';
    }
    if (error.status === 503) {
      return 'The graph database is not reachable right now. Please try again shortly.';
    }
    if (error.status === 404) {
      return 'That record does not exist in this dataset.';
    }
    return error.error?.message ?? `Request failed (${error.status}).`;
  }
  return 'Something went wrong loading this view.';
}

export function toState<T>(source: Observable<T>): Observable<AsyncState<T>> {
  return source.pipe(
    map((data) => ({ loading: false, data })),
    startWith({ loading: true } as AsyncState<T>),
    catchError((error) => of({ loading: false, error: messageOf(error) })),
  );
}
