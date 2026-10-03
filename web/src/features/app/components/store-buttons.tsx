"use client";

import { useState } from "react";
import { StoreButton, STORES, type StoreKey } from "@/components/ui/store-button";
import { siteConfig, whatsappHref } from "@/config/site";

interface StoreButtonsProps {
  className?: string;
  app?: "driver" | "charging";
}

const APPS = {
  driver: { links: siteConfig.app, name: "L'application" },
  charging: { links: siteConfig.chargingApp, name: "L'application de recharge" },
} as const;

export function StoreButtons({ className, app = "driver" }: StoreButtonsProps) {
  const { links, name } = APPS[app];
  const [soon, setSoon] = useState<StoreKey | null>(null);
  const toggle = (store: StoreKey) => setSoon((current) => (current === store ? null : store));
  return (
    <div>
      <div className={className ?? "flex flex-wrap gap-3"}>
        <StoreButton store="apple" href={links.appStore} onSoon={toggle} expanded={soon === "apple"} />
        <StoreButton store="google" href={links.googlePlay} onSoon={toggle} expanded={soon === "google"} />
      </div>
      {soon && (
        <p role="status" className="mt-3 max-w-sm rounded-xl bg-white px-4 py-3 text-sm leading-relaxed text-ink shadow-lg ring-1 ring-ink/10">
          {name} arrive bientôt sur {STORES[soon].name}. <a href={whatsappHref} target="_blank" rel="noopener noreferrer" className="font-semibold text-action hover:text-action-hover">Écrivez-nous sur WhatsApp</a> pour être prévenu dès sa sortie.
        </p>
      )}
    </div>
  );
}
