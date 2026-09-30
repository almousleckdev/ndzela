# Drivers (chauffeurs)

**Status: Decided from the product owner's description (2026-09-30). Not built.**

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
| Centre de formation | Selected from a **managed list** of training centres (Decided), **managed by the gestionnaire** (sample contract shows "NDZELA" as centre). |
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

`drivers:read`, `drivers:create`, `drivers:update`, `drivers:suspend`, `drivers:delete`. **Only the gestionnaire role creates, edits and deletes drivers** (Decided); the **ADMIN** role suspends and reactivates them (Decided). The contract also prints the **documents provided** (licence, ID, justificatif de domicile under 3 months): a simple **checklist of checkboxes in step 3** (no upload) (Decided).
