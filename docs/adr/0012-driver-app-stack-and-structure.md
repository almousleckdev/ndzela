# ADR-0012: Driver app stack and feature-first structure

**Status:** Accepted · **Date:** 2026-10-02

**Context.** The Flutter driver app (`driver/`) was created. [ADR-0008](0008-driver-accounts-separate-from-staff.md) and [mobile/architecture](../mobile/architecture.md) fix Flutter + Riverpod and leave the other choices open. Drivers pay rent from the app only; the backend talks to the payment aggregator, never the phone.

**Decision.**
- **State and injection:** `flutter_riverpod` with hand-written `Notifier` / `Provider`s. **No code generation** (no freezed, riverpod_generator, json_serializable): models are small immutable Dart classes and sealed classes, JSON is mapped by hand. Revisit when the model count makes it hurt.
- **Routing:** `go_router`, redirecting on the session state (loading, signed out, signed in).
- **HTTP:** `dio` with one client (`core/network`): base URL from `--dart-define=API_BASE_URL`, prefix `/api/v1/driver`, `X-Request-ID` on every call, bearer token from secure storage. Errors become `AppException` carrying the server's `code`; the UI maps codes to French text.
- **Storage:** `flutter_secure_storage` (Keychain / Keystore) for tokens only. No secrets or provider keys in the app.
- **Locale and money:** French first (`flutter_localizations`, `intl`); amounts are the integer `Money(amount, currency)`, never `double`.
- **Native look on each platform:** one adaptive layer, `lib/core/widgets/adaptive`, chooses the Material widget on Android and the Cupertino one on iOS (`isCupertino(context)`); `AppIcon` pairs a Material glyph with its SF Symbol. Display-only widgets live in `core/widgets/display`, loading and error states in `core/widgets/feedback`, header bands in `core/widgets/layout` (`HeaderBand`, `ScreenScaffold`), and the bottom bar in `core/widgets/navigation` (drawn by us, identical on both platforms, so it is neither Material nor Cupertino).
- **Visual identity:** the NDZELA logo's dashed road is the signature (login header, header backdrop, arrears divider, section ticks). Light, compact header bands (white, a soft orange glow and the dashed road) with navy text and one orange accent for actions; Bricolage Grotesque (display) and IBM Plex Sans (text), bundled as assets (same pair as the web admin); text and amounts use `AppTextStyles.display`. One top-level class per file.
- **Tests:** `flutter_test` plus `mocktail`.
- **Structure:** `lib/app` (app, router, theme, bottom-navigation shell), `lib/core` (config, network, error, storage, money, shared widgets), `lib/features/<feature>/{data,domain,presentation}` for `auth`, `contract`, `payments`, `profile`. `presentation` holds screens and Riverpod controllers, `domain` the immutable models, `data` the repositories wrapping the API. A feature never imports another feature's `data`; shared things move to `core`.

**Deferred.** Push notifications (needs Firebase project setup), refresh-token rotation in the Dio client (a rejected session signs the driver out), starting a payment (PVit), payment history and the driver photo.

**Consequences.** (+) Few dependencies, no build_runner step, simple onboarding. (-) Hand-written JSON mapping is repetitive; revisit if the app grows large.
