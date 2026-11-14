package com.writealong.app.glyph

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Letter coordinate system ("units"), shared by every glyph:
 *
 *   y =   0  top line
 *   y =  50  middle (dashed) line
 *   y = 100  bottom line (baseline)
 *   y = 150  tail line (descenders)
 *
 * x grows to the right. Glyphs are defined upright; cursive glyphs get a forward slant
 * (a shear around the baseline) applied when they are sampled.
 */

data class Pt(val x: Float, val y: Float) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
    operator fun times(k: Float) = Pt(x * k, y * k)
    fun dist(o: Pt): Float = hypot(x - o.x, y - o.y)
    fun length(): Float = hypot(x, y)
    fun normalized(): Pt {
        val l = length()
        return if (l < 1e-4f) Pt(1f, 0f) else Pt(x / l, y / l)
    }
}

data class Box(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
    val width get() = maxX - minX
    val height get() = maxY - minY
    val centerX get() = (minX + maxX) / 2f
    val centerY get() = (minY + maxY) / 2f

    companion object {
        fun of(points: Iterable<Pt>): Box {
            var x0 = Float.MAX_VALUE
            var y0 = Float.MAX_VALUE
            var x1 = -Float.MAX_VALUE
            var y1 = -Float.MAX_VALUE
            for (p in points) {
                x0 = min(x0, p.x); y0 = min(y0, p.y); x1 = max(x1, p.x); y1 = max(y1, p.y)
            }
            return if (x0 > x1) Box(0f, 0f, 0f, 0f) else Box(x0, y0, x1, y1)
        }
    }
}

/** One piece of a stroke. Every segment knows its own start and end point. */
sealed class Seg {
    abstract fun at(t: Float): Pt
    abstract fun translate(dx: Float): Seg
    val start: Pt get() = at(0f)
    val end: Pt get() = at(1f)

    /** Direction of travel at the very end of the segment. */
    open fun endTangent(): Pt = (at(1f) - at(0.97f)).normalized()

    fun approxLength(): Float {
        var len = 0f
        var p = at(0f)
        for (i in 1..24) {
            val q = at(i / 24f)
            len += p.dist(q)
            p = q
        }
        return len
    }
}

data class Line(val a: Pt, val b: Pt) : Seg() {
    override fun at(t: Float) = Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    override fun translate(dx: Float) = Line(Pt(a.x + dx, a.y), Pt(b.x + dx, b.y))
}

data class Cubic(val a: Pt, val c1: Pt, val c2: Pt, val b: Pt) : Seg() {
    override fun at(t: Float): Pt {
        val u = 1f - t
        val w0 = u * u * u
        val w1 = 3f * u * u * t
        val w2 = 3f * u * t * t
        val w3 = t * t * t
        return Pt(
            w0 * a.x + w1 * c1.x + w2 * c2.x + w3 * b.x,
            w0 * a.y + w1 * c1.y + w2 * c2.y + w3 * b.y,
        )
    }

    override fun translate(dx: Float) =
        Cubic(Pt(a.x + dx, a.y), Pt(c1.x + dx, c1.y), Pt(c2.x + dx, c2.y), Pt(b.x + dx, b.y))

    override fun endTangent(): Pt = if (b.dist(c2) > 0.5f) (b - c2).normalized() else super.endTangent()
}

/**
 * Elliptical arc. Angles in degrees, screen convention (y points down):
 * 0 = right, 90 = bottom, 180 = left, 270 / -90 = top.
 * A positive sweep turns clockwise on screen ("circle forward"),
 * a negative sweep turns counter-clockwise ("circle back").
 */
data class Arc(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val startDeg: Float, val sweepDeg: Float) : Seg() {
    override fun at(t: Float): Pt {
        val ang = (startDeg + sweepDeg * t) * PI / 180.0
        return Pt(cx + rx * cos(ang).toFloat(), cy + ry * sin(ang).toFloat())
    }

    override fun translate(dx: Float) = copy(cx = cx + dx)
}

/** A dot (for i, j). */
data class Dot(val p: Pt) : Seg() {
    override fun at(t: Float) = p
    override fun translate(dx: Float) = Dot(Pt(p.x + dx, p.y))
}

/** One pencil-down-to-pencil-up movement, plus the words we say to teach it. */
class StrokeDef(val segs: List<Seg>, val say: String) {
    val isDot: Boolean get() = segs.size == 1 && segs[0] is Dot
    val start: Pt get() = segs.first().start

    fun translate(dx: Float) = StrokeDef(segs.map { it.translate(dx) }, say)

