import Image from "next/image";
import Link from "next/link";
import { MobileMenu } from "@/components/layout/mobile-menu";
import { ButtonLink } from "@/components/ui/button-link";
import { Wordmark } from "@/components/ui/wordmark";
import { Container } from "@/components/ui/container";
import { siteConfig } from "@/config/site";

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-line bg-canvas/95 backdrop-blur">
      <Container className="relative flex h-16 items-center justify-between">
        <Link href="/" className="flex items-center gap-3" aria-label={`${siteConfig.name}, accueil`}>
          <Image src="/images/logo.png" alt="" width={44} height={44} priority className="h-11 w-auto" />
          <Wordmark className="text-xl" />
        </Link>
        <nav aria-label="Navigation principale" className="hidden md:block">
          <ul className="flex items-center gap-8 text-sm font-medium text-muted">
            {siteConfig.nav.map((item) => (
              <li key={item.href}><Link href={item.href} className="transition-colors hover:text-ink">{item.label}</Link></li>
            ))}
          </ul>
        </nav>
        <ButtonLink href="/devenir-chauffeur" className="hidden h-10 md:inline-flex">Devenir chauffeur partenaire</ButtonLink>
        <MobileMenu />
      </Container>
    </header>
  );
}
