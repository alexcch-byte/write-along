package com.writealong.app

import com.writealong.app.glyph.Glyph
import com.writealong.app.glyph.Glyphs
import com.writealong.app.glyph.Pt
import com.writealong.app.glyph.Script
import com.writealong.app.glyph.TextLayout
import com.writealong.app.glyph.pointAlong
import com.writealong.app.glyph.pathLength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Sanity checks for the letter data, plus an HTML contact sheet of every letter
 * (app/build/reports/glyphs.html) for eyeballing shapes and stroke order.
 */
class GlyphPreviewTest {

    @Test
    fun everyLetterExistsInBothScripts() {
        for (script in Script.entries) {
            for (ch in Glyphs.lowercase + Glyphs.uppercase) {
                val g = Glyphs.get(script, ch)
                assertNotNull("$script $ch missing", g)
                g!!
                assertTrue("$script $ch has no strokes", g.strokeCount > 0)
                g.polylines.forEachIndexed { i, line ->
                    assertTrue("$script $ch stroke $i empty", line.isNotEmpty())
                }
                for (i in 0 until g.strokeCount) assertTrue(g.instruction(i).isNotBlank())
            }
        }
    }

    @Test
    fun cursiveLowercaseStartsOnBottomLineAndJoins() {
        for (ch in Glyphs.lowercase) {
            val g = Glyphs.require(Script.CURSIVE, ch)
            assertTrue("$ch lead-in", g.leadIn)
            assertEquals("$ch should start on the bottom line", 100f, g.strokes[0].start.y, 0.5f)
            assertNotNull("$ch exit", g.exit)
        }
    }

    @Test
    fun writeContactSheet() {
        val sb = StringBuilder()
        sb.append("<!doctype html><html><head><meta charset='utf-8'><title>Glyphs</title>")
        sb.append("<style>body{font-family:sans-serif;background:#fff8ec;margin:16px} .g{display:inline-block;margin:4px;background:#fff;border:1px solid #ddd;border-radius:8px} h2{margin:18px 0 6px}</style></head><body>")
        for (script in Script.entries) {
            sb.append("<h2>${script.label} lowercase</h2>")
            Glyphs.lowercase.forEach { sb.append(glyphSvg(Glyphs.require(script, it))) }
            sb.append("<h2>${script.label} uppercase</h2>")
            Glyphs.uppercase.forEach { sb.append(glyphSvg(Glyphs.require(script, it))) }
        }
        for (script in Script.entries) {
            sb.append("<h2>${script.label} words</h2>")
            listOf("cat", "hello", "bow", "jump", "queen", "fizzy", "Amy", "I can write", "Bob", "Hi mom").forEach {
                sb.append(wordSvg(it, script))
            }
        }
        sb.append("</body></html>")
        val out = File("build/reports/glyphs.html")
        out.parentFile.mkdirs()
        out.writeText(sb.toString())
        println("Contact sheet: ${out.absolutePath}")
    }

    private val colors = listOf("#e4572e", "#2e86de", "#17a589", "#8e44ad", "#f39c12")

    private fun f(v: Float) = String.format(Locale.US, "%.1f", v)

    private fun lines(minX: Float, maxX: Float) = buildString {
        append("<line x1='${f(minX)}' y1='0' x2='${f(maxX)}' y2='0' stroke='#5b6cf0' stroke-width='1.2'/>")
        append("<line x1='${f(minX)}' y1='50' x2='${f(maxX)}' y2='50' stroke='#5b6cf0' stroke-width='1' stroke-dasharray='5 4'/>")
        append("<line x1='${f(minX)}' y1='100' x2='${f(maxX)}' y2='100' stroke='#e8505b' stroke-width='1.4'/>")
        append("<line x1='${f(minX)}' y1='150' x2='${f(maxX)}' y2='150' stroke='#bbb' stroke-width='0.8' stroke-dasharray='3 4'/>")
    }

    private fun poly(pts: List<Pt>, color: String, width: Float) =
        "<polyline fill='none' stroke='$color' stroke-width='$width' stroke-linecap='round' stroke-linejoin='round' points='" +
            pts.joinToString(" ") { "${f(it.x)},${f(it.y)}" } + "'/>"

    private fun glyphSvg(g: Glyph): String {
        val b = g.bounds
        val minX = b.minX - 14
        val maxX = b.maxX + 14
        val w = maxX - minX
        val sb = StringBuilder()
        sb.append("<svg class='g' width='${f(w * 1.1f)}' height='${f(195 * 1.1f)}' viewBox='${f(minX)} -25 ${f(w)} 195'>")
        sb.append(lines(minX, maxX))
        sb.append("<text x='${f(minX + 3)}' y='-14' font-size='11' fill='#666'>${g.id}</text>")
        g.polylines.forEachIndexed { i, pts ->
            val col = colors[i % colors.size]
            if (g.strokes[i].isDot) {
                sb.append("<circle cx='${f(pts[0].x)}' cy='${f(pts[0].y)}' r='4' fill='$col'/>")
            } else {
                sb.append(poly(pts, col, 3.2f))
                val len = pathLength(pts)
                var d = 14f
                while (d < len - 6) {
                    val (p, dir) = pointAlong(pts, d)
                    val ang = Math.toDegrees(kotlin.math.atan2(dir.y, dir.x).toDouble())
                    sb.append("<path d='M -4 -3 L 1 0 L -4 3' fill='none' stroke='#222' stroke-width='1.2' transform='translate(${f(p.x)} ${f(p.y)}) rotate(${f(ang.toFloat())})'/>")
                    d += 30f
                }
            }
            val s = pts[0]
            sb.append("<circle cx='${f(s.x)}' cy='${f(s.y)}' r='5.5' fill='#2bb673'/>")
            sb.append("<text x='${f(s.x)}' y='${f(s.y + 3.5f)}' font-size='8' fill='#fff' text-anchor='middle'>${i + 1}</text>")
        }
        sb.append("</svg>")
        return sb.toString()
    }

    private fun wordSvg(text: String, script: Script): String {
        val line = TextLayout.layout(text, script)
        val b = line.bounds
        val minX = b.minX - 14
        val maxX = b.maxX + 14
        val w = maxX - minX
        val sb = StringBuilder()
        sb.append("<svg class='g' width='${f(w)}' height='195' viewBox='${f(minX)} -25 ${f(w)} 195'>")
        sb.append(lines(minX, maxX))
        line.strokes.forEach { s ->
            if (s.isDot) sb.append("<circle cx='${f(s.points[0].x)}' cy='${f(s.points[0].y)}' r='3.5' fill='#2b2d42'/>")
            else sb.append(poly(s.points, "#2b2d42", 3f))
        }
        sb.append("</svg>")
        return sb.toString()
    }
}
