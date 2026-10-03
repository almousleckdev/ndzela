"use client";

import { useCallback, useEffect, useState } from "react";

// Counts down once per second from `initialSeconds`; call `restart` to begin again.
export function useCountdown(initialSeconds: number) {
  const [remaining, setRemaining] = useState(initialSeconds);

  useEffect(() => {
    if (remaining <= 0) return;
    const timer = setTimeout(() => setRemaining((value) => value - 1), 1000);
    return () => clearTimeout(timer);
  }, [remaining]);

  const restart = useCallback((seconds: number) => setRemaining(seconds), []);
  return { remaining, restart };
}
