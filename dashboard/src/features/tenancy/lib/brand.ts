export interface Brand {
  // Sets the colour theme
  key: "ndzela" | "angata";
  name: string;
  logo: string;
}

const NDZELA: Brand = { key: "ndzela", name: "NDZela", logo: "/images/logo.png" };
const BRANDS: Record<string, Brand> = { GA: NDZELA, ML: { key: "angata", name: "Angata", logo: "/images/angatalogo.png" } };

// The company brand of a country (Gabon
export function brandOf(countryCode: string | null | undefined): Brand {
  return (countryCode && BRANDS[countryCode]) || NDZELA;
}
