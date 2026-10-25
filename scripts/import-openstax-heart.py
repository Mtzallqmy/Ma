#!/usr/bin/env python3
"""Import an attributed, unchanged CC BY 3.0 illustration; never generate anatomy."""
import hashlib
import json
from pathlib import Path
from urllib.request import Request, urlopen

URL = "https://upload.wikimedia.org/wikipedia/commons/9/92/2008_Internal_Anatomy_of_the_HeartN.jpg"
SOURCE = "https://commons.wikimedia.org/wiki/File:2008_Internal_Anatomy_of_the_HeartN.jpg"
request = Request(URL, headers={"User-Agent": "DawaHayah/1.0 educational asset import (github.com/Mtzallqmy/Ma)"})
with urlopen(request, timeout=45) as response:
    assert response.headers.get_content_type() == "image/jpeg"
    image = response.read(2 * 1024 * 1024 + 1)
assert 10000 < len(image) <= 2 * 1024 * 1024 and image[:2] == b"\xff\xd8"
folder = Path("app/src/main/assets/anatomy")
folder.mkdir(parents=True, exist_ok=True)
(folder / "openstax-heart.jpg").write_bytes(image)
metadata = {
    "title": "Internal Anatomy of the Heart",
    "author": "OpenStax College",
    "sourceUrl": SOURCE,
    "downloadUrl": URL,
    "license": "CC BY 3.0",
    "licenseUrl": "https://creativecommons.org/licenses/by/3.0/",
    "changes": "None. Original JPEG bytes; Android display scales the image.",
    "retrievedAt": "2026-10-25",
    "sizeBytes": len(image),
    "sha256": hashlib.sha256(image).hexdigest(),
}
(folder / "openstax-heart-source.json").write_text(json.dumps(metadata, indent=2) + "\n")
print(json.dumps(metadata))
