import Image from "next/image";
import { cn } from "@/lib/utils";

const LOCAL_FLAGS = new Set(["ga", "ml"]);

function emojiFlag(code: string): string {
  return String.fromCodePoint(...[...code.toUpperCase()].map((letter) => 127397 + letter.charCodeAt(0)));
}

// The flag of a country
export function CountryFlag({ code, className }: { code: string; className?: string }) {
  const lower = code.toLowerCase();
  if (LOCAL_FLAGS.has(lower)) {
    return (
      <Image
        src={`/flags/${lower}.svg`}
        alt=""
        width={22}
        height={15}
        unoptimized
        className={cn("h-[15px] w-[22px] shrink-0 rounded-[2px] object-cover ring-1 ring-black/10", className)}
      />
    );
  }
  return <span aria-hidden="true" className={className}>{emojiFlag(code)}</span>;
}
