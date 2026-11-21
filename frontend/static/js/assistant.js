/* Career assistant chat. The conversation is kept in this browser tab only (sessionStorage). */
(function () {
  'use strict';
  const KEY = 'gp-assistant-chat';

  document.addEventListener('DOMContentLoaded', () => {
    const box = document.getElementById('chat');
    const form = document.getElementById('chat-form');
    if (!box || !form) { return; }
    const input = form.elements.question;
    const greeting = { role: 'assistant', content: box.dataset.greeting, source: '' };
    let history;
    try { history = JSON.parse(sessionStorage.getItem(KEY) || 'null'); } catch (e) { history = null; }
    if (!Array.isArray(history) || history.length === 0) { history = [greeting]; }

    function save() { try { sessionStorage.setItem(KEY, JSON.stringify(history.slice(-30))); } catch (e) { /* storage unavailable */ } }

    function draw() {
      box.innerHTML = history.map(m => {
        const mine = m.role === 'user';
        const meta = mine ? 'You' : (m.source || 'Assistant');
        const notice = m.notice ? '<small>' + window.GP.escape(m.notice) + '</small>' : '';
        // Text is escaped first; only **bold** markers are then turned into <b> (no other HTML is ever inserted).
        const text = window.GP.escape(m.content).replace(/\*\*([^*\n]+)\*\*/g, '<b>$1</b>');
        return '<div class="bubble ' + (mine ? 'me' : 'them') + '">' + text
          + '<small>' + window.GP.escape(meta) + '</small>' + notice + '</div>';
      }).join('');
      box.scrollTop = box.scrollHeight;
    }

    async function ask(text) {
      const q = text.trim();
      if (!q) { return; }
      history.push({ role: 'user', content: q });
      draw();
      const button = form.querySelector('button[type=submit]');
      button.disabled = true;
      const thinking = document.createElement('div');
      thinking.className = 'bubble them';
      thinking.textContent = 'Thinking...';
      box.appendChild(thinking);
      box.scrollTop = box.scrollHeight;
      try {
        const turns = history.map(m => ({ role: m.role, content: m.content }));
        const r = await window.GP.postJson(form.dataset.url, { history: turns });
        history.push({ role: 'assistant', content: r.text, source: r.source, notice: r.notice });
      } catch (e) {
        history.push({ role: 'assistant', content: e.message, source: 'System' });
      } finally {
        button.disabled = false;
        save();
        draw();
        input.focus();
      }
    }

    form.addEventListener('submit', e => { e.preventDefault(); const t = input.value; input.value = ''; ask(t); });
    document.querySelectorAll('[data-suggest]').forEach(b => b.addEventListener('click', () => ask(b.dataset.suggest)));
    const clear = document.getElementById('chat-clear');
    if (clear) { clear.addEventListener('click', () => { history = [greeting]; save(); draw(); }); }
    draw();
  });
})();
