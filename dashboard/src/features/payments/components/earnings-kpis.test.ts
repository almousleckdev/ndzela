import { describe, expect, it } from "vitest";
import { formatFcfa } from "@/lib/money";
import { earningsKpis } from "@/features/payments/components/earnings-kpis";
import type { Earnings } from "@/features/payments/types";

const earnings: Earnings = { collected: 56000, fees: 500, net: 55500, payingDrivers: 2, collectedCurrent: 46000, collectedFormer: 10000, months: [] };

describe("earningsKpis", () => {
  it("names the five figures of the page", () => {
    expect(earningsKpis(earnings).map((kpi) => kpi.label)).toEqual(["Encaissé", "Frais opérateur", "Net", "Chauffeurs actuels", "Anciens chauffeurs"]);
  });

  it("shows the collected money, the fees and the net in FCFA", () => {
    const [collected, fees, net] = earningsKpis(earnings);

    expect(collected.value).toBe(formatFcfa(56000));
    expect(fees.value).toBe(formatFcfa(500));
    expect(net.value).toBe(formatFcfa(55500));
  });

  it("splits the money between current and former drivers", () => {
    const [, , , current, former] = earningsKpis(earnings);

    expect(current.value).toBe(formatFcfa(46000));
    expect(former.value).toBe(formatFcfa(10000));
  });

  it("shows dashes while the figures load", () => {
    expect(earningsKpis(null).every((kpi) => kpi.value === "-")).toBe(true);
  });
});
