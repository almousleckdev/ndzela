# Drivers (chauffeurs)

**Status: Implemented for steps 1, 2, 3 and the password pop-up (2026-09-30)**: `driver` feature, migration `V11`, screens "Chauffeurs" (list, detail, edit, 3-step creation wizard) and the training-centre list. **Update (V17):** the edit screen has a contract step to refresh the active contract (see [contracts.md](contracts.md)). **Update (V14):** the creation wizard has a 4th step, the contract (see [contracts.md](contracts.md)), and a vehicle and a caution are now required. **Update (V12):** photo and document uploads, reasons, blocking and a row menu (see "Files, reasons and blocking" below). **Step 4 (contract terms, recap, PDF) is the next round** and will be inserted before the password pop-up. The API accepts a driver without a vehicle (`vehicleId` optional; the wizard requires one), which also allows assigning later and deleting a driver who never had a vehicle.

## Who creates drivers

**Only staff.** A driver cannot create an account. Staff create the driver in the web admin (a **5-step wizard**); the driver then downloads the Flutter app and signs in with **phone or email + password**. There is no public registration endpoint.
The whole wizard is submitted once at "Créer le chauffeur": the driver, the vehicle assignment, the contract and the credentials are created **atomically**. **All-or-nothing: no draft saving** (Decided).

## Step 1: Informations personnelles

| Field | Rules / notes |
| --- | --- |
| Civilité | Enum: **Male / Female** (French labels Homme/Femme: to confirm). |
| Nom, Prénom | Required. |
| Date de naissance | **Driver must be at least 18 years old on the creation date** (Decided; enforced by the server, the UI mirrors it). |
| Lieu de naissance | |
| Nationalité | Country list. |
| Pièce d'identité | The staff member **first selects the type (passport or national ID card), then types the number** (no file upload), **date de délivrance**, **lieu de délivrance**. |
| Adresse, Quartier, Ville | Address parts. |
| Téléphone principal, Téléphone secondaire | Validated with the country's phone format. Primary phone is a sign-in identifier. |
| Email | Required for the account (step 5). Sign-in identifier. |
| Permis de conduire | Licence number, **date de délivrance**, **catégorie**: enum **A, B, C, D**. |

## Step 2: Personne à contacter en cas d'urgence

Nom, Prénom, Téléphone, **Lien de parenté** (enum: `Père`, `Mère`, `Frère`, `Époux/Épouse`, `Sœur`, `Oncle`, `Tante`, `Cousin(e)`, `Ami(e)`, `Autre`), Adresse complète.

## Step 3: Association véhicule

| Field | Notes |
| --- | --- |
| Véhicule à assigner | Chosen from the **registered cars** that are available ([vehicles.md](vehicles.md)). |
| Centre de formation | **Optional**; selected from a **managed list** of training centres (Decided), **managed by the gestionnaire** (sample contract shows "NDZELA" as centre). |
| Date de formation | |
| N° Certificat | Training certificate number. **Optional at creation** (Decided; the contract prints "À compléter" when empty). |

## Step 4: Informations du contrat

Date de début, Durée (jours), Loyer journalier (FCFA), Prix total véhicule (FCFA), Caution (FCFA) *. At the bottom the **Récapitulatif financier** is generated. Details: [contracts.md](contracts.md).

## Step 5: Compte du chauffeur (pop-up)

A pop-up shows the **email entered in step 1** (read-only) and asks for the **Mot de passe** (**at least 8 characters**). Then "Créer le chauffeur" creates everything.
The admin sets the initial password (**minimum 8 characters**); **the driver must change it at first login** (Decided, recommended). The driver signs in with **email or phone + password only, no second factor** (Decided). Because only password protects the account, throttling and lockout on driver sign-in are required (design in ADR-0008).

## After creation

The next page shows the **contract as a printable PDF** containing all the information, according to the driver's country ([contracts.md](contracts.md)).

## Rules

- Driver belongs to one country (tenant-owned); the wizard creates them in the staff member's country (super admin picks the country).
- **ID number, licence number, phone and email are unique per country** (Decided). Email and phone must also be unique among driver sign-in identifiers so that one identifier means one driver.
- Personal and identity data is sensitive: access needs permissions, must be audited ([findings A4](../findings/architecture-findings.md)), never logged.
- Driver states (active, suspended for arrears, contract ended...) are described in [payments-and-arrears.md](payments-and-arrears.md).

## Permissions (Proposed)

`drivers:read`, `drivers:create`, `drivers:update`, `drivers:suspend`, `drivers:block`, `drivers:unlock`, `drivers:delete`. **Only the gestionnaire role creates, edits and deletes drivers** (Decided); the **ADMIN** role suspends and reactivates them (Decided). The contract also prints the **documents provided** (licence, ID, justificatif de domicile under 3 months): an **upload in step 3** (Decided 2026-09-30, replaces the earlier checkbox idea).

