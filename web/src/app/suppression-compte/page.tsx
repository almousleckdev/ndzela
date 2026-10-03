import type { Metadata } from "next";
import { accountDeletion } from "@/features/legal/content/account-deletion";
import { LegalPage } from "@/features/legal/components/legal-page";

export const metadata: Metadata = {
  title: accountDeletion.title,
  description: accountDeletion.description,
  alternates: { canonical: "/suppression-compte" },
};

export default function Page() {
  return <LegalPage document={accountDeletion} />;
}
