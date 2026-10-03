import type { Metadata } from "next";
import { terms } from "@/features/legal/content/terms";
import { LegalPage } from "@/features/legal/components/legal-page";

export const metadata: Metadata = {
  title: terms.title,
  description: terms.description,
  alternates: { canonical: "/conditions" },
};

export default function Page() {
  return <LegalPage document={terms} />;
}
