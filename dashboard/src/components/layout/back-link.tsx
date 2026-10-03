import Link from "next/link";
import { ArrowLeft } from "lucide-react";

// The way back from a detail page to its list
export function BackLink({ href, label }: { href: string; label: string }) {
  return (
    <Link href={href} className="inline-flex items-center gap-1.5 rounded text-sm font-medium text-muted transition-colors hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
      <ArrowLeft className="h-4 w-4" /> {label}
    </Link>
  );
}
