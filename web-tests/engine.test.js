// Checks the web engine against the same expectations as the Android unit tests.
// Run: node web-tests/engine.test.js
const assert = require('node:assert');
global.window = globalThis;
require('../docs/js/data.js');
require('../docs/js/engine.js');
const { Glyphs, Sheets, checkTrace, scoreCell, P, Assignments } = window.WA;

const W = 800, H = 360, MIN_TOL = 12;
let checks = 0;
const all = Glyphs.lowercase.concat(Glyphs.uppercase);

// Tracing the model itself is accepted, for every letter, script and size.
for (const script of ['print', 'cursive']) for (const size of ['big', 'small']) for (const ch of all) {
  const cell = Sheets.lesson(Glyphs.get(script, ch), W, H, size, true).cells[0];
  for (const s of cell.strokes) {
    const r = checkTrace(s.points, s, cell.unit, MIN_TOL);
    assert.ok(r.ok, `${script} ${size} ${ch} stroke ${s.number}: ${r.problem}`);
    checks++;
  }
}

// Going backwards is caught.
for (const ch of ['l', 'o', 'c', 'a']) {
  const cell = Sheets.lesson(Glyphs.get('print', ch), W, H, 'big', true).cells[0];
  const s = cell.strokes[0];
  const r = checkTrace(s.points.slice().reverse(), s, cell.unit, MIN_TOL);
  assert.strictEqual(r.problem, 'WRONG_WAY', ch);
  checks++;
}

// Writing the model (shifted sideways) earns 3 stars.
for (const script of ['print', 'cursive']) for (const ch of all) {
  const cells = Sheets.lesson(Glyphs.get(script, ch), W, H, 'small', false).cells;
  const cell = cells[cells.length - 1];
  const user = cell.strokes.map((s) => s.points.map((p) => P(p.x - cell.width * 0.15, p.y)));
  const sc = scoreCell(user, cell, MIN_TOL, false);
  assert.strictEqual(sc.stars, 3, `${script} ${ch}: ${sc.tip}`);
  checks++;
}

// Floating letters get the bottom-line tip.
{
  const cells = Sheets.lesson(Glyphs.get('print', 'h'), W, H, 'big', false).cells;
  const cell = cells[cells.length - 1];
  const user = cell.strokes.map((s) => s.points.map((p) => P(p.x, p.y - 30 * cell.unit)));
  const sc = scoreCell(user, cell, MIN_TOL, false);
  assert.ok(sc.problems.includes('FLOATING') && sc.stars < 3);
  checks++;
}

// Copying assignment text (a bit wider, shifted) earns 3 stars; a different word scores low.
for (const size of ['big', 'small']) for (const a of Assignments.all('Mia')) for (const item of a.items) {
  for (const [w, h] of [[640, 280], [960, 600]]) {
    const cells = Sheets.assignment(item, a.script, w, h, size).cells;
    const cell = cells[cells.length - 1];
    const xs = cell.strokes.flatMap((s) => s.points.map((p) => p.x));
    const c = (Math.min(...xs) + Math.max(...xs)) / 2;
    const user = cell.strokes.map((s) => s.points.map((p) => P(c + (p.x - c) * 1.12 + 8, p.y)));
    const sc = scoreCell(user, cell, MIN_TOL, true);
    assert.strictEqual(sc.stars, 3, `${a.id} "${item}" ${size} ${w}x${h}: ${sc.tip}`);
    checks++;
  }
}
{
  const target = Sheets.assignment('cat', 'print', 640, 280, 'big').cells[1];
  const other = Sheets.assignment('hop', 'print', 640, 280, 'big').cells[1];
  const sc = scoreCell(other.strokes.map((s) => s.points), target, MIN_TOL, true);
  assert.ok(sc.stars <= 1, `hop for cat got ${sc.stars}`);
  checks++;
}

console.log(`engine OK (${checks} checks)`);
