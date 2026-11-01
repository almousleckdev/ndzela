import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { AdditionalPhonesField } from "@/features/users/components/additional-phones-field";

describe("AdditionalPhonesField", () => {
  it("adds an empty number", async () => {
    const onChange = vi.fn();
    render(<AdditionalPhonesField phones={["062143500"]} onChange={onChange} />);
    await userEvent.click(screen.getByRole("button", { name: /Ajouter un numéro/ }));
    expect(onChange).toHaveBeenCalledWith(["062143500", ""]);
  });

  it("removes the chosen number", async () => {
    const onChange = vi.fn();
    render(<AdditionalPhonesField phones={["111111", "222222"]} onChange={onChange} />);
    await userEvent.click(screen.getAllByRole("button", { name: "Retirer ce numéro" })[0]);
    expect(onChange).toHaveBeenCalledWith(["222222"]);
  });

  it("edits one number without touching the others", async () => {
    const onChange = vi.fn();
    render(<AdditionalPhonesField phones={["111111", "222222"]} onChange={onChange} />);
    await userEvent.type(screen.getAllByPlaceholderText("Numéro supplémentaire")[1], "3");
    expect(onChange).toHaveBeenLastCalledWith(["111111", "2222223"]);
  });

  it("stops offering to add at five numbers", () => {
    render(<AdditionalPhonesField phones={["1", "2", "3", "4", "5"]} onChange={() => {}} />);
    expect(screen.queryByRole("button", { name: /Ajouter un numéro/ })).not.toBeInTheDocument();
  });
});
