# Stage 04 — first functional academy slice

Stage 03 technical delivery is merged in PR 4; scientific/academic review and clinical content expansion remain deferred. This is the start of stage 04, not a claim that the full academy/admin specification is finished.

## Working implementation
- Three original bilingual courses/modules/lessons: dimensional analysis, ideal dilution and a fictional exponential half-life model. Six interactive questions include explanations and learning outcomes. The SI source is the BIPM SI Brochure (CC BY 4.0); worked examples are original, not quotations or BIPM endorsements. Scientific review is visibly pending.
- Room v6 stores the academy catalog, in-progress answers and immutable completed attempts with lesson revision, score and timestamp. Reopening the database restores work offline. Catalog replacement preserves attempts/drafts and ignores drafts for a different lesson revision.
- Three deterministic educational calculators: same-dimension mass/volume conversion, ideal stock dilution, and percentage remaining in a defined half-life model. Explicit units, finite/range checks, zero/negative/overflow rejection, and six-significant-digit display. No patient dosing or administration recommendation.
- Existing public API stays read-only. `/v1/academy/snapshot` reads only published lesson versions, visible courses and referenced sources under a consistent transaction. Android validates and saves the complete small catalog (2 MiB transfer cap), then works without another request.
- A separate authenticated `academy` Neon Function provides `GET /v1/admin/lessons` and `POST /v1/admin/transition`. Managed Neon Auth JWTs are verified against injected JWKS and issuer using jose. Identity is never accepted from posted fields. Database memberships enforce Owner/Reviewer/Editor/ReadOnly; anonymous/unassigned identities cannot read drafts or write.
- PostgreSQL transition functions enforce optimistic revision checks, independent review, owner-only publish/withdraw, stale-review invalidation, version retention and append-only audit/review records. An edit keeps the previous publication intact until a reviewed replacement is published. The initial three lessons are explicitly unreviewed educational material, with no fabricated clinical approvals.
- No editor memberships were guessed or automatically assigned. Production administration requires an explicitly mapped, verified Neon Auth identity. Raw academy tables and mutation functions are not granted to anonymous/PUBLIC.

## Evidence
Migrations 007 and 008 were applied twice to the isolated Neon test branch before main appdb. The rollback-only SQL test in `neon/tests/stage04-rbac.sql` exercises actual database permissions, publish/edit/review/withdraw, stale revisions, immutable audit and self-review rejection without leaving synthetic approvals behind. Node tests cover signed/expired/wrong-issuer/forged tokens, spoofed actor fields, publication filtering and lesson contracts. JVM tests cover numerical identities and invalid calculator inputs. Android tests cover Room upgrades v1–v5, offline draft/attempt persistence, live catalog transfer, and interactive lesson/quiz navigation.

## Not completed or claimed
- The remaining six subject tracks and a broad clinical curriculum are not populated.
- Clinical cases, patient-specific calculators, editorial web/Android panel, role-management UI, authenticated learner sign-in and cross-device attempt synchronization/conflict management are not delivered in this slice.
- Positive sign-in with a real assigned reviewer/owner, 2G/3G throttling, physical low-memory hardware and performance profiling remain unverified.
- The internal test APK is not the clinically approved public Beta or a stable medical reference. Temporary CI signing may require reinstalling later.
