// FCM notification payloads are displayed by the browser through this worker.
// Only cache the public portal shell. Private Firestore and Functions requests bypass it.
const cacheName = 'center-one-shell-v1';
self.addEventListener('install', event => {
  event.waitUntil(caches.open(cacheName).then(cache => cache.addAll(['/','/manifest.json','/icon-192.png'])));
});
self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key.startsWith('center-one-shell-') && key !== cacheName).map(key => caches.delete(key)))));
});
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.origin !== self.location.origin) return;
  if (event.request.mode === 'navigate') {
    event.respondWith((async () => {
      try {
        const response = await fetch(event.request);
        if (response.ok && url.pathname === '/') {
          const cache = await caches.open(cacheName);
          await cache.put('/', response.clone());
        }
        return response;
      } catch {
        return await caches.match('/') || Response.error();
      }
    })());
  } else if (url.pathname.startsWith('/assets/')) {
    event.respondWith(caches.open(cacheName).then(async cache => {
      const cached = await cache.match(event.request);
      if (cached) return cached;
      const response = await fetch(event.request);
      if (response.ok) await cache.put(event.request, response.clone());
      return response;
    }));
  }
});
self.addEventListener('push', event => {
  const notification = event.data?.json()?.notification;
  if (!notification) return;
  event.waitUntil(self.registration.showNotification(notification.title, {body: notification.body, icon: '/icon-192.png'}));
});
self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil(self.clients.openWindow('/'));
});
