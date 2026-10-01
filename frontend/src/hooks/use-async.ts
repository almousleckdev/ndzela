"use client";

import { useCallback, useEffect, useState } from "react";

interface AsyncState<T> {
  data: T | null;
  error: unknown;
  isLoading: boolean;
}

/** Runs the loader on mount and whenever `deps` change; `reload()` runs it again. A stale answer is ignored. */
export function useAsync<T>(loader: () => Promise<T>, deps: readonly unknown[]) {
  const [state, setState] = useState<AsyncState<T>>({ data: null, error: null, isLoading: true });
  const [nonce, setNonce] = useState(0);

  useEffect(() => {
    let cancelled = false;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setState((current) => ({ ...current, isLoading: true }));
    loader().then(
      (data) => { if (!cancelled) setState({ data, error: null, isLoading: false }); },
      (error) => { if (!cancelled) setState((current) => ({ ...current, error, isLoading: false })); },
    );
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, nonce]);

  const reload = useCallback(() => setNonce((value) => value + 1), []);
  return { ...state, reload };
}
