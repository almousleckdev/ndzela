import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { HolderKindBadge, holderKindOf } from "@/features/charging/components/holder-kind";
import type { ChargingSessionRow } from "@/features/charging/types";

const session = (holder: ChargingSessionRow["holder"]) => ({ holder }) as ChargingSessionRow;

describe("holder kind", () => {
  it("tells a NDZela driver's session from a public one by its holder", () => {
    expect(holderKindOf(session({ driverId: "d1", driverName: "Awa Nze", vehicleId: null, vehicle: null }))).toBe("DRIVER");
    expect(holderKindOf(session(null))).toBe("PUBLIC");
  });

  it("names the NDZela driver on the badge", () => {
    render(<HolderKindBadge kind="DRIVER" />);

    expect(screen.getByText("Chauffeur NDZela")).toBeInTheDocument();
  });

  it("names the public customer on the badge", () => {
    render(<HolderKindBadge kind="PUBLIC" />);

    expect(screen.getByText("Public")).toBeInTheDocument();
  });
});
