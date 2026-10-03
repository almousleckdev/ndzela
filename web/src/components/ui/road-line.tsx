import { RoadCar } from "@/components/ui/road-car";
import { cn } from "@/lib/utils";

export function RoadLine({ className, driveSeconds = 7 }: { className?: string; driveSeconds?: number }) {
  return (
    <div aria-hidden className={cn("relative h-12 w-full overflow-hidden text-primary", className)}>
      <RoadCar width={64} style={{ animationDuration: `${driveSeconds}s` }} />
      <div className="road-dashes absolute inset-x-0 bottom-0 h-0.5 [--road-dash:14px] [--road-period:26px]" />
    </div>
  );
}
