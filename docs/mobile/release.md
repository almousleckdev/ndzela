# Releasing the driver app (TestFlight and Google Play testing)

App name **NdZela**, Android `applicationId` and iOS bundle id **`com.ndzela.driver`**. Both stores use the same version: `version: x.y.z+n` in `driver/pubspec.yaml` (`x.y.z` is shown to users, `n` is the build number; **every upload needs a higher `n`**; pass `--build-number` to override).

## 1. Which backend a build talks to
A release build refuses to start without an HTTPS `API_BASE_URL` (see `AppConfig`). The values live in `driver/env/*.json` (public values only, never a secret):

| Build | File | Backend |
| --- | --- | --- |
| Test (TestFlight, Play internal testing) | `env/staging.json` (copy `staging.example.json`, put your test API host) | A deployment with `APP_ENVIRONMENT=staging`, the **PVit sandbox** and `PVIT_SANDBOX_AMOUNT=150` (no real money moves) |
| Production | `env/production.json` | `https://api.ndzela.com` with the real PVit account ([railway-deployment](../infrastructure/railway-deployment.md)) |

## 2. Android: Google Play internal testing
1. **Upload key** (once, keep the file and passwords in the team's secret store, never in Git): `keytool -genkey -v -keystore ~/ndzela-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`. Copy `driver/android/key.properties.example` to `key.properties` and fill it. Without it `flutter build appbundle` refuses to run (a debug-signed bundle would be rejected anyway).
2. Build: `cd driver && flutter build appbundle --release --dart-define-from-file=env/staging.json` (output `build/app/outputs/bundle/release/app-release.aab`).
3. Play Console: create the app (name NdZela, package `com.ndzela.driver`), accept **Play App Signing** (Google keeps the app key; ours is only the upload key), Testing, **Internal testing**, create a release, upload the `.aab`, add testers by e-mail list, share the opt-in link.
4. Store listing needs: privacy policy `https://ndzela.com/confidentialite`, account deletion page `https://ndzela.com/suppression-compte` (the app also deletes from Profil, "Supprimer mon compte"), the Data safety form (name, e-mail, phone, payment history, all linked to the account and not shared; encrypted in transit; deletion on request), and a **reviewer test account** (a driver created for that purpose).

## 3. iOS: TestFlight
1. Apple Developer account, then App Store Connect, My Apps, **New App**: name NdZela, bundle id `com.ndzela.driver` (register it under Certificates, Identifiers & Profiles first), primary language French.
2. In Xcode open `driver/ios/Runner.xcworkspace`, target Runner, Signing & Capabilities: tick **Automatically manage signing** and pick the team (the project currently names team `W9XVSW8Z3S`; change it to the company's team).
3. Build: `cd driver && flutter build ipa --release --dart-define-from-file=env/staging.json` (output `build/ios/ipa/*.ipa`). Upload it with the **Transporter** app (or Xcode, Window, Organizer, Distribute App).
4. TestFlight: wait for processing, then add **internal testers** (App Store Connect users, no review) or an **external group** (first build goes through a short Beta App Review; give it a driver test account and the contact details). Export-compliance questions are already answered in `Info.plist` (`ITSAppUsesNonExemptEncryption=false`: the app only uses the system's HTTPS).

## 4. Before the first test
- Run `flutter analyze && flutter test` in `driver/`.
- Sign in on a real phone with a test driver and pay once (the real rent, with the real PVit account in production; the sandbox only below 1000 FCFA), then check the receipt, the Historique tab and the staff dashboard's Transactions table.
- The launcher icon is the white NDZELA mark on the brand orange (`#E48102`), full bleed with no white border: `driver/assets/icon/app_icon.png` for iOS and `app_icon_foreground.png` (white mark on transparent) over the orange background for Android (a 1024 px file made from the 500 px logo: **replace it with the designer's 1024 px master when available** and run `dart run flutter_launcher_icons`; see the driver README).

## Build commands
```
cd driver
flutter build appbundle --release --obfuscate --split-debug-info=build/symbols --dart-define-from-file=env/production.json   # Google Play (needs android/key.properties)
flutter build ipa --release --obfuscate --split-debug-info=build/symbols --dart-define-from-file=env/production.json         # App Store (then upload with Transporter or Xcode Organizer)
```
Keep `build/symbols` of every release (it turns a crash report back into readable code): upload it to Play Console (Debug symbols) and to your crash tool. Both stores need the minimum iOS to be **14.0** and Android `targetSdk` 36 (set by Flutter).

## Release hardening (2026-11-06 audit)
- **Android:** the release manifest asks only for `INTERNET`. The `READ_MEDIA_*` and `READ_EXTERNAL_STORAGE` permissions added by the file picker are removed on purpose (Google Play refuses them without a reason; the phone's own picker needs none). Cleartext traffic and backups are off. Check with `aapt2 dump permissions app-release.apk` before each upload.
- **Tokens:** kept in the keychain / keystore only, on this device (`first_unlock_this_device`, never in a backup). The keychain survives an uninstall on iOS, so the first launch of a fresh install wipes it (`FirstRunGuard`).
- **Personal files:** the contract and receipts are opened from the temporary folder and deleted when the session ends and when the app starts.
- **Payments:** the app never sends an amount, only the number of days (the server decides what is due and refuses another driver's account); a payment the operator has not answered after 10 minutes shows "Confirmation en attente" with a check button instead of an endless spinner, and the server expires it after 30 minutes. The fee shown comes from the server's quote.
- **Lints:** `analysis_options.yaml` now fails on `avoid_print`, `avoid_dynamic_calls`, `unawaited_futures` and a few bug-class rules.
- **Not done on purpose:** certificate pinning (a certificate change on Railway would lock every installed app out until an update; add it only with a spare-pin process) and root/jailbreak detection (easy to bypass, and the server is the authority).

## Store listing checklist (Google Play and App Store)
- **Privacy policy URL:** `https://www.ndzela.com/confidentialite`; **terms:** `https://www.ndzela.com/conditions`; **account deletion page** (required by Google Play): `https://www.ndzela.com/suppression-compte`. Use `www`: the bare `ndzela.com` has no DNS record yet ([ADR-0018](../adr/0018-railway-hosting-and-domains.md) addendum).
- **Data collected** (Play Data safety, App Store privacy labels): name, e-mail, phone number, photo and identity documents (provided by the driver, linked to the account, used to run the rental contract), payment information (mobile-money number and transactions, handled by PVit; no card data), no advertising, no tracking, data encrypted in transit, deletion on request.
- **App Review notes:** give a test driver account (phone and password) and say payments use mobile money (Airtel, Moov) for a vehicle rental, not digital goods.
- **iOS:** add a `PrivacyInfo.xcprivacy` to the Runner target in Xcode if App Store Connect asks for it (the plugins ship their own); the photo-library text is already in `Info.plist`.
- **Screenshots:** take them from the white screens (home, payment, historique, profil) on a 6.7" iPhone and a recent Android phone.

## 5. Going to production (payments for real)
Follow the payment go-live checklist in [infrastructure](../infrastructure/infrastructure-and-deployment.md#payment-go-live-pvit): the production backend refuses to start with a sandbox amount or missing PVit credentials. Then build with `env/production.json`, promote the tested build in TestFlight to App Store review, and move the Play internal release to a production track (or closed testing first).