## Screens, leaves and exports (Implemented, V13, 2026-09-30)

- **Table:** metrics (total, actifs, en arrêt, suspendus, assignés / sans véhicule) from `GET /drivers/stats`; tabs Actif, En arrêt, Inactif, Suspendu, Bloqué (Actif excludes drivers on leave); search; per-row icons to view details and "Gérer les arrêts", plus the three-dots menu; **Exporter** (Excel or PDF, `drivers:export`, same filters, audited). **"En retard"** (built with payments, [payments-and-arrears.md](payments-and-arrears.md)) filters the drivers with unpaid rent and tints their row by level.
- **States:** `INACTIVE` is a manual "not working for now" state (reason required, set and lifted by `drivers:update`, not a sanction). **"En arrêt"** is derived: an active driver with a non-cancelled leave covering today in the country's timezone.
- **Leaves (arrêts):** `driver_leaves`; type `SINGLE_DAY` or `DATE_RANGE` (max 90 days), reason among vehicle breakdown, accident, maintenance, public holiday, weather, administrative, personal, other (note required for "other"). Only today and future days can be declared; overlaps are refused (the driver row is locked so two requests cannot both pass); a leave that has not started can be cancelled, one that has cannot. Declarations and cancellations are audited and kept as history. **Rent is waived on declared days** (Decided; applied by the arrears computation in Round B). Endpoints `GET/POST /drivers/{id}/leaves`, `POST /drivers/{id}/leaves/{leaveId}/cancel`; declaring needs `drivers:update`.
- **Reactivating** needs the permission of the state being lifted: `drivers:suspend` (suspended), `drivers:block` (blocked), `drivers:update` (inactive).

## Deleting a driver or a vehicle (Implemented, V18, 2026-10-01)

Decided: **deletion is allowed only while no money was ever recorded.** Deleting a driver erases the driver with their contracts, assignments, leaves and files, frees their car (back to available) and writes `DRIVER_DELETED` and `CONTRACT_DELETED` (with the contract numbers) to the audit trail. It is refused when a **payment, a waiver or a recharge** exists (`DRIVER_HAS_HISTORY`, enforced by the database foreign keys) or when the driver **owes rent** (`DRIVER_HAS_ARREARS`); then suspend or block the driver. Deleting a **vehicle** (`DELETE /vehicles/{id}`, served by the driver feature because it touches contracts and assignments) erases its ended contracts and assignments, but is refused for a rented vehicle (`VEHICLE_ASSIGNED`) and when money is recorded on those contracts or recharges exist (`VEHICLE_HAS_HISTORY`); put it out of service instead. Both are **all or nothing**: a refused deletion leaves everything as it was. Contract numbers of deleted contracts are not reused. `arrears_events` follow their contract (`ON DELETE CASCADE`).

## Files, reasons and blocking (Implemented, V12)

- **Profile photo: required** at creation (step 1); replaceable on the driver page, never removable. **Documents** (licence, ID, criminal record (stored as `address-proof`), certificate of residence (`residence-certificate`, V44)): optional uploads in step 3, replaceable and removable on the driver page. Accepted: JPG, PNG, WebP (photo) and also PDF (documents), **5 MB** maximum, the type is checked from the file bytes.
- **Storage:** private Cloudinary assets under `<root>/<country>/documents/` ([ADR-0016](../adr/0016-cloudinary-file-storage.md)), never given to a client as a URL; read only through `GET /api/v1/drivers/{id}/files/{photo|licence|id|address-proof}` (needs `drivers:read`, tenant-scoped, so another country answers 404). Creation is one multipart request (`data` JSON part, `photo`, `licence`, `id`, `addressProof`); files written by a failed creation are deleted. The database keeps `<country code>/<uuid>.<ext>`.
- **States:** `ACTIVE`, `SUSPENDED` (temporary, `drivers:suspend`), `BLOCKED` (durable, `drivers:block`, ADMIN and SUPER_ADMIN). A blocked driver is reactivated only by a holder of `drivers:block`. Blocking does not free the car; "Retirer le véhicule" does.
- **Reasons (Decided):** suspending, blocking and removing the vehicle all **require a reason** (max 255 characters), stored (`status_reason`, `vehicle_assignments.end_reason`), shown in the screens and written to the audit trail. The same rule applies to disabling or blocking a staff account.
- **Menu:** each row of the drivers table has a three-dots menu: Voir, Modifier, Suspendre, Réactiver, Bloquer, Retirer le véhicule, Supprimer, each shown only when allowed.
