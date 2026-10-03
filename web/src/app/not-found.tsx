import { Container } from "@/components/ui/container";
import { ButtonLink } from "@/components/ui/button-link";

export default function NotFound() {
  return (
    <Container className="py-24 text-center">
      <h1 className="font-display text-3xl font-bold">Page introuvable</h1>
      <p className="mt-3 text-muted">Cette page n&apos;existe pas ou a été déplacée.</p>
      <ButtonLink href="/" className="mt-8">Retour à l&apos;accueil</ButtonLink>
    </Container>
  );
}
