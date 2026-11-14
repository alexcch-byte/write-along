package com.writealong.app.glyph

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
    const val SLANT = 0.15f

    private const val LOWER_WIDTH = 0.82f
    private const val UPPER_WIDTH = 0.86f

    private fun squeeze(seg: Seg, k: Float): Seg = when (seg) {
        is Line -> Line(Pt(seg.a.x * k, seg.a.y), Pt(seg.b.x * k, seg.b.y))
        is Cubic -> Cubic(
            Pt(seg.a.x * k, seg.a.y), Pt(seg.c1.x * k, seg.c1.y),
            Pt(seg.c2.x * k, seg.c2.y), Pt(seg.b.x * k, seg.b.y),
        )
        is Arc -> seg.copy(cx = seg.cx * k, rx = seg.rx * k)
        is Dot -> Dot(Pt(seg.p.x * k, seg.p.y))
    }

    private fun italic(g: Glyph): Glyph {
        val k = if (g.id.isUpperCase()) UPPER_WIDTH else LOWER_WIDTH
        val strokes = g.strokes.map { s -> StrokeDef(s.segs.map { squeeze(it, k) }, s.say) }
        return Glyph(g.id, Script.CALLIGRAPHY, strokes, SLANT)
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
    const val MIN_FRACTION = 0.16f
}
