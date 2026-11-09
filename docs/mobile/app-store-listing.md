# App Store listing: NdZela driver app (`com.ndzela.rider`)

Bundle id `com.ndzela.rider`, team `XB5CS344AV`, version 1.0.0 (1), iOS 14.0 and up. IPA: `driver/build/ios/ipa/NdZela.ipa` (build: `flutter build ipa --release --obfuscate --split-debug-info=build/symbols --dart-define-from-file=env/production.json`). Upload with the Transporter app. Steps: [release.md](release.md).

## App Store Connect
1. Register `com.ndzela.rider` under Certificates, Identifiers & Profiles (Xcode already registered it with automatic signing).
2. My Apps, New App: name NdZela, primary language French, bundle id `com.ndzela.rider`, SKU `ndzela-rider`.
3. Upload the IPA with Transporter, wait for processing, then attach the build to version 1.0.

## Texts (French)
- Subtitle (max 30): `Votre loyer, simplement.`
- Promotional text (max 170): `Payez votre loyer par mobile money, suivez votre véhicule et téléchargez vos reçus en un instant.`
- Keywords (max 100): `chauffeur,loyer,véhicule,mobile money,reçu,paiement,Gabon,Mali,contrat`
- Category: Business (secondary: Finance only if Apple asks). Price: free.
- Support URL: `https://www.ndzela.com/contact`. Marketing URL: `https://www.ndzela.com`. Privacy policy: `https://www.ndzela.com/confidentialite`.

Description:
```
NdZela est l'application des chauffeurs partenaires NdZela. Elle sert à gérer votre véhicule et votre loyer au quotidien.

• Votre véhicule : consultez le véhicule qui vous est confié et l'avancement de votre contrat.
• Votre loyer : voyez le montant à régler et la date limite, puis payez par mobile money.
• Historique : retrouvez vos paiements par jour, par semaine ou par mois, avec le total de chaque période.
• Reçus : téléchargez vos reçus de paiement en PDF.
• Profil : gérez vos informations, votre mot de passe et votre compte.

Les comptes sont créés par NdZela avec votre contrat : il n'y a pas d'inscription dans l'application. Connectez-vous avec l'e-mail et le mot de passe remis par NdZela.

Vos données sont protégées. Vous pouvez demander la suppression de votre compte à tout moment depuis Profil, Supprimer mon compte.
```

## App Review information
- Sign-in required: yes. Reviewer driver (e-mail and password) created on the production API with a vehicle and a rent due.
- Notes:
```
Accounts are created by NdZela for drivers who signed a vehicle rental contract, so there is no sign-up. Sign in with the account above. The rent is a payment for a real-world service (a rented vehicle), paid by mobile money (Airtel, Moov), not digital goods, so In-App Purchase does not apply (Guideline 3.1.3). Please do not complete a payment: it moves real money. Account deletion: Profil, Supprimer mon compte.
```

## App privacy (labels)
Data linked to the user, used for App Functionality, no tracking: name, e-mail address, phone number, user ID, payment information (mobile money transactions, no card data) and photos or files (documents provided by the driver). The privacy manifest is `driver/ios/Runner/PrivacyInfo.xcprivacy`. Export compliance is answered in `Info.plist` (`ITSAppUsesNonExemptEncryption=false`).

## Screenshots
6.7" iPhone (1290x2796), 2 to 10: home, Payer, Historique, Profil, login. Take them signed in with the reviewer driver.

## Account type
Apple asks for a legal entity for regulated fields such as financial services. A rental payment app is less likely to be flagged, but if App Review asks, enroll the company as an organization (same D-U-N-S number).
