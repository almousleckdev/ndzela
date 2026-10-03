"use client";

import { useEffect, useState } from "react";
import { CircleCheck, Loader2 } from "lucide-react";

interface SendingScreenProps {
  title: string;
  message: string;
  // Called once the wait is over, to bring the form back.
  onDone: () => void;
  seconds?: number;
}

// Shown after a successful send
export function SendingScreen({ title, message, onDone, seconds = 10 }: SendingScreenProps) {
  const [left, setLeft] = useState(seconds);

  useEffect(() => {
    const timer = setInterval(() => setLeft((value) => value - 1), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (left <= 0) onDone();
  }, [left, onDone]);

  return (
    <div role="status" className="rounded-3xl bg-sky p-10 text-center">
      <CircleCheck className="mx-auto h-12 w-12 text-action" aria-hidden />
      <h2 className="mt-5 font-display text-3xl font-bold">{title}</h2>
      <p className="mx-auto mt-3 max-w-md text-muted">{message}</p>
      <div className="mx-auto mt-8 h-1.5 max-w-xs overflow-hidden rounded-full bg-ink/10" aria-hidden>
        <div className="h-full origin-left rounded-full bg-action" style={{ animation: `shrink ${seconds}s linear forwards` }} />
      </div>
      <p className="mt-3 inline-flex items-center gap-2 text-sm text-muted"><Loader2 className="h-4 w-4 animate-spin" aria-hidden />Retour au formulaire dans {Math.max(left, 0)} s</p>
    </div>
  );
}
