/* Messages: refresh the open thread every 15 seconds (and on demand) without reloading the page. */
(function () {
  'use strict';
  document.addEventListener('DOMContentLoaded', () => {
    const thread = document.getElementById('thread');
    if (!thread || !thread.dataset.url) { return; }
    const scroll = () => { thread.scrollTop = thread.scrollHeight; };
    async function refresh() {
      try {
        const res = await fetch(thread.dataset.url, { credentials: 'same-origin', headers: { Accept: 'text/html' } });
        if (!res.ok) { return; }
        const html = await res.text();
        const doc = new DOMParser().parseFromString(html, 'text/html');
        const fresh = doc.getElementById('thread');
        if (fresh && fresh.innerHTML !== thread.innerHTML) {
          const atBottom = thread.scrollHeight - thread.scrollTop - thread.clientHeight < 40;
          thread.innerHTML = fresh.innerHTML;
          window.GP.localTimes(thread);
          if (atBottom) { scroll(); }
        }
      } catch (e) { /* offline - try again later */ }
    }
    scroll();
    setInterval(() => { if (!document.hidden) { refresh(); } }, 15000);
    const btn = document.getElementById('thread-refresh');
    if (btn) { btn.addEventListener('click', refresh); }
  });
})();
