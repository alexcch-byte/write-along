package com.writealong.app.glyph

import kotlin.math.abs

/**
 * Italic calligraphy, written with a broad-edge pen held at a steady angle.
 *
 * The skeleton of each letter follows the same stroke order as the print letters (stems pull
 * down, bowls are drawn as ovals, arches branch from the stem), but the letters are squeezed
 * into narrow ovals and leaned forward a little. What makes it calligraphy is the pen: the
 * nib is a flat edge, so strokes that cross the nib angle come out thick and strokes that
 * run along it come out hair thin (see [Nib]).
 */
object CalligraphyGlyphs {
    /** Italic lean: about 8 degrees. */
    const val SLANT = 0.21f

    private const val LOWER_WIDTH = 0.78f
    private const val UPPER_WIDTH = 0.84f

    private fun squeeze(seg: Seg, k: Float): Seg = when (seg) {
        is Line -> Line(Pt(seg.a.x * k, seg.a.y), Pt(seg.b.x * k, seg.b.y))
        is Cubic -> Cubic(
            Pt(seg.a.x * k, seg.a.y), Pt(seg.c1.x * k, seg.c1.y),
            Pt(seg.c2.x * k, seg.c2.y), Pt(seg.b.x * k, seg.b.y),
        )
        is Arc -> seg.copy(cx = seg.cx * k, rx = seg.rx * k)
        is Dot -> Dot(Pt(seg.p.x * k, seg.p.y))
    }

    private const val LIFT = "Lift your pencil. "

    private fun isDownStem(seg: Seg, minLen: Float) =
        seg is Line && abs(seg.a.x - seg.b.x) < 3f && seg.b.y - seg.a.y > minLen

    /**
     * The fancy bits: a small hook (entry serif) at the top of every stem that is pulled down,
     * and a little flick out to the right where a stem lands on the bottom line.
     */
    private fun decorate(g: Glyph, strokes: List<StrokeDef>): List<StrokeDef> = strokes.map { s ->
        if (s.isDot) return@map s
        var segs: List<Seg> = s.segs
        var hooked = false
        var flicked = false
        val first = segs.first()
        if (isDownStem(first, 25f)) {
            val a = (first as Line).a
            val hook = Cubic(Pt(a.x - 10f, a.y + 10f), Pt(a.x - 9f, a.y + 3f), Pt(a.x - 4f, a.y - 0.5f), a)
            segs = listOf<Seg>(hook) + segs
            hooked = true
        }
        val last = segs.last()
        if (isDownStem(last, 20f) && abs((last as Line).b.y - 100f) < 6f) {
            val longFlick = if (g.id.isUpperCase()) 15f else 11f
            val x = last.b.x
            val shortened = Line(last.a, Pt(x, 90f))
            val flick = Cubic(Pt(x, 90f), Pt(x, 99.5f), Pt(x + 3f, 101f), Pt(x + longFlick, 94.5f))
            segs = segs.dropLast(1) + shortened + flick
            flicked = true
        }
        if (!hooked && !flicked) return@map s
        var say = s.say
        if (hooked) {
            val lifted = say.startsWith(LIFT)
            val body = say.removePrefix(LIFT).replaceFirstChar { it.lowercase() }
            say = (if (lifted) LIFT else "") + "Make a little hook, then " + body
        }
        if (flicked) say = say.trimEnd() + " Finish with a little flick."
        StrokeDef(segs, say)
    }

    private fun italic(g: Glyph): Glyph {
        val k = if (g.id.isUpperCase()) UPPER_WIDTH else LOWER_WIDTH
        val narrow = g.strokes.map { s -> StrokeDef(s.segs.map { squeeze(it, k) }, s.say) }
        return Glyph(g.id, Script.CALLIGRAPHY, decorate(g, narrow), SLANT)
    }

    val lowercase: List<Glyph> = PrintGlyphs.lowercase.map(::italic)
    val uppercase: List<Glyph> = PrintGlyphs.uppercase.map(::italic)
}

/**
 * The flat pen nib. [ANGLE_DEG] is how far the nib edge is raised from the writing line
 * (45 degrees is the classic italic angle). A stroke running along the nib edge is
 * [MIN_FRACTION] of full width; a stroke at right angles to it is full width.
 */
object Nib {
    const val ANGLE_DEG = 45f
    const val MIN_FRACTION = 0.12f
}
