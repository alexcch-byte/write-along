package com.writealong.app.glyph

import kotlin.math.max

object Glyphs {
    private val print: Map<Char, Glyph> = (PrintGlyphs.lowercase + PrintGlyphs.uppercase).associateBy { it.id }
    private val cursive: Map<Char, Glyph> = (CursiveGlyphs.lowercase + CursiveGlyphs.uppercase).associateBy { it.id }

    val lowercase: List<Char> = ('a'..'z').toList()
    val uppercase: List<Char> = ('A'..'Z').toList()

    fun get(script: Script, ch: Char): Glyph? = when (script) {
        Script.PRINT -> print[ch]
        Script.CURSIVE -> cursive[ch]
    }

    fun require(script: Script, ch: Char): Glyph = get(script, ch) ?: error("No $script glyph for '$ch'")

    fun all(script: Script): List<Glyph> = when (script) {
        Script.PRINT -> PrintGlyphs.lowercase + PrintGlyphs.uppercase
        Script.CURSIVE -> CursiveGlyphs.lowercase + CursiveGlyphs.uppercase
    }
}

/** A stroke of a laid-out word, in letter units (slant already applied). */
class TextStroke(val points: List<Pt>, val isDot: Boolean, val glyphIndex: Int, val strokeIndex: Int)

class TextLine(val text: String, val strokes: List<TextStroke>) {
    val bounds: Box = Box.of(strokes.flatMap { it.points })
}

/**
 * Lays a word or short sentence out on the writing lines. Print letters sit side by side;
 * cursive letters are joined: each lead-in is replaced by a connector that starts where
 * the previous letter's exit stroke ended.
 */
object TextLayout {
    private const val PRINT_GAP = 14f
    private const val CURSIVE_GAP = 6f
    private const val SPACE = 38f

    fun layout(text: String, script: Script): TextLine =
        if (script == Script.PRINT) layoutPrint(text) else layoutCursive(text)

    private fun layoutPrint(text: String): TextLine {
        val out = ArrayList<TextStroke>()
        var cursor = 0f
        text.forEachIndexed { gi, ch ->
            val g = Glyphs.get(Script.PRINT, ch)
            if (g == null) {
                cursor += SPACE
                return@forEachIndexed
            }
            val dx = cursor - g.bounds.minX
            g.polylines.forEachIndexed { si, line ->
                out += TextStroke(line.map { Pt(it.x + dx, it.y) }, g.strokes[si].isDot, gi, si)
            }
            cursor += g.bounds.width + PRINT_GAP
        }
        return TextLine(text, out)
    }

    private fun layoutCursive(text: String): TextLine {
        // Work upright, then slant everything at the end (slant and horizontal moves commute).
        val raw = ArrayList<Triple<List<Pt>, Boolean, Pair<Int, Int>>>()
        var cursor = 0f
        var prevExit: Pt? = null
        var prevTangent: Pt? = null
        text.forEachIndexed { gi, ch ->
            val g = Glyphs.get(Script.CURSIVE, ch)
            if (g == null) {
                cursor += SPACE
                prevExit = null
                prevTangent = null
                return@forEachIndexed
            }
            val pe = prevExit
            val joining = pe != null && g.leadIn
            val dx = if (joining) pe!!.x - 2f else cursor - g.rawBounds.minX
            var strokes = g.strokes.map { it.translate(dx) }
            if (joining) {
                val first = strokes[0]
                val lead = first.segs[0]
                val end = lead.end
                val c2 = (lead as? Cubic)?.c2 ?: end
                val dir = prevTangent ?: Pt(1f, -1f).normalized()
                val reach = pe!!.dist(end) * 0.4f
                val connector = Cubic(pe, pe + dir * reach, c2, end)
                strokes = listOf(StrokeDef(listOf(connector) + first.segs.drop(1), first.say)) + strokes.drop(1)
            }
            strokes.forEachIndexed { si, s ->
                raw += Triple(s.sample(), s.isDot, gi to si)
            }
            val maxX = strokes.maxOf { s -> s.sample(4f).maxOf { it.x } }
            cursor = max(cursor, maxX + CURSIVE_GAP)
            prevExit = g.exit?.let { Pt(it.x + dx, it.y) }
            prevTangent = g.exitTangent
        }
        val slant = CursiveGlyphs.SLANT
        val out = raw.map { (pts, dot, idx) ->
            TextStroke(pts.map { Pt(it.x + (100f - it.y) * slant, it.y) }, dot, idx.first, idx.second)
        }
        return TextLine(text, out)
    }
}
