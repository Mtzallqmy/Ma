import html from './panel/index.html';
import css from './panel/panel.css';
import client from './panel/panel.js';
export function panelAsset(request) {
 const path=new URL(request.url).pathname;
 const headers={'Cache-Control':'no-store','X-Content-Type-Options':'nosniff','X-Frame-Options':'DENY','Referrer-Policy':'no-referrer','Content-Security-Policy':"default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self' https://ep-broad-violet-b96xtyh6.neonauth.c-7.eu-central-1.aws.neon.tech; frame-src https://ep-broad-violet-b96xtyh6.neonauth.c-7.eu-central-1.aws.neon.tech; object-src 'none'; base-uri 'none'; form-action 'self'"};
 if(request.method!=='GET')return null;
 if(path==='/'||path==='/admin')return new Response(html,{headers:{...headers,'Content-Type':'text/html; charset=utf-8'}});
 if(path==='/panel.js')return new Response(client,{headers:{...headers,'Content-Type':'text/javascript; charset=utf-8'}});
 if(path==='/panel.css')return new Response(css,{headers:{...headers,'Content-Type':'text/css; charset=utf-8'}});
 if(path==='/privacy')return new Response('الخصوصية: تسجيل الدخول عبر Neon Auth. لا نحفظ كلمة المرور أو نضع رموز الوصول في الروابط أو السجلات. إجراءات التحرير والنشر تحفظ في سجل التدقيق. لا تدخل بيانات مرضى.\nPrivacy: Sign-in uses Neon Auth. Passwords are not stored; access tokens are not written to URLs or logs. Editorial actions are audited. Do not enter patient data.',{headers:{...headers,'Content-Type':'text/plain; charset=utf-8'}});
 return null;
}
