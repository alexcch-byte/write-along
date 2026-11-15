package com.writealong.app.glyph

/**
 * Copperplate (English roundhand): the elegant, joined script of old letters and wedding
 * invitations, written with a pointed pen.
 *
 * The skeleton of each letter is the joined cursive letter, but copperplate leans much further
 * (about 30 degrees from upright) and its letters are narrow, graceful ovals. What makes it
 * copperplate is the pen: a pointed nib opens a little when you press, so the line is hair thin
 * on the way up and across, and swells into a thick "shade" on every downstroke (see
 * `drawPointedStroke` in the UI package).
 */
object CopperplateGlyphs {
    /** Lean of the letters: about 31 degrees from upright. */
    const val SLANT = 0.6f

    private const val LOWER_WIDTH = 0.92f
    private const val UPPER_WIDTH = 0.95f

    private fun squeeze(seg: Seg, k: Float): Seg = when (seg) {
        is Line -> Line(Pt(seg.a.x * k, seg.a.y), Pt(seg.b.x * k, seg.b.y))
        is Cubic -> Cubic(
            Pt(seg.a.x * k, seg.a.y), Pt(seg.c1.x * k, seg.c1.y),
            Pt(seg.c2.x * k, seg.c2.y), Pt(seg.b.x * k, seg.b.y),
        )
        is Arc -> seg.copy(cx = seg.cx * k, rx = seg.rx * k)
        is Dot -> Dot(Pt(seg.p.x * k, seg.p.y))
    }

    /** Same strokes, joins and stroke order as cursive: narrower ovals, a much steeper lean. */
    private fun roundhand(g: Glyph): Glyph {
        val k = if (g.id.isUpperCase()) UPPER_WIDTH else LOWER_WIDTH
        val narrow = g.strokes.map { s -> StrokeDef(s.segs.map { squeeze(it, k) }, s.say) }
        return Glyph(g.id, Script.COPPERPLATE, narrow, SLANT, g.leadIn, g.exitStroke)
    }

    val lowercase: List<Glyph> = CursiveGlyphs.lowercase.map(::roundhand)
    val uppercase: List<Glyph> = CursiveGlyphs.uppercase.map(::roundhand)
}
