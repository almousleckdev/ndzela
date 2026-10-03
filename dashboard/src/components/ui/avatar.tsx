import Image from "next/image";
import { cn } from "@/lib/utils";
import { mediaUrl } from "@/lib/media";

const SIZES = { sm: "h-8 w-8 text-xs", md: "h-10 w-10 text-sm", xl: "h-28 w-28 text-3xl" };
const SIZE_PX = { sm: 32, md: 40, xl: 112 };
const HUES = ["bg-orange-200 text-orange-900", "bg-indigo-200 text-indigo-900", "bg-teal-200 text-teal-900", "bg-rose-200 text-rose-900", "bg-emerald-200 text-emerald-900", "bg-violet-200 text-violet-900"];

interface AvatarProps {
  name: string;
  // Path returned by the API (e.g.
  src?: string | null;
  size?: keyof typeof SIZES;
  className?: string;
}

export function Avatar({ name, src, size = "md", className }: AvatarProps) {
  const url = mediaUrl(src ?? null);
  const initials = name.split(/[\s@.]+/).filter(Boolean).slice(0, 2).map((w) => w[0]?.toUpperCase()).join("");
  const hue = HUES[[...name].reduce((sum, c) => sum + c.charCodeAt(0), 0) % HUES.length];

  return (
    <span className={cn("relative inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full font-display font-semibold", SIZES[size], !url && hue, className)}>
      {url ? <Image src={url} alt="" width={SIZE_PX[size]} height={SIZE_PX[size]} unoptimized className="h-full w-full object-cover" /> : initials}
    </span>
  );
}
