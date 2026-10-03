"use client";

import { Container } from "@/components/ui/container";

export default function ErrorPage({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <Container className="py-24 text-center">
      <h1 className="font-display text-3xl font-bold">Une erreur est survenue</h1>
      <p className="mt-3 text-muted">Réessayez dans un instant.</p>
      <button type="button" onClick={reset} className="mt-8 inline-flex h-11 items-center rounded-lg bg-action px-5 text-sm font-semibold text-white hover:bg-action-hover">Réessayer</button>
    </Container>
  );
}
