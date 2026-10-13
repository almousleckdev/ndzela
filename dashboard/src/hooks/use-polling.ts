"use client";

import { useEffect, useRef } from "react";

// Calls `tick` every `ms` while the page is visible, and once more when the tab becomes visible again.
export function usePolling(tick: () => void, ms: number): void {
  const latest = useRef(tick);
  useEffect(() => {
    latest.current = tick;
  });
  useEffect(() => {
    const run = () => {
      if (document.visibilityState === "visible") latest.current();
    };
    const timer = window.setInterval(run, ms);
    document.addEventListener("visibilitychange", run);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", run);
    };
  }, [ms]);
}
