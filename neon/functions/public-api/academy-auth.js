import { createRemoteJWKSet, jwtVerify } from 'jose';

let jwks;
export async function academyIdentity(request, verify) {
  const auth=request.headers.get('authorization');
  if(!auth?.startsWith('Bearer ') || auth.length>8192)throw Object.assign(new Error('Unauthorized'),{status:401});
  try {
    if(!verify) {
      if(!process.env.NEON_AUTH_JWKS_URL || !process.env.NEON_AUTH_BASE_URL)throw Object.assign(new Error('Auth unavailable'),{status:503});
      jwks ??= createRemoteJWKSet(new URL(process.env.NEON_AUTH_JWKS_URL));
      verify=token=>jwtVerify(token,jwks,{issuer:new URL(process.env.NEON_AUTH_BASE_URL).origin,algorithms:['EdDSA'],requiredClaims:['sub','exp','iat']});
    }
    const {payload}=await verify(auth.slice(7));
    if(typeof payload.sub!=='string' || !payload.sub.trim() || payload.sub.length>256)throw new Error('No user');
    return payload.sub;
  } catch(e) {throw Object.assign(new Error('Unauthorized'),{status:e.status===503?503:401});}
}
