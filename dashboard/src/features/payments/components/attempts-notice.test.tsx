import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AttemptsNotice } from "@/features/payments/components/attempts-notice";

describe("AttemptsNotice", () => {
  it("counts the failed and pending attempts and opens them on a click", () => {
    const onShow = vi.fn();
    render(<AttemptsNotice failed={5} pending={1} status="" onShow={onShow} />);

    fireEvent.click(screen.getByRole("button", { name: "5 tentatives échouées" }));
    fireEvent.click(screen.getByRole("button", { name: "1 en attente" }));

    expect(onShow).toHaveBeenNthCalledWith(1, "FAILED");
    expect(onShow).toHaveBeenNthCalledWith(2, "PENDING");
  });

  it("says nothing when there is no failed attempt", () => {
    const { container } = render(<AttemptsNotice failed={0} pending={0} status="" onShow={vi.fn()} />);

    expect(container).toBeEmptyDOMElement();
  });

  it("says nothing once the user chose another status", () => {
    const { container } = render(<AttemptsNotice failed={3} pending={0} status="ALL" onShow={vi.fn()} />);

    expect(container).toBeEmptyDOMElement();
  });
});
