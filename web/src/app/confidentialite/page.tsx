import type { Metadata } from "next";
import { privacy } from "@/features/legal/content/privacy";
import { LegalPage } from "@/features/legal/components/legal-page";

export const metadata: Metadata = {
  title: privacy.title,
  description: privacy.description,
  alternates: { canonical: "/confidentialite" },
};

export default function Page() {
  return <LegalPage document={privacy} />;
}
