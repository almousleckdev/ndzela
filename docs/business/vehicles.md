# Vehicles (company cars)

**Status: Decided from the product owner's description (2026-09-30). Not built.** The company owns every car; a car is registered once, then assigned to a driver through a contract.

## Adding a car: fields

`*` = required as stated by the product owner.

| Field (UI label, French) | Notes |
| --- | --- |
| Numéro du véhicule * | Internal fleet number (sample contract: `NDZELA V-037`). **Unique** (per country). |
| Marque * | Brand (e.g. Dongfeng). Required (Decided). |
| Modèle * | |
| Année * | Model year. Range validation **Open**. |
| Immatriculation * | Registration plate (sample: `MA-937-AA`). **Unique** (per country). |
| Couleur * | |
| Prix d'achat (FCFA) * | Purchase price; money type, never floating point ([database](../database/database.md)). |
| Loyer journalier (FCFA) * | Daily rent of this car (sample: 23 000). **A contract cannot override it.** |
| N° de série (VIN) | **Added** (Decided): optional, printed in the contract ("Non renseigné" when empty). |

At the end of the form a **Récapitulatif** shows all the entered details before saving.

## Rules

- A car belongs to one country (tenant-owned) and can only be assigned to a driver of the same country.
- Only cars whose status is `AVAILABLE` (not assigned, not out of service) appear in the driver wizard's "Véhicule à assigner" list.
- A car is assigned to **at most one active contract at a time**, and a driver has at most one contract (Decided).
- **Statuses (Decided enum):** `AVAILABLE`, `ASSIGNED`, `IN_MAINTENANCE`, `OUT_OF_SERVICE`. Assignment moves `AVAILABLE` to `ASSIGNED`; the end of a contract returns it to `AVAILABLE` or hands it over (ownership transfer, **Open**).
- The contract lists equipment supplied with the car (branding, LED roof screen, GPS, remote blocking, charging cable, charging access card); whether equipment is stored per car is **Open** (Proposed: a fixed equipment list in the country's contract template).
- Cars with contract history are never hard-deleted.

## Permissions (Proposed)

`vehicles:read`, `vehicles:create`, `vehicles:update`, `vehicles:delete`. **Gestionnaire** manages vehicles (Decided).
