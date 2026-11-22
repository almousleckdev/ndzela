import { describe, expect, it } from "vitest";
import { filterAgreements } from "@/features/free-charging/lib/filter-agreements";
import type { Agreement } from "@/features/free-charging/types";

const base: Agreement = {
  id: "a1", driverId: "d1", driverName: "NZE Paul", startsOn: "2026-11-01", endsOn: null, status: "ACTIVE", conditions: null,
  eligibility: "ELIGIBLE", unpaidDays: 0, lastFreeUse: null, phone: "+24177001122", email: "paul.nze@x.test", fleetNumber: "V015", plate: "PL-0150",
};
const awa: Agreement = { ...base, id: "a2", driverId: "d2", driverName: "OBIANG Awa", phone: "+24166002233", email: "awa@x.test", fleetNumber: "V030", plate: "PL-0300" };
const noCar: Agreement = { ...base, id: "a3", driverId: "d3", driverName: "MBA Jean", phone: null, email: null, fleetNumber: null, plate: null };
const rows = [base, awa, noCar];
const ids = (text: string) => filterAgreements(rows, text).map((row) => row.id);

describe("filterAgreements", () => {
  it("keeps every row for an empty search", () => {
    expect(ids("")).toEqual(["a1", "a2", "a3"]);
    expect(ids("   ")).toEqual(["a1", "a2", "a3"]);
  });

  it("finds by name in any order and case", () => {
    expect(ids("paul")).toEqual(["a1"]);
    expect(ids("AWA obiang")).toEqual(["a2"]);
  });

  it("finds by e-mail", () => {
    expect(ids("awa@x")).toEqual(["a2"]);
  });

  it("finds by phone whatever the spacing and prefix", () => {
    expect(ids("77001122")).toEqual(["a1"]);
    expect(ids("077 00 11 22")).toEqual(["a1"]);
    expect(ids("+241 66 00 22 33")).toEqual(["a2"]);
  });

  it("finds by fleet number or plate", () => {
    expect(ids("v015")).toEqual(["a1"]);
    expect(ids("V03")).toEqual(["a2"]);
    expect(ids("pl-0300")).toEqual(["a2"]);
  });

  it("copes with missing contact details and finds nothing for an unknown text", () => {
    expect(ids("mba")).toEqual(["a3"]);
    expect(ids("zzz")).toEqual([]);
  });
});
