import Image from "next/image";
import { cn } from "@/lib/utils";

interface RoadCarProps {
  width: number;
  className?: string;
  style?: React.CSSProperties;
}

export function RoadCar({ width, className, style }: RoadCarProps) {
  const pole = Math.round(width * 0.24);
  const flagWidth = Math.round(width * 0.26);
  const flagHeight = Math.max(3, Math.round(width * 0.15));
  return (
    <span aria-hidden className={cn("road-car pointer-events-none absolute bottom-[2px] block", className)} style={{ width, ["--car" as string]: `${width}px`, ...style }}>
      <Image src="/images/ndzela-car.png" alt="" width={420} height={147} style={{ width, height: "auto" }} />
      <span className="absolute w-px bg-slate-700" style={{ left: "44%", bottom: "78%", height: pole }}>
        <span className="road-flag absolute right-px top-0" style={{ width: flagWidth, height: flagHeight }} />
      </span>
    </span>
  );
}
