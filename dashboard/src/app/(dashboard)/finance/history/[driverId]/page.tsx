import { DriverHistoryPage } from "@/features/payments/components/driver-history-page";

export default async function Page({ params }: { params: Promise<{ driverId: string }> }) {
  const { driverId } = await params;
  return <DriverHistoryPage driverId={driverId} />;
}
