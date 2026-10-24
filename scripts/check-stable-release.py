"""Fail closed. CI secrets supply an independent approval for this exact commit."""
import json,os,re,sys
from pathlib import Path
state=json.loads(Path('release/readiness.json').read_text())
required=['stableReleaseApproved','qualifiedHumanClinicalReviewComplete','productionSigningKeyConfigured','realOwnerAndReviewerFlowVerified','physicalArm64DeviceVerified','licenseAuditComplete']
missing=[key for key in required if state.get(key) is not True]
sha=os.environ.get('GITHUB_SHA','')
if not re.fullmatch('[a-f0-9]{40}',sha) or os.environ.get('CLINICAL_REVIEW_APPROVED_SHA')!=sha:missing.append('independent clinical approval for this exact commit')
for key in ['ANDROID_RELEASE_KEYSTORE_BASE64','ANDROID_RELEASE_STORE_PASSWORD','ANDROID_RELEASE_KEY_ALIAS','ANDROID_RELEASE_KEY_PASSWORD']:
 if not os.environ.get(key):missing.append(key)
if missing:
 print('Stable release BLOCKED: '+', '.join(missing));sys.exit(1)
print('Stable release prerequisites supplied for the approved commit.')
