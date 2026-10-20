import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { CountryAccessField } from "@/features/users/components/country-access-field";
import type { Country } from "@/features/auth/types";

const countries = [{ id: "ga", name: "Gabon" }, { id: "ml", name: "Mali" }] as Country[];
const props = { countries, isGlobal: false, onGlobalChange: () => {}, countryIds: ["ga"], onToggleCountry: () => {} };

describe("CountryAccessField", () => {
  it("lists the countries with the user's own ticked", () => {
    render(<CountryAccessField {...props} />);
    expect(screen.getByLabelText("Gabon")).toBeChecked();
    expect(screen.getByLabelText("Mali")).not.toBeChecked();
  });

  it("hides the country list for a global user", () => {
    render(<CountryAccessField {...props} isGlobal />);
    expect(screen.queryByLabelText("Gabon")).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Utilisateur global/)).toBeChecked();
  });

  it("reports which country was toggled and shows the error", async () => {
    const onToggleCountry = vi.fn();
    render(<CountryAccessField {...props} onToggleCountry={onToggleCountry} error="Choisissez au moins un pays" />);
    await userEvent.click(screen.getByLabelText("Mali"));
    expect(onToggleCountry).toHaveBeenCalledWith("ml");
    expect(screen.getByRole("alert")).toHaveTextContent("Choisissez au moins un pays");
  });
});