    /** Dense polyline (about one point per [step] units). Not slanted. */
    fun sample(step: Float = 1.5f): List<Pt> = sampleSegments(segs, step)
}

fun sampleSegments(segs: List<Seg>, step: Float = 1.5f): List<Pt> {
    val out = ArrayList<Pt>()
    for (s in segs) {
        if (s is Dot) {
            out += s.p
            continue
        }
        val n = max(2, ceil(s.approxLength() / step).toInt())
        val first = if (out.isNotEmpty() && out.last().dist(s.start) < 0.01f) 1 else 0
        for (i in first..n) out += s.at(i.toFloat() / n)
    }
    return out
}

enum class Script(val label: String) { PRINT("Print"), CURSIVE("Cursive"), CALLIGRAPHY("Calligraphy") }

/**
 * A letter, as a teacher would demonstrate it: an ordered list of strokes.
 *
 * Cursive lowercase letters begin with a lead-in stroke from the bottom line ([leadIn] = true:
 * the first segment of the first stroke). When letters are joined into words the lead-in is
 * replaced by a connector from the previous letter's [exit].
 */
class Glyph(
    val id: Char,
    val script: Script,
    val strokes: List<StrokeDef>,
    val slant: Float = 0f,
    val leadIn: Boolean = false,
    val exitStroke: Int? = null,
) {
    /** Where the joining stroke ends, upright coordinates. Null = this letter does not join. */
    val exit: Pt? get() = exitStroke?.let { strokes[it].segs.last().end }
    val exitTangent: Pt? get() = exitStroke?.let { strokes[it].segs.last().endTangent() }

    fun slantPoint(p: Pt): Pt = if (slant == 0f) p else Pt(p.x + (100f - p.y) * slant, p.y)

    /** Upright sampled strokes. */
    val rawPolylines: List<List<Pt>> by lazy { strokes.map { it.sample() } }

    /** Final sampled strokes, slant applied. This is what gets drawn and checked. */
    val polylines: List<List<Pt>> by lazy { rawPolylines.map { line -> line.map(::slantPoint) } }

    val bounds: Box by lazy { Box.of(polylines.flatten()) }
    val rawBounds: Box by lazy { Box.of(rawPolylines.flatten()) }

    /** Full spoken/printed instruction for stroke [i]. */
    fun instruction(i: Int): String {
        val s = strokes[i]
        if (s.isDot) return s.say
        val lift = "Lift your pencil. "
        return if (s.say.startsWith(lift)) {
            "$lift${describeStart(s.start)} ${s.say.removePrefix(lift)}"
        } else {
            "${describeStart(s.start)} ${s.say}"
        }
    }

    val strokeCount get() = strokes.size
}

/** Plain-language description of where on the writing lines a stroke begins. */
fun describeStart(p: Pt): String = when {
    p.y <= 6f -> "Start at the top line."
    p.y < 20f -> "Start just below the top line."
    p.y <= 42f -> "Start between the top line and the middle line."
    p.y <= 57f -> "Start at the middle line."
    p.y <= 70f -> "Start just below the middle line."
    p.y <= 94f -> "Start between the middle line and the bottom line."
    p.y <= 106f -> "Start at the bottom line."
    else -> "Start below the bottom line."
}

/* ---------------------------------------------------------------- builder DSL */

class StrokeBuilder {
    private val segs = mutableListOf<Seg>()
    private var cur: Pt? = null

    private fun p(x: Number, y: Number) = Pt(x.toFloat(), y.toFloat())
    private fun from(): Pt = cur ?: error("Stroke must start with m(), arc() or dot()")

    /** Move (pencil down here). */
    fun m(x: Number, y: Number) {
        cur = p(x, y)
    }

    /** Straight line. */
    fun l(x: Number, y: Number) {
        val to = p(x, y)
        segs += Line(from(), to)
        cur = to
    }

    /** Cubic curve with two control points. */
    fun c(x1: Number, y1: Number, x2: Number, y2: Number, x: Number, y: Number) {
        val to = p(x, y)
        segs += Cubic(from(), p(x1, y1), p(x2, y2), to)
        cur = to
    }

    /** Elliptical arc; see [Arc] for the angle convention. */
    fun arc(cx: Number, cy: Number, rx: Number, ry: Number, startDeg: Number, sweepDeg: Number) {
        val a = Arc(cx.toFloat(), cy.toFloat(), rx.toFloat(), ry.toFloat(), startDeg.toFloat(), sweepDeg.toFloat())
        val c0 = cur
        if (c0 != null && c0.dist(a.start) > 0.75f) segs += Line(c0, a.start)
        segs += a
        cur = a.end
    }

