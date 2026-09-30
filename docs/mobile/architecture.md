# Mobile architecture (driver app)

**Status: stack Decided (Flutter + Riverpod). Deferred: the backend and web admin are built first, and the driver app design will be provided later (into `docs/mobile/design/`). Nothing exists in this repository yet.** Where the app will live (this monorepo, e.g. `mobile/`, or its own repo) is **Open**.

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

Flutter with **Riverpod** for state management and dependency injection. Further choices (HTTP client, routing, storage, code generation, testing tools, min OS versions, distribution) are **Open** and need an ADR before the first line of app code.

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
