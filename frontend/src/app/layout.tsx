import type { Metadata } from "next";
import { Bricolage_Grotesque, IBM_Plex_Sans } from "next/font/google";
import "./globals.css";
import { Toaster } from "@/components/ui/toaster";

const body = IBM_Plex_Sans({ subsets: ["latin"], weight: ["400", "500", "600"] });
const display = Bricolage_Grotesque({ subsets: ["latin"], variable: "--font-bricolage", weight: ["500", "600", "700"] });

export const metadata: Metadata = {
  title: "NDZela Admin",
  description: "Enterprise administration platform for NDZela",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr">
      <body className={`${body.className} ${display.variable} flex h-screen overflow-hidden bg-canvas text-ink antialiased`}>
        {children}
        <Toaster />
      </body>
    </html>
  );
}