    fun circle(cx: Number, cy: Number, r: Number, startDeg: Number, sweepDeg: Number) =
        arc(cx, cy, r, r, startDeg, sweepDeg)

    fun dot(x: Number, y: Number) {
        val at = p(x, y)
        segs += Dot(at)
        cur = at
    }

    fun build(say: String): StrokeDef {
        require(segs.isNotEmpty()) { "Empty stroke" }
        return StrokeDef(segs.toList(), say)
    }
}

class GlyphBuilder {
    internal val strokes = mutableListOf<StrokeDef>()

    fun stroke(say: String, block: StrokeBuilder.() -> Unit) {
        strokes += StrokeBuilder().apply(block).build(say)
    }

    fun dotStroke(x: Number, y: Number, say: String = "Lift your pencil and add a dot.") {
        stroke(say) { dot(x, y) }
    }
}

fun glyph(
    id: Char,
    script: Script,
    slant: Float = 0f,
    leadIn: Boolean = false,
    exitStroke: Int? = null,
    block: GlyphBuilder.() -> Unit,
): Glyph = Glyph(id, script, GlyphBuilder().apply(block).strokes.toList(), slant, leadIn, exitStroke)

/* ---------------------------------------------------------------- polyline helpers */

fun pathLength(pts: List<Pt>): Float {
    var len = 0f
    for (i in 1 until pts.size) len += pts[i - 1].dist(pts[i])
    return len
}

/** Evenly spaced resampling to [n] points. */
fun resample(pts: List<Pt>, n: Int): List<Pt> {
    if (pts.isEmpty()) return emptyList()
    if (pts.size == 1) return List(n) { pts[0] }
    val total = pathLength(pts)
    if (total < 1e-3f) return List(n) { pts[0] }
    val interval = total / (n - 1)
    val out = ArrayList<Pt>(n)
    out += pts[0]
    var acc = 0f
    var prev = pts[0]
    var i = 1
    while (i < pts.size && out.size < n) {
        val cur = pts[i]
        val d = prev.dist(cur)
        if (acc + d >= interval && d > 0f) {
            val t = (interval - acc) / d
            val q = Pt(prev.x + (cur.x - prev.x) * t, prev.y + (cur.y - prev.y) * t)
            out += q
            prev = q
            acc = 0f
        } else {
            acc += d
            prev = cur
            i++
        }
    }
    while (out.size < n) out += pts.last()
    return out
}

/** Point at a given distance along the polyline, plus the direction of travel there. */
fun pointAlong(pts: List<Pt>, distance: Float): Pair<Pt, Pt> {
    if (pts.size < 2) return (pts.firstOrNull() ?: Pt(0f, 0f)) to Pt(1f, 0f)
    var acc = 0f
    for (i in 1 until pts.size) {
        val a = pts[i - 1]
        val b = pts[i]
        val d = a.dist(b)
        if (acc + d >= distance && d > 0f) {
            val t = (distance - acc) / d
            return Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t) to (b - a).normalized()
        }
        acc += d
    }
    val a = pts[pts.size - 2]
    val b = pts.last()
    return b to (b - a).normalized()
}

/** The first part of a polyline, up to [distance] along it. */
fun prefix(pts: List<Pt>, distance: Float): List<Pt> {
    if (pts.size < 2) return pts
    val out = ArrayList<Pt>()
    out += pts[0]
    var acc = 0f
    for (i in 1 until pts.size) {
        val a = pts[i - 1]
        val b = pts[i]
        val d = a.dist(b)
        if (acc + d >= distance) {
            val t = if (d > 0f) (distance - acc) / d else 0f
            out += Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
            return out
        }
        out += b
        acc += d
    }
    return out
}

/** Distance from [p] to the nearest point of a dense polyline. */
fun distanceToPolyline(p: Pt, pts: List<Pt>): Float {
    if (pts.isEmpty()) return Float.MAX_VALUE
    if (pts.size == 1) return p.dist(pts[0])
    var best = Float.MAX_VALUE
    for (i in 1 until pts.size) {
        val d = distanceToSegment(p, pts[i - 1], pts[i])
        if (d < best) best = d
    }
    return best
}

fun distanceToSegment(p: Pt, a: Pt, b: Pt): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val len2 = abx * abx + aby * aby
    if (len2 < 1e-6f) return p.dist(a)
    val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / len2).coerceIn(0f, 1f)
    return hypot(p.x - (a.x + abx * t), p.y - (a.y + aby * t))
}
