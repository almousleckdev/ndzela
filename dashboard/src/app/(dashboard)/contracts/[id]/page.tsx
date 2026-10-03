import { ContractPage } from "@/features/contracts/components/contract-page";

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ContractPage id={id} />;
}
