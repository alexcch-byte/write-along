package com.writealong.app.glyph

import kotlin.math.abs

/**
 * Gothic (blackletter) calligraphy, the "Old English" hand of medieval manuscripts and
 * newspaper headlines, written with a flat broad-edge pen.
 *
 * Letters are tall, narrow and upright, and every curve is broken into straight, angular
 * facets. The skeleton follows the print letters' stroke order. Stems begin with a hairline
 * serif and land with a small flick. The broad nib does the rest: strokes that cross the nib
 * come out thick and strokes that run along it hair thin (see [Nib]), which gives gothic its
 * dark, woven look.
 */
object GothicGlyphs {
    private const val LOWER_WIDTH = 0.72f
    private const val UPPER_WIDTH = 0.8f

    /** How far a facet may stray from the original curve, in letter units. */
    private const val FACET = 4.5f

    private const val LIFT = "Lift your pencil. "
    private const val SHARP = " Keep the corners sharp instead of curving."

    /** Douglas-Peucker: the fewest corner points that keep the shape within [tol]. */
    private fun simplify(pts: List<Pt>, tol: Float): List<Pt> {
        if (pts.size < 3) return pts
        val keep = BooleanArray(pts.size)
        keep[0] = true
        keep[pts.lastIndex] = true
        fun go(lo: Int, hi: Int) {
            var far = -1
            var best = tol
            for (i in lo + 1 until hi) {
                val d = distanceToSegment(pts[i], pts[lo], pts[hi])
                if (d > best) { best = d; far = i }
            }
            if (far >= 0) {
                keep[far] = true
                go(lo, far)
                go(far, hi)
            }
        }
        go(0, pts.lastIndex)
        return pts.filterIndexed { i, _ -> keep[i] }
    }

    private fun isDownStem(seg: Seg, minLen: Float) =
        seg is Line && abs(seg.a.x - seg.b.x) < 3f && seg.b.y - seg.a.y > minLen

    /** Facets for one stroke; stems stay as straight as they were. */
    private fun angular(s: StrokeDef, k: Float): StrokeDef {
        if (s.isDot) return StrokeDef(listOf(Dot(Pt(s.start.x * k, s.start.y))), s.say)
        val pts = s.sample().map { Pt(it.x * k, it.y) }
        val corners = simplify(pts, FACET)
        val curved = s.segs.any { it !is Line }
        val segs = corners.zipWithNext { a, b -> Line(a, b) as Seg }
        return StrokeDef(segs, if (curved) s.say.trimEnd() + SHARP else s.say)
    }

    /** Hairline serif on top of a stem that is pulled down, and a flick where it lands. */
    private fun decorate(g: Glyph, s: StrokeDef): StrokeDef {
        if (s.isDot) return s
        var segs: List<Seg> = s.segs
        var served = false
        var flicked = false
        val first = segs.first()
        if (isDownStem(first, 25f)) {
            val a = (first as Line).a
            segs = listOf<Seg>(Line(Pt(a.x - 7f, a.y + 7f), a)) + segs
            served = true
        }
        val last = segs.last()
        if (isDownStem(last, 20f) && abs((last as Line).b.y - 100f) < 6f) {
            val x = last.b.x
            val flick = if (g.id.isUpperCase()) 12f else 9f
            segs = segs.dropLast(1) + Line(last.a, Pt(x, 100f)) + Line(Pt(x, 100f), Pt(x + flick, 100f - flick))
            flicked = true
        }
        if (!served && !flicked) return s
        var say = s.say
        if (served) {
            val lifted = say.startsWith(LIFT)
            val body = say.removePrefix(LIFT).replaceFirstChar { it.lowercase() }
            say = (if (lifted) LIFT else "") + "Make a tiny hairline serif, then " + body
        }
        if (flicked) say = say.trimEnd() + " Finish with a small flick."
        return StrokeDef(segs, say)
    }

    private fun blackletter(g: Glyph): Glyph {
        val k = if (g.id.isUpperCase()) UPPER_WIDTH else LOWER_WIDTH
        val strokes = g.strokes.map { decorate(g, angular(it, k)) }
        return Glyph(g.id, Script.GOTHIC, strokes, 0f)
    }

    val lowercase: List<Glyph> = PrintGlyphs.lowercase.map(::blackletter)
    val uppercase: List<Glyph> = PrintGlyphs.uppercase.map(::blackletter)
}

/**
 * The flat pen nib. [ANGLE_DEG] is how far the nib edge is raised from the writing line.
 * A stroke running along the nib edge is [MIN_FRACTION] of full width; a stroke at right
 * angles to it is full width.
 */
object Nib {
    const val ANGLE_DEG = 40f
    const val MIN_FRACTION = 0.12f
}
