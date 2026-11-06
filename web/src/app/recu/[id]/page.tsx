import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ReceiptView } from "@/features/receipt/components/receipt-view";
import { fetchReceipt } from "@/features/receipt/services/receipt";

export const metadata: Metadata = {
  title: "Reçu de paiement",
  robots: { index: false, follow: false },
};

export default async function Page({ params }: PageProps<"/recu/[id]">) {
  const { id } = await params;
  const receipt = await fetchReceipt(id);
  if (!receipt) notFound();
  return <ReceiptView receipt={receipt} />;
}
