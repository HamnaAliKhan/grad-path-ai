/* What-if simulator: sends slider values to the server rule engine and shows the result. Nothing is saved. */
(function () {
  'use strict';
  document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('whatif');
    if (!form) { return; }
    const out = document.getElementById('whatif-result');
    let timer = null;

    function values() {
      const v = name => form.elements[name].value;
      return {
        cgpa: parseFloat(v('cgpa')), projects: parseInt(v('projects'), 10), internships: parseInt(v('internships'), 10),
        workMonths: parseInt(v('workMonths'), 10), confidentSkills: parseInt(v('confidentSkills'), 10),
        learningSkills: parseInt(v('learningSkills'), 10), certifications: parseInt(v('certifications'), 10),
        communication: parseInt(v('communication'), 10), extracurricular: parseInt(v('extracurricular'), 10)
      };
    }

    async function update() {
      out.setAttribute('aria-busy', 'true');
      try {
        const r = await window.GP.postJson(form.dataset.url, values());
        const sign = r.delta > 0 ? '+' : '';
        const rows = r.factors.map(f => '<tr><td>' + window.GP.escape(f.name) + '</td><td>' + f.points + ' / ' + f.max
          + '</td><td class="muted">' + window.GP.escape(f.rules) + '</td></tr>').join('');
        out.innerHTML = '<div class="row"><div><b style="font-size:1.6rem">' + window.GP.escape(r.scoreLabel) + '</b>'
          + ' <span class="muted">/ 100 &middot; ' + sign + r.delta + ' points compared with your saved profile</span></div>'
          + '<span class="badge b-' + window.GP.escape(r.badge) + '">' + window.GP.escape(r.category) + '</span></div>'
          + '<p class="muted small">' + window.GP.escape(r.calculation) + '</p>'
          + (r.modelPercent ? '<p class="small">Model placement estimate: <b>' + window.GP.escape(r.modelPercent) + '</b> '
            + '<span class="badge b-' + window.GP.escape(r.modelBadge) + '">' + window.GP.escape(r.modelBand) + '</span> '
            + '<span class="muted">(' + (r.modelDelta > 0 ? '+' : '') + Number(r.modelDelta) + ' percentage points; secondary, not your score)</span></p>' : '')
          + '<div class="tablewrap mt-s"><table class="table"><tr><th>Factor</th><th>Points</th><th>Rule</th></tr>' + rows + '</table></div>';
      } catch (e) {
        out.innerHTML = '<div class="flash error">' + window.GP.escape(e.message) + '</div>';
      } finally {
        out.removeAttribute('aria-busy');
      }
    }

    form.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(update, 250); });
    form.addEventListener('submit', e => { e.preventDefault(); update(); });
    form.addEventListener('reset', () => setTimeout(() => {
      form.querySelectorAll('input[type=range][data-out]').forEach(r => r.dispatchEvent(new Event('input', { bubbles: true })));
    }, 0));
    update();
  });
})();
