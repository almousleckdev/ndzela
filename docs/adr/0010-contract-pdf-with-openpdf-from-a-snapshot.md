# ADR-0010: Contract PDFs are rendered on demand with OpenPDF from a frozen snapshot

**Status:** Accepted · **Date:** 2026-09-30

**Context.** Each driver signs a French rental contract that must print identically for years, whatever happens to the driver's or the car's record afterwards. The Gabon text comes from the signed sample; Mali will have its own. A PDF library had to be chosen ([contracts.md](../business/contracts.md) left it open).

**Decision.**
- The PDF is **generated server-side with OpenPDF 2.0.3** (LGPL/MPL, no native dependency, built-in Helvetica covers French accents), one template class per country, selected by the country's `contract_template` version (`GA-1`).
- The contract stores a **JSON snapshot** of everything it prints about the parties (bailleur, locataire, licence, emergency contact, vehicle, training, documents provided) plus its financial terms. The PDF is **rendered on demand** from the snapshot and the template version; **no PDF file is stored**, so nothing needs separate protection and every rendering is identical.
- Each rendering is audited (`CONTRACT_PRINTED`). Editing a driver or a vehicle never changes a signed contract; a change of terms will be a dated amendment (avenant), not an edit.
- A country without a template refuses to create drivers (`CONTRACT_TEMPLATE_UNAVAILABLE`) until its template and legal entity data exist.

**Consequences.** (+) Reproducible, small, no file storage, no fonts to ship. (-) Changing a template means a new version (old contracts keep printing with theirs, which the code must keep); the layout is code, not an editable document. Alternatives considered: HTML to PDF (heavier, font handling), Apache PDFBox (lower-level layout), storing the generated PDF (needs secured storage and does not regenerate).
