#!/usr/bin/env python3
"""Describe the educational data actually packaged in a release APK."""
import hashlib
import json
import sys
import zipfile

with zipfile.ZipFile(sys.argv[1]) as apk:
    paths = ["assets/seed/base_seed_v1.json", "assets/seed/academy_v1.json"]
    raw = {path: apk.read(path) for path in paths}
    base, academy = (json.loads(raw[path]) for path in paths)
    # Never allow an unreviewed release inventory to misrepresent clinical approval.
    for item in base["diseases"] + base["activeIngredients"] + academy["lessons"]:
        if item.get("reviewStatus") != "EDUCATIONAL_UNREVIEWED":
            raise ValueError("Unexpected review status in unreviewed educational release")
    if any(item.get("humanReviewedAt") for item in base["diseases"]):
        raise ValueError("Unexpected human review claim")
    identifiers = [
        {"ingredientId": item["id"], "name": item["nameEn"], **identifier}
        for item in base["activeIngredients"]
        for identifier in item.get("details", {}).get("identifiers", [])
    ]
    inventory = {
        "schemaVersion": 1,
        "distribution": "educational-unreviewed-prerelease",
        "clinicalReviewComplete": False,
        "scope": "Bundled educational content, not a clinical validation or a complete source import",
        "counts": {
            **{key: len(base[key]) for key in ("systems", "organs", "diseases", "activeIngredients", "sources")},
            "courses": len(academy["courses"]),
            "lessons": len(academy["lessons"]),
            "questions": sum(len(item["questions"]) for item in academy["lessons"]),
        },
        "bundledFiles": [{"path": path, "sizeBytes": len(data), "sha256": hashlib.sha256(data).hexdigest()} for path, data in raw.items()],
        "ingredientIdentifiers": identifiers,
        "sources": base["sources"],
    }
    print(json.dumps(inventory, ensure_ascii=False, indent=2))
