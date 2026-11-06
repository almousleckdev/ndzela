import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ExportMenu } from "@/components/export-menu";
import { NO_RANGE } from "@/lib/date-range";

const downloadFile = vi.fn();
vi.mock("@/lib/download", () => ({ downloadFile: (...args: unknown[]) => downloadFile(...args) }));

const KINDS = [{ value: "ALL", label: "Les deux" }, { value: "DRIVER", label: "Chauffeurs NDZela" }, { value: "PUBLIC", label: "Public" }];

describe("ExportMenu with a period", () => {
  beforeEach(() => downloadFile.mockReset().mockResolvedValue(undefined));

  const open = () => {
    render(<ExportMenu range={NO_RANGE} option={{ label: "Type de sessions", options: KINDS, initial: "ALL" }} filename="sessions"
      failure="échec" path={(format, c) => `/x?format=${format}&from=${c.range.from}&to=${c.range.to}&kind=${c.option}`} />);
    fireEvent.click(screen.getByRole("button", { name: /Exporter/ }));
  };

  it("exports the quick period and the kind that were chosen", async () => {
    open();
    fireEvent.click(screen.getByRole("button", { name: "Hier" }));
    fireEvent.click(screen.getByRole("button", { name: "Public" }));
    fireEvent.click(screen.getByRole("button", { name: /PDF/ }));

    await waitFor(() => expect(downloadFile).toHaveBeenCalledTimes(1));
    const [path, name] = downloadFile.mock.calls[0] as [string, string];
    expect(path).toMatch(/^\/x\?format=PDF&from=\d{4}-\d{2}-\d{2}&to=\d{4}-\d{2}-\d{2}&kind=PUBLIC$/);
    expect(name).toMatch(/^sessions-\d{4}-\d{2}-\d{2}_\d{4}-\d{2}-\d{2}\.pdf$/);
  });

  it("exports any two dates typed by the user", async () => {
    open();
    fireEvent.change(screen.getByLabelText("Du"), { target: { value: "2026-11-05" } });
    fireEvent.change(screen.getByLabelText("Au"), { target: { value: "2026-11-06" } });
    fireEvent.click(screen.getByRole("button", { name: /Excel/ }));

    await waitFor(() => expect(downloadFile).toHaveBeenCalledWith("/x?format=XLSX&from=2026-11-05&to=2026-11-06&kind=ALL", "sessions-2026-11-05_2026-11-06.xlsx"));
  });
});
