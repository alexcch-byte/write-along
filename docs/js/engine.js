/*
 * Write Along — web engine. A direct port of the Android app's glyph/ and practice/ code:
 * letter shapes, writing-line layouts, cursive joining, and the tracing / writing checks.
 * Letter shapes come from data.js, which is generated from the Android source.
 *
 * Letter units: y=0 top line, 50 middle line, 100 bottom line, 150 tail line.
 */
(function (global) {
  'use strict';

  const DATA = global.WA_DATA;
  const SLANT = DATA.slant;

  /* ------------------------------------------------------------------ geometry */

  const P = (x, y) => ({ x, y });
  const dist = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);
  const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));

  function norm(x, y) {
    const l = Math.hypot(x, y);
    return l < 1e-4 ? P(1, 0) : P(x / l, y / l);
  }

  function box(points) {
    let x0 = Infinity, y0 = Infinity, x1 = -Infinity, y1 = -Infinity;
    for (const p of points) {
      if (p.x < x0) x0 = p.x;
      if (p.y < y0) y0 = p.y;
      if (p.x > x1) x1 = p.x;
      if (p.y > y1) y1 = p.y;
    }
    if (x0 > x1) { x0 = y0 = x1 = y1 = 0; }
    return { minX: x0, minY: y0, maxX: x1, maxY: y1, w: x1 - x0, h: y1 - y0, cx: (x0 + x1) / 2, cy: (y0 + y1) / 2 };
  }

  function pathLength(pts) {
    let l = 0;
    for (let i = 1; i < pts.length; i++) l += dist(pts[i - 1], pts[i]);
    return l;
  }

  function resample(pts, n) {
    if (!pts.length) return [];
    if (pts.length === 1) return Array.from({ length: n }, () => pts[0]);
    const total = pathLength(pts);
    if (total < 1e-3) return Array.from({ length: n }, () => pts[0]);
    const interval = total / (n - 1);
    const out = [pts[0]];
    let acc = 0, prev = pts[0], i = 1;
    while (i < pts.length && out.length < n) {
      const cur = pts[i];
      const d = dist(prev, cur);
      if (acc + d >= interval && d > 0) {
        const t = (interval - acc) / d;
        const q = P(prev.x + (cur.x - prev.x) * t, prev.y + (cur.y - prev.y) * t);
        out.push(q);
        prev = q;
        acc = 0;
      } else {
        acc += d;
        prev = cur;
        i++;
      }
    }
    while (out.length < n) out.push(pts[pts.length - 1]);
    return out;
  }

  /** Point at a distance along a polyline and the direction of travel there. */
  function pointAlong(pts, distance) {
    if (pts.length < 2) return { p: pts[0] || P(0, 0), dir: P(1, 0) };
    let acc = 0;
    for (let i = 1; i < pts.length; i++) {
      const a = pts[i - 1], b = pts[i];
      const d = dist(a, b);
      if (acc + d >= distance && d > 0) {
        const t = (distance - acc) / d;
        return { p: P(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t), dir: norm(b.x - a.x, b.y - a.y) };
      }
      acc += d;
    }
    const a = pts[pts.length - 2], b = pts[pts.length - 1];
    return { p: b, dir: norm(b.x - a.x, b.y - a.y) };
  }

  function prefix(pts, distance) {
    if (pts.length < 2) return pts.slice();
    const out = [pts[0]];
    let acc = 0;
    for (let i = 1; i < pts.length; i++) {
      const a = pts[i - 1], b = pts[i];
      const d = dist(a, b);
      if (acc + d >= distance) {
        const t = d > 0 ? (distance - acc) / d : 0;
        out.push(P(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t));
        return out;
      }
      out.push(b);
      acc += d;
    }
    return out;
  }

  function distToSeg(p, a, b) {
    const abx = b.x - a.x, aby = b.y - a.y;
    const len2 = abx * abx + aby * aby;
    if (len2 < 1e-6) return dist(p, a);
    const t = clamp(((p.x - a.x) * abx + (p.y - a.y) * aby) / len2, 0, 1);
    return Math.hypot(p.x - (a.x + abx * t), p.y - (a.y + aby * t));
  }

  function distToPolyline(p, pts) {
    if (!pts.length) return Infinity;
    if (pts.length === 1) return dist(p, pts[0]);
    let best = Infinity;
    for (let i = 1; i < pts.length; i++) {
      const d = distToSeg(p, pts[i - 1], pts[i]);
      if (d < best) best = d;
    }
    return best;
  }

  function centroid(pts) {
    if (!pts.length) return P(0, 0);
    let x = 0, y = 0;
    for (const p of pts) { x += p.x; y += p.y; }
    return P(x / pts.length, y / pts.length);
  }

  function cubicPoints(a, c1, c2, b, step) {
    const at = (t) => {
      const u = 1 - t;
      const w0 = u * u * u, w1 = 3 * u * u * t, w2 = 3 * u * t * t, w3 = t * t * t;
      return P(w0 * a.x + w1 * c1.x + w2 * c2.x + w3 * b.x, w0 * a.y + w1 * c1.y + w2 * c2.y + w3 * b.y);
    };
    let len = 0, prev = a;
    for (let i = 1; i <= 24; i++) { const q = at(i / 24); len += dist(prev, q); prev = q; }
    const n = Math.max(2, Math.ceil(len / step));
    const out = [];
    for (let i = 0; i <= n; i++) out.push(at(i / n));
    return out;
  }

  /* ------------------------------------------------------------------ glyphs */

  function toPts(flat) {
    const out = [];
    for (let i = 0; i < flat.length; i += 2) out.push(P(flat[i], flat[i + 1]));
    return out;
  }

  class Glyph {
    constructor(script, id, raw) {
      this.script = script;
      this.id = id;
      this.slant = script === 'cursive' ? SLANT : script === 'calligraphy' ? DATA.calSlant : 0;
      this.raw = raw.s.map((s) => toPts(s.p));
      this.dots = raw.s.map((s) => !!s.d);
      this.says = raw.s.map((s) => s.t);
      this.lead = raw.lead || null;
      this.exit = raw.exit ? P(raw.exit[0], raw.exit[1]) : null;
      this.tan = raw.tan ? P(raw.tan[0], raw.tan[1]) : null;
      this.polylines = this.raw.map((l) => l.map((p) => this.slantPt(p)));
      this.bounds = box(this.polylines.flat());
      this.rawBounds = box(this.raw.flat());
    }
    slantPt(p) { return this.slant ? P(p.x + (100 - p.y) * this.slant, p.y) : p; }
    get strokeCount() { return this.raw.length; }
    instruction(i) { return this.says[i]; }
  }

  const cache = {};
  const Glyphs = {
    lowercase: 'abcdefghijklmnopqrstuvwxyz'.split(''),
    uppercase: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split(''),
    get(script, ch) {
      const key = script + ch;
      if (key in cache) return cache[key];
      const raw = DATA.glyphs[script] && DATA.glyphs[script][ch];
      cache[key] = raw ? new Glyph(script, ch, raw) : null;
      return cache[key];
    },
  };

  /* ------------------------------------------------------------------ text layout */

  /** Lays a word or sentence out in letter units. Cursive letters are joined. */
  function layoutText(text, script) {
    const strokes = [];
    if (script !== 'cursive') {
      // Print and calligraphy letters sit side by side, each with its own lift of the pen.
      let cursor = 0;
      const gap = script === 'calligraphy' ? 12 : 14;
      [...text].forEach((ch, gi) => {
        const g = Glyphs.get(script, ch);
        if (!g) { cursor += 38; return; }
        const dx = cursor - g.bounds.minX;
        g.polylines.forEach((line, si) => {
          strokes.push({ points: line.map((p) => P(p.x + dx, p.y)), isDot: g.dots[si], glyph: gi, stroke: si });
        });
        cursor += g.bounds.w + gap;
      });
    } else {
      let cursor = 0, prevExit = null, prevTan = null;
      [...text].forEach((ch, gi) => {
        const g = Glyphs.get('cursive', ch);
        if (!g) { cursor += 38; prevExit = null; prevTan = null; return; }
        const joining = prevExit && g.lead;
        const dx = joining ? prevExit.x - 2 : cursor - g.rawBounds.minX;
        const lines = g.raw.map((l) => l.map((p) => P(p.x + dx, p.y)));
        if (joining) {
          const first = lines[0];
          const end = first[g.lead.i];
          const c2 = P(g.lead.c2[0] + dx, g.lead.c2[1]);
          const dir = prevTan || norm(1, -1);
          const reach = dist(prevExit, end) * 0.4;
          const c1 = P(prevExit.x + dir.x * reach, prevExit.y + dir.y * reach);
          lines[0] = cubicPoints(prevExit, c1, c2, end, 1.5).concat(first.slice(g.lead.i + 1));
        }
        let maxX = -Infinity;
        lines.forEach((l, si) => {
          for (const p of l) if (p.x > maxX) maxX = p.x;
          strokes.push({ points: l, isDot: g.dots[si], glyph: gi, stroke: si });
        });
        cursor = Math.max(cursor, maxX + 6);
        prevExit = g.exit ? P(g.exit.x + dx, g.exit.y) : null;
        prevTan = g.tan;
      });
      for (const s of strokes) s.points = s.points.map((p) => P(p.x + (100 - p.y) * SLANT, p.y));
    }
    return { text, strokes, bounds: box(strokes.flatMap((s) => s.points)) };
  }

  /* ------------------------------------------------------------------ sheets */

  const Paper = { HEADROOM: 30, BODY: 150, FOOTROOM: 14 };
  Paper.ROW = Paper.HEADROOM + Paper.BODY + Paper.FOOTROOM;

  function makeRow(top, unit, left, right) {
    const topLine = top + Paper.HEADROOM * unit;
    return {
      top, unit, left, right, topLine,
      midLine: topLine + 50 * unit,
      baseLine: topLine + 100 * unit,
      tailLine: topLine + 150 * unit,
      bottom: top + Paper.ROW * unit,
    };
  }

  function makeCell(index, row, rowIndex, left, right, guide, strokes) {
    return { index, row, rowIndex, left, right, guide, strokes, unit: row.unit, centerX: (left + right) / 2, width: right - left };
  }

  function placeGlyph(glyph, row, centerX) {
    const u = row.unit;
    const ox = centerX - glyph.bounds.cx * u;
    return glyph.polylines.map((line, i) => ({
      points: line.map((p) => P(ox + p.x * u, row.topLine + p.y * u)),
      isDot: glyph.dots[i],
      number: i + 1,
      group: 0,
    }));
  }

  const Sheets = {
    /** One letter: tracing roads (trace=true) or dotted, faint and empty spots. */
    lesson(glyph, w, h, size, trace) {
      const margin = 14;
      const usableW = w - 2 * margin;
      const cellUnits = Math.max(glyph.bounds.w + 46, 86);
      let rowsCount, unit, maxCells;
      if (size === 'big') {
        rowsCount = 1;
        const minCells = trace ? 1 : 3;
        maxCells = trace ? 2 : 6;
        unit = Math.min(h / Paper.ROW, usableW / (cellUnits * minCells), 3.4);
      } else {
        const smallRows = h > 560 ? 4 : 3;
        rowsCount = trace ? 1 : smallRows;
        maxCells = trace ? 5 : 9;
        unit = Math.min(h / (smallRows * Paper.ROW), usableW / (cellUnits * 3), 1.2);
      }
      const perRow = clamp(Math.floor(usableW / (cellUnits * unit)), 1, maxCells);
      const top0 = (h - rowsCount * Paper.ROW * unit) / 2;
      const rows = [];
      for (let r = 0; r < rowsCount; r++) rows.push(makeRow(top0 + r * Paper.ROW * unit, unit, margin, w - margin));
      const cellW = usableW / perRow;
      const cells = [];
      rows.forEach((row, r) => {
        for (let c = 0; c < perRow; c++) {
          const guide = trace ? 'trace' : c === 0 ? 'dotted' : c === 1 ? 'faint' : 'none';
          const left = margin + c * cellW;
          cells.push(makeCell(cells.length, row, r, left, left + cellW, guide, placeGlyph(glyph, row, left + cellW / 2)));
        }
      });
      return { rows, cells, nib: glyph.script === 'calligraphy' };
    },

    /** One big letter for the Watch demonstration. */
    demo(glyph, w, h) {
      const margin = 14;
      const unit = Math.min(h / Paper.ROW, (w - 2 * margin) / (glyph.bounds.w + 80), 3.6);
      const row = makeRow((h - Paper.ROW * unit) / 2, unit, margin, w - margin);
      return { rows: [row], cells: [makeCell(0, row, 0, margin, w - margin, 'faint', placeGlyph(glyph, row, w / 2))], nib: glyph.script === 'calligraphy' };
    },

    /** Free-writing paper, optionally with a word to trace (dotted rows, then faint rows). */
    free(text, script, w, h, size) {
      const margin = 14;
      const rowsCount = size === 'big' ? 2 : h > 560 ? 5 : 4;
      const unit = h / (rowsCount * Paper.ROW);
      const top0 = (h - rowsCount * Paper.ROW * unit) / 2;
      const rows = [];
      for (let r = 0; r < rowsCount; r++) rows.push(makeRow(top0 + r * Paper.ROW * unit, unit, margin, w - margin));
      const cells = [];
      const trimmed = text.trim();
      if (!trimmed) return { rows, cells, nib: script === 'calligraphy' };
      const pad = 16;
      const maxUnits = (w - 2 * margin) / unit - 2 * pad;
      const lines = wrap(trimmed, script, maxUnits);
      const plan = lines.map((l) => [l, 'dotted']).concat(lines.map((l) => [l, 'faint']));
      plan.slice(0, rowsCount).forEach(([lineText, guide], r) => {
        const row = rows[r];
        const laid = layoutText(lineText, script);
        const ox = margin + pad * unit - laid.bounds.minX * unit;
        const ghosts = laid.strokes.map((s) => ({
          points: s.points.map((p) => P(ox + p.x * unit, row.topLine + p.y * unit)),
          isDot: s.isDot, number: s.stroke + 1, group: s.glyph,
        }));
        cells.push(makeCell(cells.length, row, r, row.left, row.right, guide, ghosts));
      });
      return { rows, cells, nib: script === 'calligraphy' };
    },

    /** One assignment task: dotted first row, faint second (small lines), then empty rows. */
    assignment(text, script, w, h, size) {
      const margin = 14;
      const rowsCount = size === 'big' ? 2 : h > 560 ? 4 : 3;
      const laid = layoutText(text, script);
      const pad = 20;
      const unit = Math.min(h / (rowsCount * Paper.ROW), (w - 2 * margin) / (laid.bounds.w + 2 * pad), 3.4);
      const top0 = (h - rowsCount * Paper.ROW * unit) / 2;
      const rows = [];
      for (let r = 0; r < rowsCount; r++) rows.push(makeRow(top0 + r * Paper.ROW * unit, unit, margin, w - margin));
      const cells = rows.map((row, r) => {
        const guide = r === 0 ? 'dotted' : r === 1 && rowsCount > 2 ? 'faint' : 'none';
        const ox = margin + pad * unit - laid.bounds.minX * unit;
        const ghosts = laid.strokes.map((s) => ({
          points: s.points.map((p) => P(ox + p.x * unit, row.topLine + p.y * unit)),
          isDot: s.isDot, number: s.stroke + 1, group: s.glyph,
        }));
        return makeCell(r, row, r, row.left, row.right, guide, ghosts);
      });
      return { rows, cells, nib: script === 'calligraphy' };
    },
  };

  function wrap(text, script, maxUnits) {
    const words = text.split(/\s+/).filter(Boolean);
    const lines = [];
    let current = '';
    for (const w of words) {
      const candidate = current ? current + ' ' + w : w;
      if (current && layoutText(candidate, script).bounds.w > maxUnits) {
        lines.push(current);
        current = w;
      } else {
        current = candidate;
      }
    }
    if (current) lines.push(current);
    return lines;
  }

  /* ------------------------------------------------------------------ checking */

  const TRACE_MSG = {
    WRONG_START: 'Start at the green dot.',
    WRONG_WAY: 'Oops, other way! Follow the arrows.',
    OFF_PATH: 'Stay on the gray path.',
    TOO_SHORT: 'Keep going to the end without lifting your pencil.',
    DOT: 'Just tap a little dot on the spot.',
  };

  const WRITE_MSG = {
    LETTER_HEIGHTS: 'Look closely at each letter: which ones are tall, which are small, and which have tails?',
    WRONG_WAY: 'Check which way your pencil goes. Follow the arrows on the dotted letter.',
    ORDER: 'Remember the order. Follow the numbers on the dotted letter.',
    TOO_SHORT: 'Make it taller. Reach up to the right line.',
    TOO_TALL: 'Not quite so tall. Watch where the dotted letters stop.',
    FLOATING: 'Sit your writing down on the bottom line.',
    SINKING: 'Stop on the bottom line.',
    NEEDS_TAIL: 'The tail goes down below the bottom line.',
    TOO_MANY: 'Try to make it with fewer pencil lifts.',
  };

  const PRAISES = ['Excellent!', 'Super writing!', 'Perfect!', 'Wow, great job!', 'Beautiful!'];
  const praise = (seed) => PRAISES[((seed % PRAISES.length) + PRAISES.length) % PRAISES.length];

  const N = 48;

  function checkTrace(user, model, unit, minTol) {
    const tol = Math.max(15 * unit, minTol);
    if (!user.length) return { ok: false, problem: 'OFF_PATH' };
    if (model.isDot) {
      const target = model.points[0];
      if (dist(centroid(user), target) > tol * 1.5) return { ok: false, problem: 'OFF_PATH' };
      if (pathLength(user) > 30 * unit + minTol) return { ok: false, problem: 'DOT' };
      return { ok: true };
    }
    const m = model.points;
    const userLen = pathLength(user);
    const modelLen = pathLength(m);
    if (user.length < 2 || userLen < tol * 0.6) {
      return { ok: false, problem: dist(user[0], m[0]) < tol * 1.6 ? 'TOO_SHORT' : 'WRONG_START' };
    }
    const ru = resample(user, N);
    const rm = resample(m, N);
    const coverage = rm.filter((p) => distToPolyline(p, user) <= tol).length / rm.length;
    const precision = ru.filter((p) => distToPolyline(p, m) <= tol).length / ru.length;
    let fwd = 0, rev = 0;
    for (let i = 0; i < N; i++) { fwd += dist(ru[i], rm[i]); rev += dist(ru[i], rm[N - 1 - i]); }
    fwd /= N; rev /= N;
    if (precision < 0.68) return { ok: false, problem: 'OFF_PATH' };
    if (coverage < 0.72) return { ok: false, problem: userLen < modelLen * 0.9 ? 'TOO_SHORT' : 'OFF_PATH' };
    if (rev < fwd * 0.8 && fwd > tol * 0.7) return { ok: false, problem: 'WRONG_WAY' };
    if (dist(user[0], m[0]) > tol * 1.7) return { ok: false, problem: 'WRONG_START' };
    return { ok: true };
  }

  /** Points with the direction the line runs there (hasDir=false for dots). */
  function oriented(strokes) {
    const out = [];
    for (const s of strokes) {
      if (s.length < 2) { for (const p of s) out.push({ x: p.x, y: p.y, tx: 0, ty: 0, d: false }); continue; }
      for (let i = 0; i < s.length; i++) {
        const a = s[Math.max(0, i - 2)], b = s[Math.min(s.length - 1, i + 2)];
        const dx = b.x - a.x, dy = b.y - a.y;
        const l = Math.hypot(dx, dy);
        out.push(l < 1e-3 ? { x: s[i].x, y: s[i].y, tx: 0, ty: 0, d: false } : { x: s[i].x, y: s[i].y, tx: dx / l, ty: dy / l, d: true });
      }
    }
    return out;
  }

  function matchedFraction(from, to, tol, shift) {
    if (!from.length) return 0;
    const tol2 = tol * tol;
    const sh = shift || 0;
    let hit = 0;
    for (const p of from) {
      for (const q of to) {
        const dx = p.x + sh - q.x, dy = p.y - q.y;
        if (dx * dx + dy * dy > tol2) continue;
        if (!p.d || !q.d || Math.abs(p.tx * q.tx + p.ty * q.ty) >= 0.6) { hit++; break; }
      }
    }
    return hit / from.length;
  }

  /** Score what the child wrote in one practice spot. Null if empty. */
  function scoreCell(user, cell, minTol, words) {
    if (!user.length || !cell.strokes.length) return null;
    const u = cell.unit;
    const tol = Math.max(14 * u, minTol);
    const shapeTol = Math.max(9 * u, minTol * 0.6);
    const mb = box(cell.strokes.flatMap((s) => s.points));
    const ub = box(user.flat());

    let model;
    if (words && mb.w > 1) {
      const k = clamp(ub.w / mb.w, 0.7, 1.4);
      model = cell.strokes.map((s) => s.points.map((p) => P(ub.cx + (p.x - mb.cx) * k, p.y)));
    } else {
      const dx = clamp(ub.cx - mb.cx, -cell.width / 2, cell.width / 2);
      model = cell.strokes.map((s) => s.points.map((p) => P(p.x + dx, p.y)));
    }
    const denseUser = user.map((s) => (s.length < 2 ? s : resample(s, Math.max(2, Math.floor(pathLength(s) / (1.5 * u))))));
    const modelPts = oriented(model);
    const userPts = oriented(denseUser);

    const partOf = words ? cell.strokes.map((s) => s.group) : cell.strokes.map((_, i) => i);
    const groups = new Map();
    model.forEach((line, i) => {
      const k = partOf[i];
      if (!groups.has(k)) groups.set(k, []);
      groups.get(k).push(line);
    });
    const parts = [...groups.values()].map((lines) => oriented(lines));

    let partCov, precision, zoneMisses = 0;
    if (words) {
      const wordTol = Math.max(7 * u, minTol * 0.5);
      const slide = 20 * u, step = 2.5 * u;
      const fitted = parts.map((part) => {
        let x0 = Infinity, x1 = -Infinity;
        for (const p of part) { if (p.x < x0) x0 = p.x; if (p.x > x1) x1 = p.x; }
        x0 -= slide + wordTol; x1 += slide + wordTol;
        const near = userPts.filter((p) => p.x >= x0 && p.x <= x1);
        let best = -1, bestDx = 0;
        for (let dx = -slide; dx <= slide + 0.01; dx += step) {
          const c = matchedFraction(part, near, wordTol, dx);
          if (c > best + 1e-4 || (Math.abs(c - best) <= 1e-4 && Math.abs(dx) < Math.abs(bestDx))) { best = c; bestDx = dx; }
        }
        return { cov: best, pts: part.map((p) => ({ x: p.x + bestDx, y: p.y, tx: p.tx, ty: p.ty, d: p.d })) };
      });
      partCov = fitted.map((f) => f.cov);
      precision = matchedFraction(userPts, fitted.flatMap((f) => f.pts), wordTol);
      const top = cell.row.topLine;
      const inkTop = fitted.map(() => Infinity);
      const inkBot = fitted.map(() => -Infinity);
      const cap2 = (22 * u) * (22 * u);
      for (const p of userPts) {
        let bestG = -1, bestD = cap2;
        fitted.forEach((f, gi) => {
          for (const q of f.pts) {
            const dx = p.x - q.x, dy = p.y - q.y;
            const d2 = dx * dx + dy * dy;
            if (d2 < bestD) { bestD = d2; bestG = gi; }
          }
        });
        if (bestG >= 0) {
          if (p.y < inkTop[bestG]) inkTop[bestG] = p.y;
          if (p.y > inkBot[bestG]) inkBot[bestG] = p.y;
        }
      }
      zoneMisses = fitted.filter((f, gi) => {
        if (inkTop[gi] === Infinity) return true;
        let mt = Infinity, mbot = -Infinity;
        for (const q of f.pts) { if (q.y < mt) mt = q.y; if (q.y > mbot) mbot = q.y; }
        return Math.abs((inkTop[gi] - top) / u - (mt - top) / u) > 20 || Math.abs((inkBot[gi] - top) / u - (mbot - top) / u) > 20;
      }).length;
    } else {
      partCov = parts.map((part) => matchedFraction(part, userPts, shapeTol));
      precision = matchedFraction(userPts, modelPts, shapeTol);
    }
    let weighted = 0;
    parts.forEach((part, i) => { weighted += partCov[i] * part.length; });
    const meanCov = weighted / Math.max(1, modelPts.length);
    const minCov = partCov.length ? Math.min(...partCov) : 0;
    const coverage = words ? 0.6 * meanCov + 0.4 * minCov : 0.75 * meanCov + 0.25 * minCov;
    const zoneMissRate = parts.length ? zoneMisses / parts.length : 0;
    const shape = (0.5 * coverage + 0.5 * precision) * (1 - 0.6 * zoneMissRate);

    const problems = [];
    if (zoneMisses > 0) problems.push('LETTER_HEIGHTS');
    if (!words && user.length === model.length && model.length > 0) {
      let wrongWay = false, wrongOrder = false;
      for (let i = 0; i < model.length; i++) {
        if (cell.strokes[i].isDot || user[i].length < 2) continue;
        const ru = resample(user[i], N), rm = resample(model[i], N);
        let fwd = 0, rev = 0;
        for (let k = 0; k < N; k++) { fwd += dist(ru[k], rm[k]); rev += dist(ru[k], rm[N - 1 - k]); }
        fwd /= N; rev /= N;
        if (rev < fwd * 0.75 && fwd > tol * 0.8) wrongWay = true;
      }
      if (model.length > 1) {
        const mc = model.map(centroid);
        user.forEach((s, i) => {
          const c = centroid(s);
          let nearest = 0;
          mc.forEach((m, j) => { if (dist(m, c) < dist(mc[nearest], c)) nearest = j; });
          if (nearest !== i && dist(mc[nearest], mc[i]) > tol) wrongOrder = true;
        });
      }
      if (wrongWay) problems.push('WRONG_WAY');
      if (wrongOrder) problems.push('ORDER');
    } else if (!words && user.length > model.length + 2) {
      problems.push('TOO_MANY');
    }

    const top = cell.row.topLine;
    const units = (y) => (y - top) / u;
    const mTop = units(mb.minY), mBot = units(mb.maxY), uTop = units(ub.minY), uBot = units(ub.maxY);
    if (uTop - mTop > 16) problems.push('TOO_SHORT');
    else if (mTop - uTop > 18) problems.push('TOO_TALL');
    if (mBot > 120 && mBot - uBot > 18) problems.push('NEEDS_TAIL');
    else if (mBot - uBot > 14) problems.push('FLOATING');
    else if (mBot < 110 && uBot - mBot > 16) problems.push('SINKING');

    const cut = words ? [0.74, 0.56, 0.36] : [0.8, 0.62, 0.4];
    let stars = shape >= cut[0] ? 3 : shape >= cut[1] ? 2 : shape >= cut[2] ? 1 : 0;
    if (stars === 3 && problems.length) stars = 2;
    const what = words ? 'writing' : 'letter';
    let tip;
    if (stars === 0) tip = `Let's try that one again. Trace the dotted ${what} first.`;
    else if (problems.length) tip = WRITE_MSG[problems[0]];
    else if (stars === 3) tip = praise(Math.abs(Math.trunc(ub.cx)));
    else if (stars === 2) tip = `Good! Try to match the dotted ${what} even more closely.`;
    else tip = `Nice try! Look at the dotted ${what} and try again.`;
    return { stars, tip, problems, shape };
  }

  /* ------------------------------------------------------------------ assignments */

  function formatName(raw) {
    return raw.trim().split(/\s+/).filter(Boolean).map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join(' ');
  }

  const Assignments = {
    list: DATA.assignments,
    forName(name) {
      const n = formatName(name || '');
      if (!n) return [];
      return [
        { id: 'name-print', title: 'My name', emoji: '😊', script: 'print', kind: 'name', items: [n, n] },
        { id: 'name-cursive', title: 'My name in cursive', emoji: '🖋️', script: 'cursive', kind: 'name', items: [n, n] },
        { id: 'name-calligraphy', title: 'My name in calligraphy', emoji: '🎨', script: 'calligraphy', kind: 'name', items: [n, n] },
      ];
    },
    all(name) { return this.forName(name).concat(this.list); },
    byId(id, name) { return this.all(name).find((a) => a.id === id) || null; },
    prompt(a, item) {
      switch (a.kind) {
        case 'letters': return `Write these letters: ${item.split(' ').join(', ')}.`;
        case 'words': return `Write the word "${item}".`;
        case 'sentences': return `Write the sentence: ${item}. Start with a capital letter and leave a finger space between words.`;
        default: return `Write your name: ${item}. Names start with a capital letter.`;
      }
    },
    kindLabel(a) { return { letters: 'Letters', words: 'Words', sentences: 'Sentences', name: 'Name' }[a.kind]; },
  };

  global.WA = {
    P, dist, box, pathLength, resample, pointAlong, prefix, centroid,
    Glyphs, layoutText, Paper, Sheets, checkTrace, scoreCell, praise, TRACE_MSG, Assignments, formatName,
  };
})(typeof window !== 'undefined' ? window : globalThis);
