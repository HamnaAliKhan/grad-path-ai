/* Renders server-built Plotly specs from [data-chart] attributes using the shared dark-blue theme. */
(function () {
  'use strict';
  const C = { text: '#E8ECF8', muted: '#93A0BD', border: '#26315A' };

  function themed(layout) {
    const axis = { gridcolor: C.border, zerolinecolor: C.border, linecolor: C.border, color: C.text, automargin: true, fixedrange: true };
    const base = {
      paper_bgcolor: 'rgba(0,0,0,0)', plot_bgcolor: 'rgba(0,0,0,0)',
      font: { color: C.text, family: 'Inter, system-ui, sans-serif' },
      margin: { l: 56, r: 24, t: 50, b: 56 },
      legend: { bgcolor: 'rgba(0,0,0,0)', font: { color: C.text }, orientation: 'h', y: -0.18 },
      hoverlabel: { bgcolor: '#141C38', bordercolor: C.border, font: { color: C.text } },
      dragmode: false
    };
    const out = Object.assign({}, base, layout || {});
    if (typeof out.title === 'string') { out.title = { text: out.title, font: { size: 15, color: C.text }, x: 0.02 }; }
    out.xaxis = Object.assign({}, axis, (layout || {}).xaxis || {});
    out.yaxis = Object.assign({}, axis, (layout || {}).yaxis || {});
    return out;
  }

  function render(el) {
    let spec;
    try { spec = JSON.parse(el.dataset.chart); } catch (e) { return; }
    if (!window.Plotly) {
      el.innerHTML = '<div class="chart-fallback">Charts need an internet connection to load the Plotly library. The figures are also listed on this page.</div>';
      return;
    }
    (spec.data || []).forEach(t => { if (t.type === 'pie') { t.textfont = { color: C.text }; } });
    window.Plotly.newPlot(el, spec.data || [], themed(spec.layout), {
      displayModeBar: false, responsive: true, scrollZoom: false, doubleClick: false, showTips: false
    });
  }

  function renderAll() { document.querySelectorAll('[data-chart]').forEach(render); }

  window.GP = window.GP || {};
  window.GP.renderChart = render;
  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', renderAll); } else { renderAll(); }
})();
