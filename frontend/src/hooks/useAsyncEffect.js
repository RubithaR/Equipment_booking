import { useEffect } from 'react';

/**
 * Run an async loader as an effect with automatic cancellation.
 * The handler receives a `cancelled()` predicate it should consult after each
 * `await` to skip stale state updates when the component has unmounted or
 * the deps have changed.
 *
 * The same handler can be invoked from event handlers without an argument —
 * the `isCancelled?.()` optional-chain in callers makes the guard a no-op.
 */
export function useAsyncEffect(handler, deps) {
  useEffect(() => {
    let cancelled = false;
    Promise.resolve(handler(() => cancelled)).catch((err) => {
      if (!cancelled) console.error('useAsyncEffect:', err);
    });
    return () => { cancelled = true; };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
}
