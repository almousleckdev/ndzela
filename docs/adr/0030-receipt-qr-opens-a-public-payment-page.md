# ADR-0030: The QR code of a receipt opens a public payment page

**Status:** Accepted · **Date:** 2026-11-06

**Context.** The receipt PDF carried a QR code with the payment reference only, which opened nothing. The product owner wants anyone who scans it to see the payment, the driver account and the days settled, on the NDZela website.

**Decision.** The QR code encodes `<PUBLIC_SITE_URL>/recu/<ledger entry id>` (`https://www.ndzela.com`: the bare `ndzela.com` has no DNS record, see ADR-0018). The id is a random UUID (122 bits), so the address cannot be guessed and acts as the proof that the holder has the receipt. The website page `/recu/[id]` (not indexed, disallowed in robots.txt) reads `GET /api/v1/public/receipts/{id}` (public, rate limited to 60 a minute per address, resolved in the public site's country) and shows: confirmed or cancelled, reference, date, driver name, contract number, operator, **masked** phone, rent, fee, total, beneficiary, and each day settled with on-time or late. It never shows the driver's phone number, e-mail, address or documents. Only `PAYMENT` ledger entries are served; anything else is a 404.

**Consequences.** (+) A scanned receipt now proves the payment and what it settled; a cancelled payment shows as cancelled. (-) Whoever holds a receipt can see the driver's name and what was paid; a receipt photographed or shared exposes that, which is the same information printed on it. Receipts issued before this change still carry the old QR code (the reference only). The website needs `PUBLIC_SITE_URL` on the API and `NEXT_PUBLIC_API_URL` on the website.
