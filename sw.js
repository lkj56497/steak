const C="steak-v1";
const FILES=["./","index.html","manifest.webmanifest","icon-180.png","icon-192.png","icon-512.png"];
self.addEventListener("install",e=>{e.waitUntil(caches.open(C).then(c=>c.addAll(FILES)));self.skipWaiting()});
self.addEventListener("activate",e=>{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==C).map(k=>caches.delete(k)))));self.clients.claim()});
self.addEventListener("fetch",e=>{if(e.request.method!=="GET")return;
 const u=new URL(e.request.url);
 if(u.pathname.endsWith(".apk")||u.pathname.endsWith("install.html"))return;
 e.respondWith(caches.match(e.request,{ignoreSearch:true}).then(r=>r||fetch(e.request).then(res=>{
  if(res.ok&&(u.origin===location.origin||u.host.endsWith("gstatic.com")||u.host.endsWith("googleapis.com"))){const cp=res.clone();caches.open(C).then(c=>c.put(e.request,cp))}
  return res}).catch(()=>caches.match("index.html"))))});
