# ADR-0016: Files live in Cloudinary: private assets read through the backend, public CDN images only for profile pictures

**Status:** Accepted · **Date:** 2026-10-13

**Context.** Files were written to the server's disk (`UPLOAD_DIR`): not tenant-aware, lost with the server, unusable with more than one instance, and the avatars were served publicly from `/media/**`. The platform stores identity documents, driving licences and photos of drivers, which are personal data.

**Decision.**
- **Cloudinary**, reached over its signed REST API with the existing `RestClient` (no SDK dependency, same pattern as PVit). `infrastructure/storage/CloudinaryClient` is the only code that knows Cloudinary; it is a plain class (no interface) and tests replace it with an in-memory subclass.
- **Two kinds of file.**
  - *Private* (driver photo, licence, identity document, proof of address, the same files of a job application): uploaded as `raw` assets of type `authenticated` under `<root>/<country code>/documents/<uuid>.<ext>`. Nothing can load them without a request signed with the API secret. **Cloudinary URLs are never given to a client**: the backend reads the file through Cloudinary's signed download API after the usual permission and tenant checks and streams it (`Cache-Control: no-store`), exactly as before.
  - *Public* (staff profile pictures): ordinary image assets under `<root>/<country code or global>/avatars/<uuid>`; the database keeps the CDN URL.
- **What the database stores.** For private files `"<country code>/<uuid>.<ext>"` (for example `ga/9f1c…​.pdf`), so the location is known even when a super admin works across countries; the value is checked against that exact pattern before it is turned into a Cloudinary id, so a bad value can never point elsewhere. For avatars the URL; it is deleted only if it is one this application stored (our cloud, our root folder).
- **Environments** share one account and are separated by `CLOUDINARY_ROOT_FOLDER` (`ndzela-dev`, `ndzela-prod`). Credentials have no default: the application refuses to start without them. The API secret stays in `backend/.env` or the secret store, never in the repository, a frontend or a log (`StorageProperties.toString` hides it).
- **Validation is unchanged and server-side**: the type is decided by the file's first bytes, never by the client's name or type; 5 MB for documents (PNG, JPEG, WebP, PDF) and 2 MB for pictures.
- **Failures.** An upload or a read that Cloudinary refuses or cannot answer is `502 STORAGE_UNAVAILABLE`. Deleting is best effort (a file that cannot be removed is only an orphan and must not undo the change that made it unneeded; it is logged).
- **Tests** never reach Cloudinary (`FakeCloudinaryClient`). `CloudinaryLiveTest` talks to the real account, only when `CLOUDINARY_LIVE_TEST=true` and the `CLOUDINARY_*` variables are set, and removes what it creates.
- **Removed.** `UPLOAD_DIR`, the local `uploads/` folder, `MediaConfig` and the public `GET /media/**`.

**Not covered.** Contract and receipt PDFs are still drawn on demand from their stored snapshot (ADR-0010) and not stored: archiving a frozen copy, or a scanned signed contract, needs a business decision. Files written to the local disk before this change are not migrated (development data only; a driver whose photo name has no country prefix answers 404 for that file).

**Consequences.** (+) Files survive the server and can be shared by several instances; per-country folders; no public URL for personal documents; no new library. (-) A file read is a round trip to Cloudinary (not cached on purpose); a Cloudinary outage blocks document upload and viewing; deleting can leave orphans that need a periodic clean-up.

**Alternatives considered.** The Cloudinary Java SDK (a dependency for a handful of calls); signed delivery URLs handed to the browser (the link can be shared; the signed delivery URL was also refused by this account when tried); S3-compatible storage (a decision for a hosting provider that is not yet chosen).
