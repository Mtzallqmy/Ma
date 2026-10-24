# Stage 03 — medicine library technical delivery

Scientific and academic review is deferred at the owner’s request. This delivery does not mark any drug, dose or product clinically approved. The signed APK is an internal technical test build, not the clinically gated public Beta described in FIVE_STAGE_ROADMAP.md.

## Implemented
- Seven medicine-detail tabs: overview, indications, safety, identifiers, products, dosing and references. Search by generic name, RxNorm identifier and verified brand; filter by class and market, including Yemen.
- Structured evidence and review metadata flow from Neon through the consistent published snapshot into Room v5. Explicit v4→v5 migration preserves user data. Existing installs receive identifier metadata without replacing clinical descriptions.
- Published clinical facts require the clinical publication status, human review date and source. Server additionally requires reviewer attribution, but never exposes reviewer identities. Regional products require verified registration, date, source and HTTPS evidence; registration is distinguished from availability.
- Eight exact ingredient identifiers verified against official NLM RxNorm ingredient concepts; evidence is archived in docs/data/rxnorm-identifiers-20261024.json. Identifier matching is not clinical review. Insert-only migration 006 was tested on an isolated Neon branch, then applied to the supplied appdb.
- HTTPS content downloads retain partial files across interrupted requests; Range responses are checked, servers ignoring Range restart safely, and existing SHA-256 verification and atomic activation remain mandatory.
- Internal ARM64 Android 8+ APK workflow uses a temporary test certificate, verifies signature, ABI and checksum, and stores the installable artifact. A future certificate may require reinstalling; no production signing key is claimed. Public prerelease publication remains deferred with scientific review.

## Content boundary
The bundled library currently has eight educational ingredients. The database has no published clinical dosing references or verified local products. Empty sections are shown honestly; no doses, brand registrations, prices, availability or review signatures were invented. The project is not yet a complete drug encyclopaedia.

## Verification
Node API contract tests cover publication predicates, source filtering, snapshot revisions and private reviewer exclusion. JVM tests cover clinical/product gates and interrupted HTTP downloads. Android device tests cover RxNorm search and detail navigation, legacy Room upgrades, live Neon synchronization and real cloud-pack download followed by offline reads. CI and APK build results are recorded in the pull request and Actions run.
