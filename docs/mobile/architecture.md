# Mobile architecture (driver app)

**Status: stack Decided (Flutter + Riverpod), project scaffolded in `driver/` (shell, routing, network, secure storage; login is not wired because the driver API does not exist yet). The app design will be provided later (into `docs/mobile/design/`).**

## Purpose

The app is **only for drivers who already hold a contract**. Drivers **never register themselves**: staff create the driver, assign a car and a contract in the web admin, then the driver installs the app and signs in with **phone or email + password**
([business/drivers.md](../business/drivers.md)). It is not a public app, not ride-hailing, and not the charging app of the legacy spec.

A driver can:
- sign in with **phone or email + password only** (no second factor, Decided) and **must change the initial password at first login**;
- **make the daily payment** through the country's mobile-money providers (Gabon: Airtel Money, Moov Money Gabon; Mali: Orange Money, Moov Money Mali);
- follow the contract: car, daily rent, duration, total price, paid, remaining, arrears, payment history and receipts;
- see arrears warnings (24 h / 48 h / 72 h) and, when suspended, be sent to the payment flow only ([business/payments-and-arrears.md](../business/payments-and-arrears.md));
- see limited profile data and receive notifications (channels **Open**).

The **complete design of the driver app already exists** (product owner): it is the source for the screens. **Please share it** (screens exported as images/PDF, or a design-tool link) into `docs/mobile/design/`; until then the screen list is not documented here. The app is referred to as **my-ndzela** in the contract.

## Technology (Decided)

Flutter with **Riverpod**, `go_router`, `dio`, `flutter_secure_storage`, no code generation, feature-first folders: see [ADR-0012](../adr/0012-driver-app-stack-and-structure.md). The app lives in `driver/` of this repository. Still **Open**: min OS versions and distribution. **Store readiness (2026-10-03):** Profile, "Mes données" has the privacy policy and terms (opened from `SITE_URL`, default `https://ndzela.com`) and "Supprimer mon compte" (confirmation, then `POST /driver/account/deletion-request`). Android `applicationId` and iOS bundle id are `com.ndzela.driver`, display name NDZela. Still to do before submission: production HTTPS `API_BASE_URL`, signing keys, store assets, the reviewer test account. **Brand (2026-10-02):** the colours come from `BrandColors`, a theme extension chosen by the signed-in driver's `countryCode` (orange NDZela for GA, green Angata for ML); widgets read `context.brand`, never a fixed orange.

## Screens built so far

Login (phone or email + password), a forced **new password** screen on first login, then a shell with a white bottom bar, every tab labelled (**Accueil**, **Payer**, **Historique**, **Profil**). **Lock:** a driver with three or more unpaid days (or a suspended account) lands on the payment screen at login and sees no tabs, only the payment form (the whole arrears, never part of them) and a sign-out button, until everything is paid (the message to support stays reachable, because a locked driver may need help paying); the rule is the server's (`/account` says `locked` and `suspended`), the app follows it. A tap on a payment in **Historique** opens its details, and the confirmation page after paying shows the same receipt (`PaymentReceipt`): date and time, sender (the driver and the number), operator, beneficiary, amount, fee, total paid, our reference and the operator network's transaction number. A payment the operator did not confirm says so plainly (PVit gives no reason, so the screen lists the usual causes) and offers "Contacter le support", which opens the message with the payment's reference already written. After a confirmed payment, and from its details, **"Partager le reçu"** downloads the PDF receipt from the server and opens the phone's share sheet (send it to NDZELA, save it, print it). **Signing out** always asks for confirmation (one shared action, `confirmLogout`, used by the profile, the payment screen of a locked driver and the new-password screen). **Accueil**: a compact header with the greeting, notifications icon and avatar, then the vehicle card (photo, name, number plate), the arrears card ("Régulariser maintenant") and a contract summary. **Payer**: amount and days, Mobile Money number, operator choice from the server, Récapitulatif, "Payer … FCFA". **Historique**: the driver's payments. **Profil**: personal information (phone, e-mail, birth date, address), my contract (number, start date, total duration, daily rent), my documents (which ones were handed in), support (call, WhatsApp, e-mail, and "Envoyer un message" stored for the staff), and a red logout with a confirmation dialog. Every figure comes from the driver API ([api conventions](../api/conventions.md)); the app holds no sample data. Paying works end to end: the Récapitulatif shows the amount, the operator's fee on top and the total (quoted by the server); "Payer" starts the payment, the app waits for the phone approval (checking every 3 seconds) and shows the confirmation or the failure; **Historique** lists the payments with their status. **Not wired yet:** notifications and opening the documents. The driver's photo is loaded with the session (`GET /me/photo`); a vehicle has no photo field yet, so the vehicle card shows the NDZELA fleet picture (`assets/images/car.png`).

**Platform rule (binding):** the app never renders a Material widget on iOS or a Cupertino widget on Android. Every platform-specific widget lives in `lib/core/widgets/adaptive` (button, text field, bottom bar, dialog, message, refresh, icons...) and screens use only those; a Material or Cupertino widget outside that folder is a defect.

## Proposed structure and standards (to confirm when the app starts)

- **Feature-first** folders mirroring the web admin's philosophy: `lib/features/<feature>/{data,domain,presentation}` and `lib/core` (API client, auth, theming, error handling, config), no cross-feature internals.
- Push notifications and email are the warning channels (Decided; no SMS). Riverpod: providers are the only way to obtain services/state; **no business rules in widgets**; UI reads `AsyncValue` and handles loading/error/empty; repositories wrap the API; models are immutable.
- **Tokens in secure storage** (Keychain/Keystore), never in plain preferences; no secrets or provider keys in the app.
- One API client with the single standard error format and `X-Request-ID`; user text comes from a localisation layer (French first).
- Money is handled as exact decimals/integers with the country's currency, never `double`.
- Tests: unit tests for logic, widget tests for screens, integration tests for the payment and suspension flows.

## Binding architecture rules

1. **The app is a client of the same backend.** It never talks to the database, and contains no rule the server does not also enforce (arrears state, suspension, amounts due are server truth).
2. **Driver accounts are separate from staff `users`** ([ADR-0008](../adr/0008-driver-accounts-separate-from-staff.md)): own identity, own login by phone/email + password, own token audience, and a fixed capability set: **own contract and own payments only**.
3. **Tenant isolation applies:** a driver belongs to one country; every driver endpoint is scoped to that driver and country. Countries drive currency, providers and language shown.
4. Driver endpoints live under their own prefix (Proposed: `/api/v1/driver/**`) with their own security rules, separate from staff endpoints.
5. **Payments** are initiated through the backend and **confirmed by the provider's callback/webhook**; the backend records them in the ledger. The app never marks a payment as paid.
6. A **suspended driver** is not simply rejected at login: the backend issues a session restricted to the payment flow (design to be confirmed).
7. Same error format and conventions as the rest of the API ([api conventions](../api/conventions.md)).

## Open decisions

Offline behaviour on weak connectivity, languages per country, repository location, distribution (stores, MDM), minimum OS versions.

**Home and history (2026-10-03):** Home is the branded balance card (amount owed, days late, the pay button, the animated road), then the vehicle row (name and plate, no photo) and the contract glance (daily rent, end date, days left). History shows the total paid, filter pills (all, confirmed, failed or expired) and the payments grouped by month in flat rows. The profile keeps the legal links and the account deletion in two separate blocks.
