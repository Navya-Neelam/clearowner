import { HttpErrorResponse } from '@angular/common/http';
import { MonoTypeOperatorFunction, Observable, catchError, map, of, retry, startWith, timer } from 'rxjs';

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
      return 'Could not reach the ClearOwner API after several attempts. It may be offline.';
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

/**
 * The API is hosted on a free tier that suspends the instance after a period of
 * inactivity, and the first request afterwards fails while it starts up. Those
 * failures are transient and resolve on their own, so retry them with a widening
 * delay before deciding the request has genuinely failed.
 * <p>
 * Only failures that a retry could plausibly fix are retried. A 404 or a 400 is
 * reported immediately - repeating them would just make real errors slow.
 */
export function retryTransient<T>(): MonoTypeOperatorFunction<T> {
  const RETRY_DELAYS_MS = [2_000, 6_000, 12_000, 20_000];

  return retry<T>({
    count: RETRY_DELAYS_MS.length,
    delay: (error, retryCount) => {
      const transient =
        error instanceof HttpErrorResponse &&
        (error.status === 0 || error.status === 502 || error.status === 503 || error.status === 504);

      if (!transient) {
        throw error;
      }
      return timer(RETRY_DELAYS_MS[retryCount - 1]);
    },
  });
}

export function toState<T>(source: Observable<T>): Observable<AsyncState<T>> {
  return source.pipe(
    retryTransient(),
    map((data) => ({ loading: false, data })),
    startWith({ loading: true } as AsyncState<T>),
    catchError((error) => of({ loading: false, error: messageOf(error) })),
  );
}
