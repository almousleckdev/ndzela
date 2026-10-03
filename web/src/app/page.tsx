import { AppShowcase } from "@/features/app/components/app-showcase";
import { ContactBand } from "@/features/home/components/contact-band";
import { DriverJourney } from "@/features/home/components/driver-journey";
import { Hero } from "@/features/home/components/hero";
import { Included } from "@/features/home/components/included";
import { Offers } from "@/features/home/components/offers";
import { Recharge } from "@/features/home/components/recharge";

export default function HomePage() {
  return (
    <>
      <Hero />
      <Offers />
      <DriverJourney />
      <Included />
      <AppShowcase />
      <Recharge />
      <ContactBand />
    </>
  );
}
