import type { Metadata } from "next";
import { Bricolage_Grotesque, Inter } from "next/font/google";
import "./globals.css";
import { Toaster } from "@/components/ui/toaster";

const inter = Inter({ subsets: ["latin"] });
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
    <html lang="en">
      <body className={`${inter.className} ${display.variable} flex h-screen overflow-hidden bg-slate-50 text-slate-900`}>
        {children}
        <Toaster />
      </body>
    </html>
  );
}
