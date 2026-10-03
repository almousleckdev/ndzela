"use client";

import { useState } from "react";
import Link from "next/link";
import { Menu, X } from "lucide-react";
import { ButtonLink } from "@/components/ui/button-link";
import { siteConfig } from "@/config/site";

// The navigation below the `md` breakpoint
export function MobileMenu() {
  const [open, setOpen] = useState(false);
  return (
    <div className="md:hidden">
      <button type="button" aria-expanded={open} aria-controls="mobile-menu" aria-label={open ? "Fermer le menu" : "Ouvrir le menu"} onClick={() => setOpen((value) => !value)}
        className="inline-flex h-10 w-10 items-center justify-center rounded-lg text-ink hover:bg-sky focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-action/40">
        {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
      </button>
      {open && (
        <nav id="mobile-menu" aria-label="Navigation mobile" className="absolute inset-x-0 top-full border-b border-line bg-canvas px-4 pb-6 pt-2 shadow-lg">
          <ul>
            {siteConfig.nav.map((item) => (
              <li key={item.href}><Link href={item.href} onClick={() => setOpen(false)} className="block border-b border-line py-3 text-base font-medium">{item.label}</Link></li>
            ))}
          </ul>
          <ButtonLink href="/devenir-chauffeur" onClick={() => setOpen(false)} className="mt-5 w-full">Devenir chauffeur partenaire</ButtonLink>
        </nav>
      )}
    </div>
  );
}
