# Contracts

**Status: Decided (product owner's answers and the Gabon sample contract); a few items marked Proposed/Open. Not built. Focus country: Gabon.**
Reference sample: [samples/contract-gabon.pdf](samples/contract-gabon.pdf) (10 pages, French, **test data**, contract `NDZ-77836447`). Analysis below is derived from it.

## What a contract is

A **rental with ownership transfer**: NDZELA SARL (the *bailleur*) leases an electric car to a driver (the *locataire*) for a fixed number of **billable days**. If the driver pays all of them and respects the contract, **ownership of the car is transferred** to the driver. The car stays NDZELA's property during the contract. The car is for **VTC activity in Libreville only** under the NDZELA brand. A contract is created at the same time as the driver ([drivers.md](drivers.md)), and its PDF is generated right after successful creation.

Rules confirmed by the product owner: **one contract per driver and one driver per car** at a time; the **daily rent cannot be changed** per contract (it comes from the car); the contract text is **in French**; the PDF is generated **after the driver account is created successfully**.

## Data of the contract (step 4 of the wizard)

| Field | Notes |
| --- | --- |
| Date de début | **Always the creation date** (Decided). |
| Durée (jours) | **1 248 billable days** (Decided). |
| Loyer journalier (FCFA) | Comes from the vehicle (sample: **23 000 FCFA**). Not editable. |
| Prix total véhicule (FCFA) | Wizard field, **computed = rent x days = 28 704 000 FCFA**, read-only (the sample contract never prints it). |
| Caution (FCFA) | Required deposit (sample: **250 000 FCFA**). |
| Contract number | `NDZ-` (Gabon prefix) + 8 digits; unique per country. |

## Billing calendar (from the sample, Decided)

- **Rent is due Monday to Saturday inclusive. Sunday is a rest day and is not billed.**
- **Deadline: every day before 12:00 (noon) of the next day**, in the country's timezone. A rent unpaid after that is an **impayé**.
- Payment platform named in the contract: **my-ndzela** (the driver app / payment platform).
- **The electric charging cost is not included in the daily rent** (billed separately, see [ocpp-integration.md](ocpp-integration.md)).
- **End date** in the sample: 25 September 2030 for a 30 September 2026 start. This equals the **1 248th billable day (Mon-Sat), counting the first billable day as the day after the start date**. I will implement exactly this rule once confirmed.

## Decisions on the earlier conflicts (Decided, 2026-09-30)

1. **Duration = 1 248 billable days** (4 years x 52 weeks x 6 days, Monday to Saturday). End date = the 1 248th billable day, counting the first billable day as the day after the start date (matches the sample: 30 Sep 2026 to 25 Sep 2030).
2. **Deposit: the contract wins.** Returned at the end **only if all rents are paid and the car is returned in good condition** (art. 4.1); **kept by NDZELA** on breach and termination for fault (art. 14.2, and 4.1 after 3 unpaid days); **used for the ownership-transfer costs** when ownership transfers (art. 8.2).
3. **Only violations cancel ownership**, not a simple warning: the driver gets the car if all 1 248 days are paid and the contract was not terminated for a violation of art. 14.
4. **Start date is always the creation date (today)**; staff do not choose it.
5. **Contract number:** prefix is per country (**`NDZ-` for Gabon**) followed by 8 digits, **sequential per country** (Decided).
6. **Prix total véhicule = daily rent x billable days** = 23 000 x 1 248 = **28 704 000 FCFA**, computed and read-only (Decided). The car's own purchase price (Prix d'achat) is a separate figure.

## Récapitulatif financier (in the wizard and in the contract)

The sample's recap (art. 4.3) shows: **Caution versée**, **Loyer journalier**, **Nombre de jours**. The wizard shows the recap at the bottom of step 4: caution, daily rent, number of days, and the computed total and the end date.

## PDF generation (Decided in principle)

- Generated **server-side** after the driver is created; shown on the next page as a **printable** PDF; stored per country in access-controlled storage; regenerable from stored data and the **template version** used ([findings D4](../findings/architecture-findings.md)). PDF library **Open**.
- **One template per country** (Gabon now; Mali later with its own text). The Gabon sample structure:

| Part | Content | Data source |
| --- | --- | --- |
| Header | "NDZELA SARL - Mobilité Électrique, Libreville, Gabon", contract number | country legal entity, contract |
| Bailleur | Raison sociale, forme juridique, siège, **RCCM**, phone, email | **per-country legal entity** (Open: new country data) |
| Locataire | Nom, prénoms, date and place of birth, nationality, ID number, issue date and place, address, ville, phones, email | driver step 1 |
| Emergency contact | Nom, prénoms, phone, lien de parenté, address | driver step 2 |
| Préambule and licence declaration | Licence number, category, issue date | driver step 1 |
| Art. 1-2 | Object; vehicle table (marque, modèle, **N° véhicule**, immatriculation, couleur, **N° de série VIN**, année); equipment list (branding, LED roof screen, GPS, **remote blocking**, charging cable, NDZELA charging access card) | vehicle; equipment list per template |
| Art. 3 | Training (centre, date, **N° certificat**: prints "À compléter" when empty), licence, documents provided (licence, ID, proof of address < 3 months) | driver step 3 |
| Art. 4-5 | Deposit, daily rent, payment days Mon-Sat, Sunday rest, deadline noon, recap, duration, start and end dates, 6-day inactivity clause | contract |
| Art. 6-17 | Obligations, unpaid sanctions, ownership transfer, liability, insurance, geolocation, data protection (Gabonese law), force majeure, termination, **Gabonese law and Libreville commercial court**, annexes 1-6 | **country template text** |
| Signatures | Bailleur and locataire, "Lu et approuvé", mandatory **handwritten mention** box | template |

Fields **not** in the wizard but printed in the contract: **VIN** (prints "Non renseigné"), **justificatif de domicile**, the annex documents. See open questions.

## Lifecycle (Open)

Draft/active/completed/terminated/transferred; termination triggers listed in art. 14 (3 consecutive unpaid days, branding, non-VTC use, sub-letting, wilful damage, driving under influence, licence withdrawn, criminal conviction, harm to NDZELA image). The contract states the car is retrieved and rents already paid are **not refunded**. Contract terms become immutable once active; changes are dated amendments.

## Permissions (Proposed)

`contracts:read`, `contracts:create` (through the driver wizard: **gestionnaire**), `contracts:update`, `contracts:terminate`, `contracts:print`.
