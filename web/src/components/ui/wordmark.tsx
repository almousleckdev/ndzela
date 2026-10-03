import { cn } from "@/lib/utils";

export function Wordmark({ className }: { className?: string }) {
  return <span className={cn("font-wordmark font-bold uppercase leading-none tracking-[0.14em]", className)}>Ndzela</span>;
}
