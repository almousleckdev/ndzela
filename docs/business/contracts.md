# Contracts

**Status: Built for Gabon (2026-09-30)**: creation with the driver, recap, printable French PDF, termination and renewal on vehicle change. Lifecycle beyond active/terminated, amendments and ownership transfer remain open. Focus country: Gabon.
Reference sample: [samples/contract-gabon.pdf](samples/contract-gabon.pdf) (10 pages, French, **test data**, contract `NDZ-77836447`). Analysis below is derived from it.

## What a contract is

A **rental with ownership transfer**: NDZELA SARL (the *bailleur*) leases an electric car to a driver (the *locataire*) for a fixed number of **billable days**. If the driver pays all of them and respects the contract, **ownership of the car is transferred** to the driver. The car stays NDZELA's property during the contract. The car is for **VTC activity in Libreville only** under the NDZELA brand. A contract is created at the same time as the driver ([drivers.md](drivers.md)), and its PDF is generated right after successful creation.

Rules confirmed by the product owner: **one contract per driver and one driver per car** at a time; the **daily rent cannot be changed** per contract (it comes from the car); the contract text is **in French**; the PDF is generated **after the driver account is created successfully**.

## As built (2026-09-30)

- **Wizard step 4 "Contrat"**: the gestionnaire enters the **caution** (prefilled 250 000 FCFA, editable, whole FCFA); the server computes the recap (`POST /contracts/preview`: start today in the country's timezone, 1 248 billable days, end date, rent from the car, total = rent x days). After the password pop-up the driver and the contract are created **in one transaction** and the app opens `/contracts/{id}`: the PDF with **Imprimer** and **Télécharger**. The contract is also reachable from the driver page.
- **Tables (V14):** `contracts` (number `NDZ-` + 8 digits, sequential per country through a locked counter row `contract_counters`; partial unique indexes allow **one ACTIVE contract per driver and per vehicle**; financial terms; `snapshot` JSON of the parties as printed; `template_version`), and per-country legal-entity columns on `countries` (prefix, template, legal name and form, head office, RCCM, phone, email, signing city), seeded for Gabon from the sample. Mali has none: creating a Mali driver answers `CONTRACT_TEMPLATE_UNAVAILABLE` until Mali's text and entity data exist.
- **Consequences for drivers:** creating a driver **requires a vehicle and a caution**; removing the vehicle **terminates** the active contract (with the removal reason); assigning a vehicle to a driver without one **opens a new contract** (the dialog asks for the caution). A driver or a vehicle can be **deleted while no money is recorded** (see below); suspend, block or retire instead once a payment or recharge exists.
- **PDF:** [ADR-0010](../adr/0010-contract-pdf-with-openpdf-from-a-snapshot.md): OpenPDF, rendered on demand from the snapshot, audited (`CONTRACT_PRINTED`). Template `GA-1` follows the contract text of `documents/CONTRAT NDZELA .pdf` (2026-10-20: articles 1 to 18, annexes, signature boxes, handwritten mention). The sample's figures come from the contract, the parties from the snapshot; "N° de série" prints "Non renseigné" and "N° certificat" prints "À compléter" when empty; article 3.3 lists the documents actually uploaded.
- **Permissions (Implemented):** `contracts:read` (GESTIONNAIRE, ADMIN, COMPTABLE, VIEWER, SUPER_ADMIN), `contracts:print` (all but VIEWER); creation goes through `drivers:create` and the preview needs it too.
- **API:** `GET /contracts?driverId=`, `GET /contracts/{id}`, `GET /contracts/{id}/pdf`, `POST /contracts/preview`. Audit: `CONTRACT_CREATED`, `CONTRACT_TERMINATED`, `CONTRACT_PRINTED`.

## Updating a contract (Implemented, V17, 2026-10-01)

A contract can be **updated while it is active**: editing a driver (Chauffeurs, Modifier) now has a 4th step "Contrat" for a driver who has one; with "Mettre à jour le contrat avec ces informations" ticked (default) the contract is refreshed after the driver is saved, and the updated contract opens for printing. `PUT /drivers/{id}/contract {deposit}` (permission `contracts:update`: GESTIONNAIRE, ADMIN, SUPER_ADMIN) **replaces the parties printed in the contract with the driver's and the vehicle's current details and may change the deposit**; the **number, the start and end dates, the rent, the total price and the bailleur stay as signed**. Each update increments `revision`, records `updated_at` and `updated_by`, writes `CONTRACT_UPDATED` to the audit trail (revision, old and new deposit) and prints "Contrat mis à jour le ... (révision N)" on page 1. Editing a driver **without** ticking the box leaves the signed contract untouched. A terminated contract cannot be updated (`CONTRACT_NOT_ACTIVE`); a driver without an active contract answers `NO_ACTIVE_CONTRACT`. This refines the earlier rule that a signed contract never changes: it now changes only by this explicit, counted update.

## Changing the car and the start date (Implemented, 2026-11-12, [ADR-0034](../adr/0034-change-car-and-start-date-on-an-active-contract.md))

In "Modifier le chauffeur" (holders of `contracts:update`, driver with an active contract), staff can **change the car** (step 3) and **correct the start date** (step 4). The contract stays the same: number, rent, total price, deposit and dates are unchanged by a car change, even when the new car's rent differs; the printed vehicle and parties are refreshed and the revision counted. The start date can move (today to 90 days ahead, end date following) **only while no payment, waiver, fee, reversal or incident is recorded** on the contract. Endpoints `PUT /drivers/{id}/vehicle` and `PUT /drivers/{id}/contract/start-date`; audit `CONTRACT_VEHICLE_CHANGED`, `CONTRACT_RESCHEDULED`.

## Contract text of 2026-11-12

**3.3** lists "Casier judiciaire de moins de 3 mois" instead of "Justificatif de domicile de moins de 3 mois" (the third document uploaded in the wizard is the criminal record). **6** no longer requires "Effectuer en moyenne 3 recharges par semaine" (the 6 rides a day stay). Applies to every contract printed from now on, like the 2026-10-20 text.

## Contract text of 2026-10-20 (`documents/CONTRAT NDZELA .pdf`)

The PDF template (`ContractArticles` for the preamble and articles 1 to 5, `ContractClauses` for 6 to 18) now follows the updated contract. It applies to **every contract printed from now on, including those already created** (the text is rendered from the snapshot at print time; a frozen text per signed contract would need a new template version: ask before doing it). What changed:
- Figures say **"jours ouvrés"** (1 248 working days, 4 years x 52 weeks x 6 days) in articles 4, 5 and 8; the payment platform is written `My-ndzela`.
- **4.1** the deposit also covers "les frais de remise en état du branding" (the recharge credit is no longer mentioned). **2.2** the equipment list ends with the remote-blocking system (no charging cable or access card line).
- **6** new duties: no secondary activity other than driving NDZELA taxis; on average **3 recharges a week** and **6 rides a day** through the VTC app; be at the NDZELA office within **24 hours** when asked.
- **8.1** ownership transfers after the 4 years (1 248 days, Monday to Saturday) to a locataire who kept all engagements, financial ones above all.
- **9.3** damage is repaired in the garage approved by NDZELA. **13 (new)** conditions and reasons for remote blocking: public holidays and non-working days, illness or work stoppage, permissions and absences. Force majeure, termination, law, miscellaneous and annexes become **14 to 18**.
- **15.1** two new termination reasons: charging on sites other than NDZELA's, and any repair or maintenance outside the approved garage or without agreement. **18** annexes: driving licence, national ID, deposit receipt, **criminal record**.
- Editorial: duplicates merged (one "no secondary activity" duty), typos fixed ("Branting" is "branding"), the company stays written `Ndzela` as decided in V28.

## Tracking since day one (2026-10-20)

From the day a contract is signed (its start date) the system keeps, per contract: **every rent day** with its status (paid on time, paid late, unpaid) from day one, **every payment, fee, waiver and reversal** in the ledger, and a **progress summary** (`ContractProgress`): the 1 248 working days (elapsed, left, paid, unpaid), the total price, the money received (payments, net of reversals) and the amount left to pay, and the deposit. It is shown on the driver's page in the dashboard ("Suivi du contrat") and in the driver app (Accueil and Profil, "Mon contrat"), computed by the server (`GET /payments/accounts/{driverId}` and `GET /driver/contract`), never by the client.

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
- **Electricity cuts (2026-10-02):** an administrator declares them for a past billable day. A cut of 4 to 8 hours makes it a **half day** (rent / 2, rounded to the nearest franc); a cut of more than 8 hours is a **stop** (no rent that day). Another cause needs a note. One live incident per contract and day; it can be cancelled, which restores the full rent.
- **Payment methods (per country):** the driver pays only with the country's mobile-money operators (`payment_operators`): Gabon Airtel Money and Moov Money, Mali Orange Money and Moov Money (PVit integration for Mali comes later). Staff can enter one received outside the app with the operator's transaction number. `GET /payments/operators` lists them.
- **Mali contract (Angata, template `ML-1`, V24):** the Gabon contract with the brand swapped: company ANGATA SARL, green accent `#15803D`, the Angata logo, Bamako instead of Libreville, Malian law and court. Everything else is identical. Angata's details (V25): Hamdallaye ACI 2000, Rue 223, Porte 338, Bamako; +223 79 35 34 93; info@angata.com; prefix `ANG-`; the RCCM is still to be provided. The payment receipt and the arrears e-mail follow the country's brand (name, colour, logo), and the admin shows the Angata logo and a green theme to Mali users (`data-brand` in `globals.css`).
- **Deadline: every day before 12:00 (noon) of the same day (payable from 00:00)**, in the country's timezone. A rent unpaid after that is an **impayé**.
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
