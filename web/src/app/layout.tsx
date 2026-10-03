import type { Metadata } from "next";
import { Bricolage_Grotesque, IBM_Plex_Sans, Orbitron } from "next/font/google";
import { SiteFooter } from "@/components/layout/site-footer";
import { WhatsAppButton } from "@/components/ui/whatsapp-button";
import { SiteHeader } from "@/components/layout/site-header";
import { siteConfig } from "@/config/site";
import "./globals.css";

const body = IBM_Plex_Sans({ subsets: ["latin"], weight: ["400", "500", "600"] });
const wordmark = Orbitron({ subsets: ["latin"], variable: "--font-orbitron", weight: ["700"] });
const display = Bricolage_Grotesque({ subsets: ["latin"], variable: "--font-bricolage", weight: ["500", "600", "700"] });

export const metadata: Metadata = {
  metadataBase: new URL(siteConfig.url),
  title: { default: `${siteConfig.name} | ${siteConfig.tagline}`, template: `%s | ${siteConfig.name}` },
  description: siteConfig.description,
  openGraph: { images: ["/images/cars/car3.jpeg"], siteName: siteConfig.name, locale: siteConfig.locale, type: "website" },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="fr">
      <body className={`${body.className} ${display.variable} ${wordmark.variable} flex min-h-screen flex-col bg-canvas text-ink antialiased`}>
        <SiteHeader />
        <main className="flex-1">{children}</main>
        <SiteFooter />
        <WhatsAppButton />
      </body>
    </html>
  );
}
