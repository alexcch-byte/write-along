/*
 * Write Along — web version. Same lessons as the Android app:
 * Print and Cursive letters (Watch, Trace, Write), Assignments and Free Write.
 */
(function () {
  'use strict';

  const { Glyphs, layoutText, Sheets, checkTrace, scoreCell, praise, TRACE_MSG, Assignments, pathLength, prefix, pointAlong, centroid, P, dist } = window.WA;

  const REPO_URL = 'https://github.com/alexcch-byte/write-along';
  const APK_URL = REPO_URL + '/releases/latest/download/WriteAlong.apk';

  const C = {
    text: '#2B2D42', sub: '#6B6F80', primary: '#5B6CF0', print: '#FF8A3D', cursive: '#9B5DE5', calligraphy: '#C0395F', free: '#14B8A6',
    good: '#22A861', warn: '#E9A23B', bad: '#E5484D', star: '#FFC233', starEmpty: '#E3DED3',
    topLine: '#5B6CF0', midLine: '#8C98F5', baseLine: '#E5484D', tailLine: '#C9C3B8', zone: 'rgba(91,108,240,0.06)',
    road: '#E4E1DA', roadActive: '#FFE3A3', roadDone: '#C8EED6', dotted: '#9A97A0', faint: 'rgba(133,130,144,0.22)',
    arrow: '#55525C', startDot: '#22A861', stopDot: '#E5484D', ink: '#2B2D42',
  };
  const PENS = ['#2B2D42', '#2F6BFF', '#E63946', '#16A34A', '#8E44AD'];
  const scriptColor = (s) => C[s] || C.cursive;
  const scriptLabel = (s) => ({ print: 'Print', cursive: 'Cursive', calligraphy: 'Calligraphy' }[s] || 'Print');
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

  /* ------------------------------------------------------------------ storage */

  const store = {
    get(k, d) {
      try { const v = localStorage.getItem('wa.' + k); return v == null ? d : JSON.parse(v); } catch (e) { return d; }
    },
    set(k, v) {
      try { localStorage.setItem('wa.' + k, JSON.stringify(v)); } catch (e) { /* private mode: keep going */ }
    },
  };

  const Progress = {
    stars: store.get('stars', {}),
    key: (script, ch) => 's_' + script + '_' + ch.charCodeAt(0),
    letter(script, ch) { return this.stars[this.key(script, ch)] || 0; },
    record(script, ch, n) { this.bump(this.key(script, ch), n); },
    assignment(id) { return this.stars['a_' + id] || 0; },
    recordAssignment(id, n) { this.bump('a_' + id, n); },
    bump(k, n) {
      if (n > (this.stars[k] || 0)) { this.stars[k] = n; store.set('stars', this.stars); }
    },
    total(script) { return Glyphs.lowercase.concat(Glyphs.uppercase).reduce((s, ch) => s + this.letter(script, ch), 0); },
  };

  /* ------------------------------------------------------------------ voice */

  const Speech = {
    on: store.get('voice', true),
    ok: 'speechSynthesis' in window,
    voice: null,
    pickVoice() {
      if (!this.ok) return;
      const vs = speechSynthesis.getVoices();
      const gb = vs.filter((v) => /^en[-_]GB$/i.test(v.lang));
      const male = /(?<!fe)male|daniel|george|ryan|thomas|oliver|arthur|rishi|alfie|noel/i;
      const female = /female|libby|sonia|maisie|hazel|susan|kate|serena|stephanie|martha|fiona|amy|emma|google uk english female/i;
      // A British female voice if the device has one; otherwise any British voice that is not male.
      this.voice = gb.find((v) => female.test(v.name) && !male.test(v.name)) ||
        gb.find((v) => !male.test(v.name)) || gb[0] ||
        vs.find((v) => /^en/i.test(v.lang) && female.test(v.name)) || vs.find((v) => /^en/i.test(v.lang)) || null;
    },
    serial: 0,
    say(text) {
      if (!this.on || !this.ok || !text) return;
      const id = ++this.serial;
      const u = new SpeechSynthesisUtterance(text);
      u.rate = 0.9; u.pitch = 1.1; u.lang = 'en-GB';
      if (this.voice) u.voice = this.voice;
      if (speechSynthesis.speaking || speechSynthesis.pending) {
        // Chrome can drop a sentence spoken right after cancel(); give it a moment.
        speechSynthesis.cancel();
        setTimeout(() => { if (id === this.serial) speechSynthesis.speak(u); }, 90);
      } else {
        speechSynthesis.speak(u);
      }
    },
    stop() { this.serial++; if (this.ok) speechSynthesis.cancel(); },
    async done(maxMs = 9000) {
      await sleep(250);
      let w = 0;
      while (this.on && this.ok && speechSynthesis.speaking && w < maxMs) { await sleep(100); w += 100; }
    },
    toggle() {
      this.on = !this.on;
      store.set('voice', this.on);
      if (this.on) this.say('Voice is on.'); else this.stop();
    },
  };
  if (Speech.ok) {
    Speech.pickVoice();
    speechSynthesis.addEventListener && speechSynthesis.addEventListener('voiceschanged', () => Speech.pickVoice());
  }

  /* ------------------------------------------------------------------ DOM helpers */

  function h(tag, props, ...kids) {
    const el = document.createElement(tag);
    if (props) {
      for (const [k, v] of Object.entries(props)) {
        if (v == null || v === false) continue;
        if (k === 'class') el.className = v;
        else if (k.startsWith('--')) el.style.setProperty(k, v);
        else if (k === 'style') Object.assign(el.style, v);
        else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
        else el.setAttribute(k, v === true ? '' : v);
      }
    }
    for (const kid of kids.flat(Infinity)) {
      if (kid == null || kid === false) continue;
      el.append(kid instanceof Node ? kid : document.createTextNode(String(kid)));
    }
    return el;
  }

  function btn(label, onClick, o = {}) {
    return h('button', {
      type: 'button', class: 'kbtn' + (o.selected || o.filled ? ' solid' : '') + (o.cls ? ' ' + o.cls : ''),
      '--c': o.color || C.primary, onclick: onClick, disabled: o.disabled || null, 'aria-pressed': o.selected == null ? null : String(!!o.selected),
    }, o.emoji ? h('span', { class: 'emo', 'aria-hidden': 'true' }, o.emoji) : null, h('span', { class: 'lbl' }, label));
  }

  function roundBtn(emoji, label, onClick, cls) {
    return h('button', { type: 'button', class: 'rbtn' + (cls ? ' ' + cls : ''), 'aria-label': label, title: label, onclick: onClick }, emoji);
  }

  function choiceRow(options, selected, label, onSelect, color, disabled) {
    return h('div', { class: 'choices' }, options.map((o) => btn(label(o), () => onSelect(o), { color, selected: o === selected, disabled })));
  }

  function voiceToggle() {
    const b = roundBtn(Speech.on ? '🔊' : '🔇', Speech.on ? 'Turn voice off' : 'Turn voice on', () => {
      Speech.toggle();
      b.textContent = Speech.on ? '🔊' : '🔇';
      b.setAttribute('aria-label', Speech.on ? 'Turn voice off' : 'Turn voice on');
    });
    return b;
  }

  function starPoints(cx, cy, outer) {
    const inner = outer * 0.48;
    const pts = [];
    for (let i = 0; i < 10; i++) {
      const r = i % 2 === 0 ? outer : inner;
      const a = -Math.PI / 2 + (i * Math.PI) / 5;
      pts.push([cx + r * Math.cos(a), cy + r * Math.sin(a)]);
    }
    return pts;
  }

  function starsEl(earned, size) {
    const span = h('span', { class: 'stars', role: 'img', 'aria-label': `${earned} of 3 stars` });
    let svg = `<svg width="${size * 3 + 4}" height="${size}" viewBox="0 0 ${size * 3 + 4} ${size}">`;
    for (let i = 0; i < 3; i++) {
      const pts = starPoints(size / 2 + i * (size + 2), size / 2 + size * 0.04, size * 0.48).map((p) => p.map((v) => v.toFixed(1)).join(',')).join(' ');
      svg += `<polygon points="${pts}" fill="${i < earned ? C.star : C.starEmpty}" stroke="rgba(0,0,0,.2)" stroke-width="0.8" stroke-linejoin="round"/>`;
    }
    span.innerHTML = svg + '</svg>';
    return span;
  }

  function sectionLabel(t) { return h('div', { class: 'sec-label' }, t); }

  function setupCanvas(cv) {
    const r = cv.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    const w = Math.max(1, Math.round(r.width * dpr)), hh = Math.max(1, Math.round(r.height * dpr));
    if (cv.width !== w || cv.height !== hh) { cv.width = w; cv.height = hh; }
    const ctx = cv.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    return { ctx, w: r.width, h: r.height };
  }

  /** Small picture of letters on writing lines. */
  function letterArt(strokes, bounds, color, o = {}) {
    const height = o.height || 84;
    const cv = h('canvas', { class: 'art', style: { height: height + 'px' }, 'aria-hidden': 'true' });
    const draw = () => {
      if (!cv.isConnected) return;
      const { ctx, w, h: H } = setupCanvas(cv);
      if (w < 2) return;
      ctx.clearRect(0, 0, w, H);
      const top = -18, bottom = 160;
      const u = Math.min(H / (bottom - top), w / (bounds.w + (o.pad || 30)));
      const oy = (H - (bottom - top) * u) / 2 - top * u;
      const ox = w / 2 - bounds.cx * u;
      const lw = Math.max(1.2 * u, 1);
      line(ctx, 0, oy, w, oy, 'rgba(91,108,240,.7)', lw);
      line(ctx, 0, oy + 50 * u, w, oy + 50 * u, 'rgba(140,152,245,.8)', lw, [6 * u + 3, 5 * u + 2]);
      line(ctx, 0, oy + 100 * u, w, oy + 100 * u, 'rgba(229,72,77,.8)', lw * 1.2);
      const ww = Math.max((o.weight || 8) * u, 2) * (o.nib ? 0.75 : 1);
      for (const l of strokes) drawPen(ctx, !!o.nib, l.map((p) => P(ox + p.x * u, oy + p.y * u)), color, ww);
    };
    new ResizeObserver(draw).observe(cv);
    return cv;
  }

  /* ------------------------------------------------------------------ canvas drawing */

  function line(ctx, x0, y0, x1, y1, color, width, dash) {
    ctx.save();
    ctx.strokeStyle = color; ctx.lineWidth = width;
    if (dash) ctx.setLineDash(dash);
    ctx.beginPath(); ctx.moveTo(x0, y0); ctx.lineTo(x1, y1); ctx.stroke();
    ctx.restore();
  }

  function polyPath(ctx, pts) {
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length; i++) ctx.lineTo(pts[i].x, pts[i].y);
  }

  function strokeRound(ctx, color, width, alpha) {
    ctx.save();
    ctx.globalAlpha = alpha == null ? 1 : alpha;
    ctx.strokeStyle = color; ctx.lineWidth = width; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    ctx.stroke();
    ctx.restore();
  }

  function circle(ctx, p, r, color, alpha) {
    ctx.save();
    ctx.globalAlpha = alpha == null ? 1 : alpha;
    ctx.fillStyle = color;
    ctx.beginPath(); ctx.arc(p.x, p.y, r, 0, Math.PI * 2); ctx.fill();
    ctx.restore();
  }

  function drawPaperRow(ctx, row) {
    const u = row.unit;
    const heavy = Math.max(1.7 * u, 1.5), light = Math.max(1.3 * u, 1.2);
    ctx.fillStyle = C.zone;
    ctx.fillRect(row.left, row.midLine, row.right - row.left, row.baseLine - row.midLine);
    line(ctx, row.left, row.topLine, row.right, row.topLine, C.topLine, heavy);
    const dash = Math.max(9 * u, 7);
    line(ctx, row.left, row.midLine, row.right, row.midLine, C.midLine, light, [dash, dash * 0.75]);
    line(ctx, row.left, row.baseLine, row.right, row.baseLine, C.baseLine, heavy * 1.15);
    line(ctx, row.left, row.tailLine, row.right, row.tailLine, C.tailLine, light * 0.8, [dash * 0.4, dash * 0.6]);
  }

  function drawRoad(ctx, pts, isDot, color, width) {
    if (isDot || pts.length < 2) { circle(ctx, pts[0], width * 0.62, color); return; }
    polyPath(ctx, pts);
    strokeRound(ctx, color, width);
  }

  function drawDotted(ctx, pts, isDot, color, width, gap) {
    if (isDot || pts.length < 2) { circle(ctx, pts[0], width * 1.3, color); return; }
    ctx.save();
    ctx.setLineDash([0.001, gap]);
    polyPath(ctx, pts);
    strokeRound(ctx, color, width);
    ctx.restore();
  }

  function drawInk(ctx, pts, color, width, alpha) {
    if (!pts.length) return;
    if (pts.length === 1 || pathLength(pts) < width * 0.5) { circle(ctx, pts[0], width * 0.6, color, alpha); return; }
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    if (pts.length < 3) {
      for (let i = 1; i < pts.length; i++) ctx.lineTo(pts[i].x, pts[i].y);
    } else {
      for (let i = 1; i < pts.length - 1; i++) {
        const a = pts[i], b = pts[i + 1];
        ctx.quadraticCurveTo(a.x, a.y, (a.x + b.x) / 2, (a.y + b.y) / 2);
      }
      const last = pts[pts.length - 1];
      ctx.lineTo(last.x, last.y);
    }
    strokeRound(ctx, color, width, alpha);
  }

  /* A flat, broad-edge calligraphy pen: thick across the nib, hair thin along it. */
  const NIB_ANGLE = 45, NIB_MIN = 0.16, NIB_RATIO = 1.5;

  function drawPen(ctx, nib, pts, color, width, alpha) {
    if (nib) drawNib(ctx, pts, color, width * NIB_RATIO, alpha); else drawInk(ctx, pts, color, width, alpha);
  }

  function drawNib(ctx, pts, color, nibLen, alpha) {
    if (!pts.length) return;
    const ang = NIB_ANGLE * Math.PI / 180;
    const hx = Math.cos(ang) * nibLen / 2, hy = -Math.sin(ang) * nibLen / 2;
    const first = pts[0];
    ctx.save();
    ctx.globalAlpha = alpha == null ? 1 : alpha;
    if (pts.length === 1 || pathLength(pts) < nibLen * 0.3) {
      ctx.strokeStyle = color; ctx.lineWidth = nibLen * 0.34; ctx.lineCap = 'butt';
      ctx.beginPath(); ctx.moveTo(first.x - hx * 0.9, first.y - hy * 0.9); ctx.lineTo(first.x + hx * 0.9, first.y + hy * 0.9); ctx.stroke();
      ctx.restore();
      return;
    }
    const sm = pts.map((b, i) => {
      if (i === 0 || i === pts.length - 1) return b;
      const a = pts[i - 1], c = pts[i + 1];
      return P((a.x + 2 * b.x + c.x) / 4, (a.y + 2 * b.y + c.y) / 4);
    });
    const minStep = Math.max(nibLen / 14, 1);
    const line = [sm[0]];
    for (let i = 1; i < sm.length; i++) {
      if (dist(sm[i], line[line.length - 1]) >= minStep || i === sm.length - 1) line.push(sm[i]);
    }
    // Hair line under the body so strokes along the nib edge never vanish.
    polyPath(ctx, line);
    strokeRound(ctx, color, Math.max(nibLen * NIB_MIN, 1));
    ctx.fillStyle = color;
    ctx.beginPath();
    for (let i = 1; i < line.length; i++) {
      const p = line[i - 1], q = line[i];
      let v = [[p.x - hx, p.y - hy], [p.x + hx, p.y + hy], [q.x + hx, q.y + hy], [q.x - hx, q.y - hy]];
      let area = 0;
      for (let k = 0; k < 4; k++) { const a = v[k], b = v[(k + 1) % 4]; area += a[0] * b[1] - b[0] * a[1]; }
      if (area < 0) v = v.reverse();   // every piece wound the same way, so pieces never cancel
      ctx.moveTo(v[0][0], v[0][1]);
      for (let k = 1; k < 4; k++) ctx.lineTo(v[k][0], v[k][1]);
      ctx.closePath();
    }
    ctx.fill('nonzero');
    ctx.restore();
  }

  function drawArrows(ctx, pts, color, size, spacing, startAt) {
    if (pts.length < 2) return;
    const len = pathLength(pts);
    let d = startAt == null ? spacing * 0.6 : startAt;
    const w = Math.max(size * 0.28, 1.5);
    while (d < len - size * 0.8) {
      const { p, dir } = pointAlong(pts, d);
      const tip = P(p.x + dir.x * size * 0.45, p.y + dir.y * size * 0.45);
      const bx = -dir.x * size, by = -dir.y * size;
      const sx = -dir.y * size * 0.62, sy = dir.x * size * 0.62;
      ctx.save();
      ctx.strokeStyle = color; ctx.lineWidth = w; ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.moveTo(tip.x + bx + sx, tip.y + by + sy); ctx.lineTo(tip.x, tip.y); ctx.lineTo(tip.x + bx - sx, tip.y + by - sy);
      ctx.stroke();
      ctx.restore();
      d += spacing;
    }
  }

  function drawBadge(ctx, p, r, n, color) {
    circle(ctx, p, r * 1.18, '#fff');
    circle(ctx, p, r, color);
    ctx.save();
    ctx.fillStyle = '#fff';
    ctx.font = `800 ${Math.round(r * 1.25)}px ui-rounded, "Segoe UI", system-ui, sans-serif`;
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    ctx.fillText(String(n), p.x, p.y + r * 0.06);
    ctx.restore();
  }

  function drawStars(ctx, cx, cy, r, earned) {
    const gap = r * 2.25;
    const start = cx - gap;
    for (let i = 0; i < 3; i++) {
      const pts = starPoints(start + i * gap, cy, r);
      ctx.beginPath();
      pts.forEach(([x, y], k) => (k ? ctx.lineTo(x, y) : ctx.moveTo(x, y)));
      ctx.closePath();
      ctx.fillStyle = i < earned ? C.star : C.starEmpty;
      ctx.fill();
      ctx.strokeStyle = 'rgba(0,0,0,.2)'; ctx.lineWidth = Math.max(r * 0.08, 1); ctx.lineJoin = 'round';
      ctx.stroke();
    }
  }

  /* ------------------------------------------------------------------ boards */

  const activeBoards = new Set();
  function frame(now) {
    for (const b of activeBoards) b.draw(now);
    requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);

  /**
   * A writing surface: the lines and guides, the child's ink, and (for tracing) which stroke
   * we're waiting for. mode: 'trace' | 'write' | 'free'.
   */
  class Board {
    constructor(o) {
      this.mode = o.mode;
      this.glyph = o.glyph || null;
      this.words = !!o.words;
      this.build = o.build;
      this.intro = o.intro || null;
      this.onMessage = o.onMessage || (() => {});
      this.onFinished = o.onFinished || (() => {});
      this.pen = o.pen || C.ink;
      this.minTol = 12;
      this.el = h('div', { class: 'board' });
      this.canvas = h('canvas', { class: 'board-canvas', 'aria-label': 'Writing area' });
      this.el.append(this.canvas);
      this.reset();
      this.stylusSeen = false;
      this.pointer = null;
      this.began = false;
      this.ro = new ResizeObserver(() => this.layout());
      this.ro.observe(this.el);
      activeBoards.add(this);

      const cv = this.canvas;
      const pt = (e) => { const r = cv.getBoundingClientRect(); return P(e.clientX - r.left, e.clientY - r.top); };
      cv.addEventListener('pointerdown', (e) => {
        if (e.pointerType === 'pen') this.stylusSeen = true;
        if (this.stylusSeen && e.pointerType === 'touch') return; // palm rejection
        if (this.pointer != null || e.button > 0) return;
        e.preventDefault();
        try { cv.setPointerCapture(e.pointerId); } catch (err) { /* ignore */ }
        this.pointer = e.pointerId;
        this.down(pt(e));
      });
      cv.addEventListener('pointermove', (e) => {
        if (e.pointerId !== this.pointer) return;
        const evs = e.getCoalescedEvents ? e.getCoalescedEvents() : [e];
        for (const ev of (evs.length ? evs : [e])) this.move(pt(ev));
      });
      const end = (e, cancel) => {
        if (e.pointerId !== this.pointer) return;
        this.pointer = null;
        if (cancel) this.current = null; else this.up();
      };
      cv.addEventListener('pointerup', (e) => end(e, false));
      cv.addEventListener('pointercancel', (e) => end(e, true));
      cv.addEventListener('contextmenu', (e) => e.preventDefault());
    }

    reset() {
      this.inks = [];
      this.current = null;
      this.rejected = null;
      this.rejectAt = 0;
      this.cellIdx = 0;
      this.strokeIdx = 0;
      this.results = null;
      this.praiseSeed = 0;
    }

    layout() {
      const r = this.el.getBoundingClientRect();
      if (r.width < 20 || r.height < 20) return;
      if (this.sheet && Math.abs(r.width - this.w) < 1 && Math.abs(r.height - this.h) < 1) return;
      this.w = r.width;
      this.h = r.height;
      this.sheet = this.build(this.w, this.h);
      this.reset();
      if (!this.began) { this.began = true; this.begin(); } else if (this.mode === 'trace') this.announce(false);
    }

    destroy() { this.ro.disconnect(); activeBoards.delete(this); }

    get traceDone() { return this.mode === 'trace' && this.sheet && this.cellIdx >= this.sheet.cells.length; }

    begin() {
      if (this.mode === 'trace') this.announce(true);
      else if (this.mode === 'write') {
        this.onMessage(this.intro || "Trace the dotted letter, then write it by yourself in the empty spaces. Tap Check when you're done.", true);
      }
    }

    announce(speak) {
      const g = this.glyph;
      if (!g || this.traceDone) return;
      const n = g.strokeCount;
      this.onMessage((n > 1 ? `Stroke ${this.strokeIdx + 1} of ${n}. ` : '') + g.instruction(this.strokeIdx), speak);
    }

    down(p) { if (!this.sheet || this.traceDone) return; this.current = [p]; }
    move(p) {
      if (!this.current) return;
      const last = this.current[this.current.length - 1];
      if (Math.hypot(p.x - last.x, p.y - last.y) >= 0.75) this.current.push(p);
    }
    up() {
      const pts = this.current;
      this.current = null;
      if (!pts || !pts.length) return;
      if (this.mode === 'trace') this.handleTrace(pts);
      else { this.inks.push({ points: pts, color: this.pen }); this.results = null; }
    }

    handleTrace(pts) {
      const cell = this.sheet.cells[this.cellIdx];
      if (!cell) return;
      const r = checkTrace(pts, cell.strokes[this.strokeIdx], cell.unit, this.minTol);
      if (!r.ok) {
        this.rejected = pts;
        this.rejectAt = performance.now();
        this.onMessage(TRACE_MSG[r.problem] || 'Try again!', true);
        return;
      }
      this.rejected = null;
      this.inks.push({ points: pts, color: this.pen });
      if (this.strokeIdx + 1 < cell.strokes.length) {
        this.strokeIdx++;
        if (this.cellIdx === 0) this.announce(true);
        else this.onMessage('Next stroke! ' + this.glyph.instruction(this.strokeIdx), true);
        return;
      }
      this.strokeIdx = 0;
      this.cellIdx++;
      if (this.cellIdx >= this.sheet.cells.length) {
        this.onMessage('You traced it! Great job!', true);
        this.onFinished(3);
      } else {
        this.onMessage(praise(this.praiseSeed++) + ' Now trace the next one.', true);
      }
    }

    undo() {
      if (this.mode === 'trace') return;
      this.inks.pop();
      this.results = null;
    }

    check() {
      if (this.mode !== 'write' || !this.sheet) return;
      const groups = new Map();
      for (const ink of this.inks) {
        const cell = this.cellFor(ink.points);
        if (!cell) continue;
        if (!groups.has(cell.index)) groups.set(cell.index, []);
        groups.get(cell.index).push(ink.points);
      }
      const scores = new Map();
      for (const cell of this.sheet.cells) {
        const s = scoreCell(groups.get(cell.index) || [], cell, this.minTol, this.words);
        if (s) scores.set(cell.index, s);
      }
      this.results = scores;
      if (!scores.size) {
        this.onMessage(this.words ? 'Write it on the lines first, then tap Check.' : 'Write the letter in the spaces first, then tap Check.', true);
        return;
      }
      const vals = [...scores.values()];
      // Three stars overall only when every letter earned three.
      let overall = Math.max(0, Math.min(3, Math.round(vals.reduce((s, v) => s + v.stars, 0) / vals.length)));
      if (overall === 3 && vals.some((v) => v.stars < 3)) overall = 2;
      const counts = new Map();
      vals.filter((v) => v.stars < 3).forEach((v) => counts.set(v.tip, (counts.get(v.tip) || 0) + 1));
      let tip = null, best = 0;
      counts.forEach((n, t) => { if (n > best) { best = n; tip = t; } });
      const msg = tip == null
        ? (this.words ? 'Amazing! Your writing looks great!' : 'Amazing! Every letter looks great!')
        : overall >= 2 ? 'Nice work! Tip: ' + tip : 'Good try! ' + tip;
      this.onMessage(msg, true);
      this.onFinished(overall);
    }

    cellFor(points) {
      if (!points.length) return null;
      const c = centroid(points);
      let row = null, bestD = Infinity;
      for (const r of this.sheet.rows) {
        const d = Math.abs(c.y - (r.topLine + 75 * r.unit));
        if (d < bestD) { bestD = d; row = r; }
      }
      let cell = null; bestD = Infinity;
      for (const k of this.sheet.cells) {
        if (k.row !== row) continue;
        const d = Math.abs(k.centerX - c.x);
        if (d < bestD) { bestD = d; cell = k; }
      }
      return cell;
    }

    draw(now) {
      if (!this.sheet) return;
      const { ctx, w, h: H } = setupCanvas(this.canvas);
      ctx.clearRect(0, 0, w, H);
      const sheet = this.sheet;
      const pulse = this.mode === 'trace' ? (Math.sin(now / 650 * Math.PI) + 1) / 2 : 0;

      if (this.results) {
        for (const [idx, sc] of this.results) {
          const cell = sheet.cells[idx];
          const tint = [C.bad, C.warn, C.star, C.good][sc.stars];
          ctx.save();
          ctx.globalAlpha = 0.13; ctx.fillStyle = tint;
          roundRect(ctx, cell.left + 3, cell.row.top, cell.width - 6, cell.row.bottom - cell.row.top, 14);
          ctx.fill();
          ctx.restore();
        }
      }
      for (const row of sheet.rows) drawPaperRow(ctx, row);
      for (const cell of sheet.cells) this.drawGuide(ctx, cell, pulse);

      const inkW = Math.max(5.5 * sheet.rows[0].unit, 3.5);
      for (const ink of this.inks) drawPen(ctx, sheet.nib, ink.points, ink.color, inkW);
      if (this.rejected) {
        const t = now - this.rejectAt;
        const alpha = t < 300 ? 1 : Math.max(0, 1 - (t - 300) / 700);
        if (alpha > 0.01) drawPen(ctx, sheet.nib, this.rejected, C.bad, inkW, alpha); else this.rejected = null;
      }
      if (this.current) drawPen(ctx, sheet.nib, this.current, this.pen, inkW);

      if (this.results) {
        for (const [idx, sc] of this.results) {
          const cell = sheet.cells[idx];
          const u = cell.unit;
          const r = Math.min(Math.max(8 * u, 7), cell.width / 9);
          drawStars(ctx, cell.centerX, cell.row.top + Math.max(13 * u, r * 1.2), r, sc.stars);
        }
      }
    }

    drawGuide(ctx, cell, pulse) {
      const u = cell.unit;
      if (cell.guide === 'trace') {
        const roadW = Math.max(13 * u, 15);
        const active = this.mode === 'trace' && cell.index === this.cellIdx;
        const done = this.mode === 'trace' && cell.index < this.cellIdx;
        cell.strokes.forEach((s, i) => {
          const color = done || (active && i < this.strokeIdx) ? C.roadDone : active && i === this.strokeIdx ? C.roadActive : C.road;
          drawRoad(ctx, s.points, s.isDot, color, roadW);
        });
        if (active) {
          const badgeR = Math.max(7.5 * u, 10);
          for (let i = this.strokeIdx + 1; i < cell.strokes.length; i++) {
            drawBadge(ctx, cell.strokes[i].points[0], badgeR * 0.72, cell.strokes[i].number, C.dotted);
          }
          const s = cell.strokes[this.strokeIdx];
          if (!s.isDot) {
            drawArrows(ctx, s.points, C.arrow, Math.max(5 * u, 6), Math.max(26 * u, 30), badgeR * 2.2);
            const end = s.points[s.points.length - 1];
            circle(ctx, end, badgeR * 0.55, '#fff');
            circle(ctx, end, badgeR * 0.42, C.stopDot);
          }
          drawBadge(ctx, s.points[0], badgeR * (1 + 0.18 * pulse), s.number, C.startDot);
        }
      } else if (cell.guide === 'dotted') {
        const w = Math.max(3.4 * u, 2.8), gap = Math.max(8 * u, 6);
        for (const s of cell.strokes) drawDotted(ctx, s.points, s.isDot, C.dotted, w, gap);
        if (this.mode !== 'free') {
          const badgeR = Math.max(6 * u, 8);
          for (const s of cell.strokes) {
            if (s.isDot) continue;
            const len = pathLength(s.points);
            drawArrows(ctx, s.points, C.startDot, Math.max(4.5 * u, 5), len * 2, Math.min(len * 0.45, badgeR * 3.2));
          }
          for (const s of cell.strokes.slice().reverse()) drawBadge(ctx, s.points[0], badgeR, s.number, C.startDot);
        }
      } else if (cell.guide === 'faint') {
        const w = Math.max(3.2 * u, 2.4);
        if (this.sheet.nib) {
          const nibLen = Math.max(5.5 * this.sheet.rows[0].unit, 3.5) * NIB_RATIO;
          for (const s of cell.strokes) drawNib(ctx, s.points, C.faint, nibLen);
        } else {
          for (const s of cell.strokes) drawRoad(ctx, s.points, s.isDot, C.faint, s.isDot ? w * 1.8 : w);
        }
      }
    }
  }

  function roundRect(ctx, x, y, w, hh, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + hh, r);
    ctx.arcTo(x + w, y + hh, x, y + hh, r);
    ctx.arcTo(x, y + hh, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  }

  /** "Watch": the letter writes itself stroke by stroke while the steps are read aloud. */
  class WatchBoard {
    constructor(glyph, color, onCaption) {
      this.glyph = glyph;
      this.color = color;
      this.onCaption = onCaption;
      this.el = h('div', { class: 'board' });
      this.canvas = h('canvas', { class: 'board-canvas', 'aria-label': 'Letter demonstration' });
      this.el.append(this.canvas);
      this.strokeNow = -1;
      this.frac = 0;
      this.anim = null;
      this.token = 0;
      this.ro = new ResizeObserver(() => {
        const r = this.el.getBoundingClientRect();
        if (r.width > 20 && r.height > 20) this.sheet = Sheets.demo(glyph, r.width, r.height);
      });
      this.ro.observe(this.el);
      activeBoards.add(this);
    }

    destroy() { this.token++; this.ro.disconnect(); activeBoards.delete(this); }

    async play() {
      const token = ++this.token;
      const g = this.glyph;
      const alive = () => token === this.token;
      this.strokeNow = -1; this.frac = 0; this.anim = null;
      await sleep(450);
      for (let i = 0; i < g.strokeCount; i++) {
        if (!alive()) return;
        this.strokeNow = i; this.frac = 0;
        this.onCaption((g.strokeCount > 1 ? `${i + 1}. ` : '') + g.instruction(i));
        Speech.say(g.instruction(i));
        await sleep(400);
        if (!alive()) return;
        const dur = g.dots[i] ? 300 : Math.min(700 + pathLength(g.polylines[i]) * 11, 4500);
        this.anim = { start: performance.now(), dur };
        await sleep(dur);
        this.anim = null; this.frac = 1;
        await Speech.done();
        await sleep(250);
      }
      if (!alive()) return;
      this.strokeNow = g.strokeCount;
      const msg = 'Your turn! Tap Trace to try it.';
      this.onCaption(msg);
      Speech.say(msg);
    }

    draw(now) {
      if (!this.sheet) return;
      if (this.anim) this.frac = Math.min(1, (now - this.anim.start) / this.anim.dur);
      const { ctx, w, h: H } = setupCanvas(this.canvas);
      ctx.clearRect(0, 0, w, H);
      const cell = this.sheet.cells[0];
      const u = cell.unit;
      drawPaperRow(ctx, this.sheet.rows[0]);
      const roadW = Math.max(11 * u, 12);
      for (const s of cell.strokes) drawRoad(ctx, s.points, s.isDot, C.road, roadW);
      const inkW = Math.max(6.5 * u, 4);
      const nib = !!this.sheet.nib;
      cell.strokes.forEach((s, i) => {
        if (i < this.strokeNow) drawPen(ctx, nib, s.points, this.color, inkW);
        else if (i === this.strokeNow) {
          if (s.isDot) { if (this.frac > 0.3) drawPen(ctx, nib, s.points, this.color, inkW); return; }
          const part = prefix(s.points, pathLength(s.points) * this.frac);
          drawPen(ctx, nib, part, this.color, inkW);
          const tip = part[part.length - 1];
          circle(ctx, tip, inkW * 1.25, '#fff');
          circle(ctx, tip, inkW * 0.85, C.text);
        }
      });
      if (this.strokeNow >= 0 && this.strokeNow < cell.strokes.length) {
        const s = cell.strokes[this.strokeNow];
        if (!s.isDot) drawArrows(ctx, s.points, 'rgba(85,82,92,.55)', Math.max(5 * u, 6), Math.max(26 * u, 30));
      }
      const badgeR = Math.max(8 * u, 11);
      cell.strokes.slice().reverse().forEach((s) => {
        drawBadge(ctx, s.points[0], badgeR, s.number, s.number - 1 === this.strokeNow ? C.startDot : C.dotted);
      });
    }
  }

  /* ------------------------------------------------------------------ shared pieces */

  function banner(color) {
    const txt = h('div', { class: 'txt' }, ' ');
    let text = '';
    const el = h('div', { class: 'banner', '--c': color }, txt, roundBtn('🔊', 'Say it again', () => Speech.say(text)));
    return { el, set(t) { text = t; txt.textContent = t || ' '; }, text: () => text };
  }

  function celebration(o) {
    return h('div', { class: 'overlay' },
      h('div', { class: 'pop', '--c': o.color, role: 'dialog', 'aria-label': o.title },
        h('div', { class: 'e', 'aria-hidden': 'true' }, o.emoji),
        h('div', { class: 't' }, o.title),
        starsEl(o.stars, 34),
        h('p', null, o.message),
        h('div', { class: 'row2' }, btn(o.secondary[0], o.secondary[1], { color: o.color }), btn(o.primary[0], o.primary[1], { color: o.color, filled: true }))));
  }

  function backRow(href, label) {
    return h('div', { class: 'row2' }, roundBtn('⬅️', label, () => go(href)), h('div', { style: { flex: '1' } }), voiceToggle());
  }

  const rotateHint = () => h('div', { class: 'rotate-hint' }, 'Tip: turn your device sideways for bigger lines.');

  /* ------------------------------------------------------------------ screens */

  function homeScreen() {
    const max = 52 * 3;
    const card = (title, sub, color, text, script, footer, href) => {
      const laid = layoutText(text, script);
      return h('a', { class: 'card', href, '--c': color },
        letterArt(laid.strokes.map((s) => s.points), laid.bounds, color, { height: 110, nib: script === 'calligraphy' }),
        h('div', { class: 't' }, title), h('div', { class: 's' }, sub), h('div', { class: 'f' }, footer));
    };
    const tip = (e, t) => h('div', { class: 'tip' }, h('span', { class: 'e', 'aria-hidden': 'true' }, e), h('span', null, t));
    const el = h('div', { class: 'scroll' },
      h('div', { class: 'top' },
        h('div', { class: 'grow' }, h('h1', null, 'Write Along ✏️'),
          h('p', { class: 'sub' }, 'Learn to write every letter the right way, in print, cursive and calligraphy.')),
        h('a', { class: 'kbtn solid', href: APK_URL, '--c': C.good }, h('span', { class: 'emo', 'aria-hidden': 'true' }, '📲'), h('span', { class: 'lbl' }, 'Android app')),
        voiceToggle()),
      h('div', { class: 'cards' },
        card('Print', 'Ball-and-stick letters', C.print, 'Aa', 'print', `⭐ ${Progress.total('print')} / ${max}`, '#/letters/print'),
        card('Cursive', 'Joined-up letters', C.cursive, 'Aa', 'cursive', `⭐ ${Progress.total('cursive')} / ${max}`, '#/letters/cursive'),
        card('Calligraphy', 'Fancy pen letters', C.calligraphy, 'Aa', 'calligraphy', `⭐ ${Progress.total('calligraphy')} / ${max}`, '#/letters/calligraphy'),
        card('Free Write', 'Words on big or small lines', C.free, 'hi', 'cursive', '📝 Practice paper', '#/free/print'),
        card('Assignments', 'Copy words and sentences', C.primary, 'I can', 'print', `📋 ${Assignments.list.length} to do`, '#/assignments')),
      h('h2', { class: 'section' }, 'Before you write'),
      h('div', { class: 'tips' },
        tip('🪑', 'Sit up tall with your feet flat on the floor.'),
        tip('✋', 'Hold the tablet steady with your other hand.'),
        tip('✏️', 'Use one finger, a mouse or a stylus. Hold it gently.'),
        tip('🐢', 'Go slowly. Neat beats fast!')),
      h('div', { class: 'get-app' },
        h('span', { style: { fontSize: '34px' }, 'aria-hidden': 'true' }, '🤖'),
        h('div', { class: 'grow' }, h('b', null, 'Get Write Along for Android'),
          h('small', null, 'Download the APK on your Android phone or tablet, open it, and allow installing from your browser when asked. Works offline.')),
        h('a', { class: 'kbtn solid', href: APK_URL, '--c': C.good }, h('span', { class: 'emo', 'aria-hidden': 'true' }, '⬇️'), h('span', { class: 'lbl' }, 'Download APK'))),
      h('p', { class: 'foot' },
        'This web version works in any modern browser with a finger, mouse or stylus. Stars are saved in this browser only. ',
        h('a', { href: REPO_URL }, 'Source code on GitHub'), '.'));
    return { el };
  }

  function lettersScreen(script) {
    const color = scriptColor(script);
    let upper = store.get('upper_' + script, false);
    const grid = h('div', { class: 'grid' });
    const toggle = h('div', { style: { width: '190px' } });
    const fill = () => {
      toggle.replaceChildren(choiceRow([false, true], upper, (u) => (u ? 'ABC' : 'abc'), (u) => { upper = u; store.set('upper_' + script, u); fill(); }, color));
      grid.replaceChildren(...(upper ? Glyphs.uppercase : Glyphs.lowercase).map((ch) => {
        const g = Glyphs.get(script, ch);
        return h('a', { class: 'tile', href: `#/lesson/${script}/${encodeURIComponent(ch)}`, 'aria-label': `${scriptLabel(script)} ${ch}` },
          letterArt(g.polylines, g.bounds, color, { height: 84, pad: 44, nib: script === 'calligraphy' }), starsEl(Progress.letter(script, ch), 15));
      }));
    };
    fill();
    const el = h('div', { class: 'scroll' },
      h('div', { class: 'top' },
        roundBtn('⬅️', 'Back', () => go('#/')),
        h('div', { class: 'grow' }, h('h1', { class: 'sm', style: { color } }, `${scriptLabel(script)} letters`), h('p', { class: 'sub' }, script === 'calligraphy' ? 'Pick a letter. Picture a flat pen tilted like a slanted roof.' : 'Pick a letter to learn.')),
        toggle,
        h('a', { class: 'kbtn', href: `#/free/${script}`, '--c': C.free }, h('span', { class: 'emo' }, '📝'), h('span', { class: 'lbl' }, 'Free Write')),
        voiceToggle()),
      grid);
    return { el };
  }

  function lessonScreen(script, ch) {
    const glyph = Glyphs.get(script, ch);
    if (!glyph) return null;
    const color = scriptColor(script);
    const letters = ch === ch.toUpperCase() ? Glyphs.uppercase : Glyphs.lowercase;
    const i = letters.indexOf(ch);
    const prev = letters[(i - 1 + letters.length) % letters.length];
    const next = letters[(i + 1) % letters.length];
    const lessonHref = (c) => `#/lesson/${script}/${encodeURIComponent(c)}`;

    let mode = 'watch';
    let size = store.get('size', 'big');
    let board = null;
    const root = h('div', { class: 'work' });
    const ban = banner(color);
    const MODES = [['watch', 'Watch', '👀'], ['trace', 'Trace', '✏️'], ['write', 'Write', '⭐']];

    function render() {
      if (board) board.destroy();
      board = null;
      Speech.stop();
      ban.set('');
      const stage = h('div', { class: 'stage' });
      const panel = h('aside', { class: 'panel', '--c': color },
        h('div', { class: 'sec' },
          backRow(`#/letters/${script}`, 'Back to letters'),
          h('div', { class: 'row2' },
            roundBtn('◀', 'Previous letter', () => go(lessonHref(prev)), 'sm'),
            h('div', { style: { flex: '1', minWidth: '0' } }, letterArt(glyph.polylines, glyph.bounds, color, { height: 76, pad: 34, nib: script === 'calligraphy' })),
            roundBtn('▶', 'Next letter', () => go(lessonHref(next)), 'sm')),
          h('div', { class: 'row2' }, h('div', { class: 'title', style: { flex: '1' } }, `${scriptLabel(script)} ${ch}`), starsEl(Progress.letter(script, ch), 14))),
        h('div', { class: 'sec' },
          sectionLabel('Steps'),
          MODES.map(([m, label, emoji], k) => btn(`${k + 1}. ${label}`, () => { mode = m; render(); }, { emoji, color, selected: mode === m }))),
        h('div', { class: 'sec' },
          sectionLabel('Line size'),
          choiceRow(['big', 'small'], size, (s) => (s === 'big' ? 'Big' : 'Small'), (s) => { size = s; store.set('size', s); render(); }, color, mode === 'watch'),
          sectionLabel(mode === 'write' ? "When you're done" : 'Again'),
          mode === 'watch' ? btn('Watch again', () => render(), { emoji: '🔁', color })
            : mode === 'trace' ? btn('Start over', () => render(), { emoji: '🔁', color })
              : [btn('Check', () => board && board.check(), { emoji: '✅', color: C.good, filled: true }),
                h('div', { class: 'row2' }, btn('Undo', () => board && board.undo(), { emoji: '↩️', color }), btn('Clear', () => render(), { emoji: '🧽', color }))]));

      if (mode === 'watch') {
        board = new WatchBoard(glyph, color, (t) => ban.set(t));
        stage.append(board.el);
        board.play();
      } else {
        const trace = mode === 'trace';
        board = new Board({
          mode, glyph,
          build: (w, hh) => Sheets.lesson(glyph, w, hh, size, trace),
          onMessage: (t, speak) => { ban.set(t); if (speak) Speech.say(t); },
          onFinished: (stars) => {
            if (trace) {
              Progress.record(script, ch, 1);
              stage.append(celebration({
                emoji: '🎉', title: 'You traced it!', message: `Now try writing ${ch} by yourself.`, stars: 3, color,
                primary: ['⭐ Write it', () => { mode = 'write'; render(); }], secondary: ['🔁 Trace again', () => render()],
              }));
            } else {
              Progress.record(script, ch, stars);
              if (stars === 3) {
                stage.append(celebration({
                  emoji: '🏆', title: 'Super writing!', message: 'All your letters look great.', stars: 3, color,
                  primary: ['Next letter ▶', () => go(lessonHref(next))], secondary: ['🔁 Practice more', () => render()],
                }));
              }
            }
          },
        });
        stage.append(board.el);
      }
      root.replaceChildren(panel, h('main', { class: 'main' }, ban.el, rotateHint(), stage));
    }
    render();
    return { el: root, destroy: () => board && board.destroy() };
  }

  function freeScreen(startScript) {
    let script = ['cursive', 'calligraphy'].includes(startScript) ? startScript : 'print';
    let size = store.get('freeSize', 'big');
    let word = '';
    let pen = 0;
    let board = null;
    const color = C.free;
    const root = h('div', { class: 'work' });
    const ban = banner(color);
    const WORDS = ['', 'cat', 'dog', 'sun', 'fish', 'mom', 'dad', 'love', 'I can write'];
    const input = h('input', { class: 'text', type: 'text', placeholder: 'Type a word', maxlength: '30', 'aria-label': 'Type a word to trace', autocomplete: 'off' });
    let speakTimer = 0;
    input.addEventListener('input', () => {
      const clean = input.value.replace(/[^a-zA-Z ]/g, '').slice(0, 30);
      if (clean !== input.value) input.value = clean;
      word = clean;
      renderBoard(false);
      renderWords();
      clearTimeout(speakTimer);
      speakTimer = setTimeout(() => Speech.say(ban.text()), 900);
    });
    const wordsBox = h('div', { class: 'sec' });
    const penBox = h('div', { class: 'pens' });
    const stage = h('div', { class: 'stage' });

    function renderWords() {
      const rows = [];
      for (let k = 0; k < WORDS.length; k += 2) {
        rows.push(h('div', { class: 'row2' }, WORDS.slice(k, k + 2).map((w) =>
          btn(w || 'Blank', () => { word = w; input.value = ''; renderBoard(); renderWords(); }, { color, selected: word === w && !input.value }))));
      }
      wordsBox.replaceChildren(sectionLabel('Trace a word'), ...rows, input);
    }
    function renderPens() {
      penBox.replaceChildren(...PENS.map((c, k) => h('button', {
        type: 'button', class: 'pen' + (k === pen ? ' on' : ''), style: { background: c }, 'aria-label': 'Pencil color ' + (k + 1),
        onclick: () => { pen = k; if (board) board.pen = PENS[pen]; renderPens(); },
      })));
    }
    function renderBoard(speak = true) {
      if (board) board.destroy();
      const text = word.trim()
        ? `Trace "${word.trim()}" on the dotted line. Then write it again by yourself.`
        : 'Write anything! Tall letters touch the top line. Small letters fit under the middle line.';
      ban.set(text);
      if (speak) Speech.say(text);
      board = new Board({ mode: 'free', pen: PENS[pen], build: (w, hh) => Sheets.free(word, script, w, hh, size) });
      stage.replaceChildren(board.el);
    }
    function render() {
      const panel = h('aside', { class: 'panel', '--c': color },
        h('div', { class: 'sec' },
          backRow('#/', 'Back'),
          h('div', { class: 'title' }, 'Free Write'),
          sectionLabel('Letters'),
          choiceRow(['print', 'cursive', 'calligraphy'], script, scriptLabel, (s) => { script = s; render(); }, color),
          sectionLabel('Line size'),
          choiceRow(['big', 'small'], size, (s) => (s === 'big' ? 'Big' : 'Small'), (s) => { size = s; store.set('freeSize', s); render(); }, color)),
        wordsBox,
        h('div', { class: 'sec' },
          sectionLabel('Pencil color'), penBox,
          sectionLabel('Fix it'),
          h('div', { class: 'row2' }, btn('Undo', () => board && board.undo(), { emoji: '↩️', color }), btn('Clear', () => renderBoard(), { emoji: '🧽', color }))));
      renderWords();
      renderPens();
      root.replaceChildren(panel, h('main', { class: 'main' }, ban.el, rotateHint(), stage));
      renderBoard();
    }
    render();
    return { el: root, destroy: () => { clearTimeout(speakTimer); if (board) board.destroy(); } };
  }

  function assignmentsScreen() {
    const grid = h('div', { class: 'agrid' });
    const fill = () => {
      const name = store.get('name', '');
      grid.replaceChildren(...Assignments.all(name).map((a) => {
        const color = scriptColor(a.script);
        const laid = layoutText(a.items[0], a.script);
        return h('a', { class: 'acard', href: '#/assignment/' + encodeURIComponent(a.id), '--c': color },
          h('div', { class: 'row' },
            h('span', { class: 'e', 'aria-hidden': 'true' }, a.emoji),
            h('div', { style: { flex: '1', minWidth: '0' } }, h('div', { class: 't' }, a.title),
              h('div', { class: 'k' }, `${scriptLabel(a.script)} · ${Assignments.kindLabel(a)} · ${a.items.length} tasks`)),
            starsEl(Progress.assignment(a.id), 16)),
          letterArt(laid.strokes.map((s) => s.points), laid.bounds, color, { height: 64, pad: 40, nib: a.script === 'calligraphy' }));
      }));
    };
    const input = h('input', { class: 'text', type: 'text', maxlength: '20', value: store.get('name', ''), autocomplete: 'off', 'aria-label': 'Your first name' });
    input.addEventListener('input', () => {
      const clean = input.value.replace(/[^a-zA-Z ]/g, '').slice(0, 20);
      if (clean !== input.value) input.value = clean;
      store.set('name', clean);
      fill();
    });
    fill();
    const el = h('div', { class: 'scroll' },
      h('div', { class: 'top' },
        roundBtn('⬅️', 'Back', () => go('#/')),
        h('div', { class: 'grow' }, h('h1', { class: 'sm' }, 'Writing Assignments'),
          h('p', { class: 'sub' }, 'Copy the dotted writing, then write it again on your own and tap Check.')),
        h('label', { class: 'name-field', style: { width: '230px' } }, 'Your first name', input),
        voiceToggle()),
      grid);
    return { el };
  }

  function assignmentScreen(id) {
    const a = Assignments.byId(id, store.get('name', ''));
    if (!a) return null;
    const color = scriptColor(a.script);
    const count = a.items.length;
    let itemIdx = 0;
    let size = store.get('taskSize', 'big');
    let scores = new Map();
    let checked = false;
    let board = null;
    const root = h('div', { class: 'work' });
    const ban = banner(color);

    function next() {
      if (itemIdx < count - 1) { itemIdx++; render(); return; }
      const avg = Math.max(0, Math.min(3, Math.round([...scores.values()].reduce((s, v) => s + v, 0) / Math.max(1, scores.size))));
      Progress.recordAssignment(a.id, avg);
      Speech.say(avg >= 2 ? 'You finished the assignment! Great work!' : 'You finished the assignment! Keep practicing!');
      root.querySelector('.stage').append(celebration({
        emoji: avg >= 2 ? '🏆' : '🎈', title: 'Assignment done!', stars: avg, color,
        message: avg >= 2 ? 'Wonderful writing. Pick another assignment!' : 'Good effort! Try it again for more stars.',
        primary: ['Done', () => go('#/assignments')],
        secondary: ['🔁 Do it again', () => { scores = new Map(); itemIdx = 0; render(); }],
      }));
    }

    function render() {
      if (board) board.destroy();
      checked = false;
      const item = a.items[itemIdx];
      const intro = Assignments.prompt(a, item) + ' Trace the dotted line first, then write it again on the empty line.';
      const laid = layoutText(item, a.script);
      const nextBtn = btn(itemIdx < count - 1 ? 'Next task' : 'Finish', next, { emoji: itemIdx < count - 1 ? '▶️' : '🏁', color, disabled: true });
      const hint = h('div', { class: 'hint' }, 'Tap Check to unlock the next task.');
      const stage = h('div', { class: 'stage' });
      board = new Board({
        mode: 'write', words: true, intro,
        build: (w, hh) => Sheets.assignment(item, a.script, w, hh, size),
        onMessage: (t, speak) => { ban.set(t); if (speak) Speech.say(t); },
        onFinished: (stars) => {
          scores.set(itemIdx, Math.max(scores.get(itemIdx) || 0, stars));
          checked = true;
          nextBtn.disabled = false;
          nextBtn.classList.add('solid');
          hint.remove();
          dots.replaceChildren(...dotEls());
        },
      });
      stage.append(board.el);
      const dotEls = () => a.items.map((_, k) => h('i', {
        class: k === itemIdx ? 'on' : '',
        style: { background: scores.has(k) ? C.good : k === itemIdx ? color : '' },
      }));
      const dots = h('div', { class: 'dots', 'aria-label': `Task ${itemIdx + 1} of ${count}` }, dotEls());
      const panel = h('aside', { class: 'panel', '--c': color },
        h('div', { class: 'sec' },
          backRow('#/assignments', 'Back to assignments'),
          h('div', { class: 'title' }, `${a.emoji} ${a.title}`),
          h('div', { class: 'hint', style: { color: C.text, fontWeight: '700' } }, `Task ${itemIdx + 1} of ${count}`),
          dots,
          h('div', { class: 'mini' }, letterArt(laid.strokes.map((s) => s.points), laid.bounds, color, { height: 64, pad: 24, nib: a.script === 'calligraphy' }))),
        h('div', { class: 'sec' },
          sectionLabel('Line size'),
          choiceRow(['big', 'small'], size, (s) => (s === 'big' ? 'Big' : 'Small'), (s) => { size = s; store.set('taskSize', s); render(); }, color),
          sectionLabel("When you're done"),
          btn('Check', () => board.check(), { emoji: '✅', color: C.good, filled: true }),
          h('div', { class: 'row2' }, btn('Undo', () => board.undo(), { emoji: '↩️', color }), btn('Clear', () => render(), { emoji: '🧽', color })),
          nextBtn, hint));
      root.replaceChildren(panel, h('main', { class: 'main' }, ban.el, rotateHint(), stage));
    }
    render();
    return { el: root, destroy: () => board && board.destroy() };
  }

  /* ------------------------------------------------------------------ routing */

  const app = document.getElementById('app');
  let current = null;

  function go(hash) {
    if (location.hash === hash) route(); else location.hash = hash;
  }

  function route() {
    if (current && current.destroy) current.destroy();
    Speech.stop();
    const parts = location.hash.replace(/^#\/?/, '').split('/').map(decodeURIComponent);
    const [page, a, b] = parts;
    let screen = null;
    const SCRIPTS = ['print', 'cursive', 'calligraphy'];
    if (page === 'letters' && SCRIPTS.includes(a)) screen = lettersScreen(a);
    else if (page === 'lesson' && SCRIPTS.includes(a) && b) screen = lessonScreen(a, b);
    else if (page === 'free') screen = freeScreen(a);
    else if (page === 'assignments') screen = assignmentsScreen();
    else if (page === 'assignment' && a) screen = assignmentScreen(a);
    if (!screen) screen = homeScreen();
    current = screen;
    app.replaceChildren(screen.el);
    window.scrollTo(0, 0);
  }

  window.addEventListener('hashchange', route);
  route();
})();
