import { Phone } from "lucide-react";
import { ButtonLink } from "@/components/ui/button-link";
import { Container } from "@/components/ui/container";
import { phoneHref, siteConfig } from "@/config/site";

export function ContactBand() {
  return (
    <section className="py-20 sm:py-28">
      <Container>
        <div className="rounded-3xl bg-sky px-6 py-14 text-center sm:px-12">
          <h2 className="mx-auto max-w-2xl font-display text-3xl font-bold tracking-tight text-balance sm:text-4xl">Prêt à rouler électrique avec Ndzela ?</h2>
          <p className="mx-auto mt-4 max-w-xl text-lg text-muted">Parlez à notre équipe : nous vous expliquons le programme et répondons à toutes vos questions.</p>
          <div className="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <ButtonLink href="/devenir-chauffeur" className="h-12 px-6 text-base">Devenir chauffeur partenaire</ButtonLink>
            <ButtonLink href="/contact" variant="secondary" className="h-12 px-6 text-base">Nous contacter</ButtonLink>
            <a href={phoneHref} className="inline-flex h-12 items-center gap-2 px-4 font-semibold text-ink hover:text-action"><Phone className="h-4 w-4" aria-hidden />{siteConfig.contact.phone}</a>
          </div>
        </div>
      </Container>
    </section>
  );
}
