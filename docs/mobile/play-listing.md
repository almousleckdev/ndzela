# Google Play listing: NdZela driver app (`com.ndzela.rider`)

Package `com.ndzela.rider` (Android `applicationId` and iOS bundle id). Bundle: `driver/build/app/outputs/bundle/release/app-release.aab`, version 1.0.0 (1), built with `env/production.json`. Build steps: [release.md](release.md).

## Store settings
- App or game: App. Category: Auto & Vehicles. Free. Default language: French (France).
- Contact: support e-mail, website `https://www.ndzela.com`.
- Privacy policy: `https://www.ndzela.com/confidentialite`. Terms: `https://www.ndzela.com/conditions`. Account deletion: `https://www.ndzela.com/suppression-compte` (also in the app: Profil, Supprimer mon compte).

## Texts (French)
App name (max 30): `NdZela`

Short description (max 80): `Payez votre loyer, suivez votre véhicule et vos reçus, simplement.`

Full description:
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

## Release notes (internal testing)
```
<fr-FR>
Première version de NdZela, l'application des chauffeurs.
• Consultez votre véhicule et votre loyer à régler
• Payez votre loyer par mobile money
• Retrouvez votre historique par jour, semaine ou mois
• Téléchargez vos reçus en PDF
• Gérez votre profil et votre compte
</fr-FR>
```

## Sign in details (Play Console, App content)
Restricted access: yes. Username and password of a **reviewer driver** created for Google (production API, verified e-mail, a contract and a vehicle). Other information (English, max 500):
```
Sign in with the e-mail and password above. Accounts are created by NdZela, so there is no sign-up. The account has a vehicle and a rent due. Payments use real mobile money (Airtel, Moov): please do not pay. All other screens (home, history, receipts, profile) are fully usable. Account deletion: Profile, Delete my account.
```

## App content declarations
- Ads: none. Advertising ID: no. Government app: no. Health: none. Target audience: 18 and over.
- Content rating: category "All other app types"; answer no to violence, sexual content, profanity, drugs, gambling and user-generated content.
- Financial features: Mobile payments and digital wallets only if Google asks for the payment of rent by mobile money; the account must be an **organization** account (a personal account is refused for payment features).
- Data safety (collected, linked to the account, not shared, encrypted in transit, deletion on request, not optional except photo):
  - Personal info: name, e-mail address, phone number, user IDs, address (contract).
  - Financial info: purchase history (payments and receipts).
  - Photos and files: identity and contract documents provided by the driver.
  - Purpose: app functionality and account management. No advertising, no tracking.

## Graphics
- Icon 512x512 PNG: from `driver/assets/icon/app_icon.png`.
- Feature graphic 1024x500: cropped from `driver/assets/images/image.png`; replace with the designer's version when available.
- Phone screenshots (2 to 8, Android, 9:16): home, Payer, Historique, Profil, login. Take them signed in with the reviewer driver.
