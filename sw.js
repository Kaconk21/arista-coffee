// Versi cache: naikkan setiap kali aset berubah.
const CACHE_NAME = "arista-pos-v13";
const ROOT = self.location.href;
const PRECACHE_URLS = ["./", "./index.html", "./config.js", "./manifest.json", "./icon/icon-192.png", "./icon/icon-512.png"]
  .map(p => new URL(p, ROOT).href);
const INDEX_URL = new URL("./index.html", ROOT).href;

function putInCache(request, response) {
  const copy = response.clone();
  caches.open(CACHE_NAME).then(c => c.put(request, copy)).catch(() => {});
}
self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(c => Promise.all(PRECACHE_URLS.map(u => c.add(u).catch(() => null))))
      .then(() => self.skipWaiting())
  );
});
self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});
self.addEventListener("fetch", event => {
  const request = event.request;
  if (request.method !== "GET") return;
  if (new URL(request.url).origin !== self.location.origin) return; // Supabase dan CDN tidak dicegat
  const isPage = request.mode === "navigate" || request.destination === "document";
  if (isPage) {
    event.respondWith(
      fetch(request)
        .then(res => { if (res.ok) putInCache(request, res); return res; })
        .catch(() => caches.match(request)
          .then(c => c || caches.match(INDEX_URL))
          .then(c => c || new Response("Offline", { status: 503 })))
    );
    return;
  }
  event.respondWith(
    caches.match(request).then(c => {
      if (c) return c;
      return fetch(request).then(res => { if (res.ok) putInCache(request, res); return res; });
    })
  );
});
