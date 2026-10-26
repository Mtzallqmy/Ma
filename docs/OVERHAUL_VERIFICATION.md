# Native Android overhaul — evidence and scope

Work branch: `feat/dawa-hayah-real-production-overhaul` · [PR #9](https://github.com/Mtzallqmy/Ma/pull/9).

## Implemented behavior

- Native Compose home uses responsive cards, actual Room counts, last successful sync, and working destinations. Search results, bookmarks, reading history and notes open a persistent reader with related records, citations and sharing.
- Room v7 adds local notes and migration 6→7. Backup schema 2 includes notes and accepts schema 1. Newer empty notes remain deletion tombstones during restore. Content refresh and pack removal do not remove personal notes.
- Search debounces input, cancels obsolete jobs, escapes literal LIKE wildcards, supports explicit aliases, and offers bounded one-edit suggestions without silently changing scientific terms.
- WorkManager refreshes metadata daily with network/storage constraints and Wi-Fi preference. Optional assets use the existing checksum-verified, resumable pack manager; downloads expose pause/resume and removal.
- An original OpenStax heart plate is displayed natively with bounded zoom/pan and attribution. A separate optional BodyParts3D pack renders five real source meshes, supports orbit/pan/pinch, hide/isolate and ray-to-triangle surface selection. It is a partial cardiac collection, not a complete heart or full-body atlas.
- Eight additional bilingual organ notes give each of the thirteen systems at least one organ. Fresh installs include the notes offline; existing installs receive them through the authoritative HTTPS snapshot. This is foundational coverage, not completion of the anatomy encyclopedia.
- Drug classes and interaction lookup use actual stored records. No published reviewed interaction facts are present; the UI explicitly states that absence of records does not prove absence of an interaction. No invented dose, product registration or indication is added.

## Live Neon changes

Migrations 013–015 are additive; 016 corrects two exact legacy captions without deleting records. All are checked twice on isolated branch `br-blue-sunset-b9i12jus` before applying to `br-steep-moon-b9kxm3uf`, project `icy-mode-02911943`, database `appdb`.

| Migration | Actual changes |
| --- | --- |
| 013 | Heart plate source, organ citation and verified media metadata |
| 014 | BodyParts3D source, media metadata, optional pack and file manifest, citations |
| 015 | Eight organs, eight sections, seven section-specific sources, citations and unreviewed attribution records |
| 016 | Correct two legacy schematic captions that incorrectly claimed scientific verification, conditional on their exact old titles |

Read-back after 015: 18 organs, zero systems without an organ, zero editorial reviews. No human review date was assigned to the new notes. Existing 18 diseases and eight active ingredients remain clinical drafts in Neon; the public API excludes those drafts. Locally bundled educational summaries are labelled unreviewed. These counts must not be described as clinically validated monographs.

The public transport remains Android → HTTPS Neon Function → Postgres → JSON snapshot → Room → Compose. PostgreSQL credentials remain server-side. The owner's prior authorized assignment is retained; this overhaul does not reassign roles.

## Asset provenance

| Asset | Size / SHA-256 | License and origin |
| --- | --- | --- |
| Original heart JPEG | 777,730 bytes · `5d962a658cab2538378fa846e2276c67389902c51968d64950873b9850938437` | OpenStax College, [original Commons file](https://commons.wikimedia.org/wiki/File:2008_Internal_Anatomy_of_the_HeartN.jpg), CC BY 3.0; unchanged JPEG, display scaling only |
| Optional cardiac GLB | 1,623,364 bytes · `b50c7a983e53f6e517b3174a81677bd6e2a68cef5d6253598563e5f64badc470` | Database Center for Life Science, BodyParts3D; [pinned source](https://github.com/Kevin-Mattheus-Moerman/BodyParts3D/tree/f0eeb6e843380cfe6b83797cf8c3e1af74de5e61), copied repository's CC BY-SA 2.1 Japan license retained |

GLB source parts are FMA3736, FMA3768, FMA7234, FMA7235 and FMA7246. Conversion welds vertices, computes normals, applies one shared coordinate transform/scale, and adds study colours. Geometry is not decimated. Input checksums and triangle counts are in `app/src/main/assets/anatomy/bodyparts-cardiac-source.json`; the GLB lives outside the APK in `public/anatomy/` and is downloaded only on request.

The newly consulted OpenStax text pages currently state CC BY-NC-SA 4.0. Their short bilingual adaptations retain that license and section attribution; this does not relicense application code or the older CC BY 3.0 image. Full reuse/license audit of legacy content remains outstanding. New notes leave unverified Latin/TA2 identifiers blank instead of fabricating them.

## Verification

- Local API/authorization/contract suite: 13 tests passed.
- JVM report from `7680994`: 29 tests passed, zero failed or ignored.
- Actual browser geometry implementation: two Node tests passed, covering nearest surface, hidden/isolation filtering, misses and inverse camera/aspect calculations.
- Generated Room v7 schema was retrieved from the real CI build and is committed for future migration comparison. Historical v1–v6 migration fixtures remain reconstructed, not historical customer databases.
- [Run 40226106473](https://github.com/Mtzallqmy/Ma/actions/runs/40226106473) passed Kotlin unit tests, Android Lint, security/dependency gates and debug assembly. Device tests exposed two new test defects (offscreen lazy-item lookup and non-void JUnit method); both were fixed in `58c219c`. This earlier pass is not evidence that later device tests passed.
- [Run 40226901646](https://github.com/Mtzallqmy/Ma/actions/runs/40226901646), commit `7680994`: all three jobs succeeded, including 24/24 device tests on API26 and 24/24 on API35. This includes real Neon snapshot→Room, persisted note/reader navigation, and optional five-mesh download/render with WebView networking disabled. Screenshots were initially lost during test-app uninstall; collection is corrected in the following batch and visual inspection remains pending.
- [Run 40254993753](https://github.com/Mtzallqmy/Ma/actions/runs/40254993753), commit `896053c`: all jobs passed and ten screenshots were retained. Visual review exposed wrapped large-font navigation labels and a model screenshot captured before presentation. The next batch uses compact navigation and a WebView visual-state callback, and asserts actual coloured model pixels rather than relying only on the GLB loaded callback.
- No public overhaul APK has been released.

## Remaining acceptance work

Independent clinical review, complete anatomical coverage and images for every organ, reviewed disease→drug indication links, real interaction coverage, authenticated editorial workflow with independent reviewer, physical ARM64 testing, slow-network/low-memory field testing and permanent signing are not complete. A successful software build cannot substitute for these checks. PR remains draft; no production-ready or full scientific-completion claim is made.
