import hashlib,json,os,subprocess,sys,tempfile,unittest,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
FLAGS=['stableReleaseApproved','qualifiedHumanClinicalReviewComplete','productionSigningKeyConfigured','realOwnerAndReviewerFlowVerified','physicalArm64DeviceVerified','licenseAuditComplete']
class ReleaseGates(unittest.TestCase):
 def check_gate(self,state,approved=True):
  with tempfile.TemporaryDirectory() as d:
   p=Path(d)/'release';p.mkdir();(p/'readiness.json').write_text(json.dumps(state))
   env={**os.environ,'GITHUB_SHA':'a'*40,'CLINICAL_REVIEW_APPROVED_SHA':('a' if approved else 'b')*40}
   for key in ['ANDROID_RELEASE_KEYSTORE_BASE64','ANDROID_RELEASE_STORE_PASSWORD','ANDROID_RELEASE_KEY_ALIAS','ANDROID_RELEASE_KEY_PASSWORD']:env[key]='software-test-fixture'
   return subprocess.run([sys.executable,str(ROOT/'scripts/check-stable-release.py')],cwd=d,env=env,capture_output=True).returncode
 def test_repository_state_blocks_stable(self):self.assertNotEqual(self.check_gate(json.loads((ROOT/'release/readiness.json').read_text())),0)
 def test_each_missing_approval_blocks(self):
  for flag in FLAGS:
   state=dict.fromkeys(FLAGS,True);state[flag]=False;self.assertNotEqual(self.check_gate(state),0)
 def test_different_commit_cannot_reuse_approval(self):self.assertNotEqual(self.check_gate(dict.fromkeys(FLAGS,True),False),0)
 def test_all_explicit_prerequisites_reach_signing_step(self):self.assertEqual(self.check_gate(dict.fromkeys(FLAGS,True)),0)
 def test_sbom_inventory_matches_file_bytes(self):
  with tempfile.TemporaryDirectory() as d:
   apk=Path(d)/'fixture.apk'
   with zipfile.ZipFile(apk,'w') as z:z.writestr('fixture.txt',b'original test bytes')
   result=json.loads(subprocess.check_output([sys.executable,str(ROOT/'scripts/apk-sbom.py'),str(apk)]))
   self.assertEqual(result['spdxVersion'],'SPDX-2.3');self.assertEqual(result['files'][0]['checksums'][0]['checksumValue'],hashlib.sha256(b'original test bytes').hexdigest())
if __name__=='__main__':unittest.main()
