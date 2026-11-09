package com.writealong.app

import com.writealong.app.glyph.Glyphs
import com.writealong.app.glyph.Pt
import com.writealong.app.glyph.Script
import com.writealong.app.practice.PaperSize
import com.writealong.app.practice.SheetBuilder
import com.writealong.app.practice.StrokeChecker
import com.writealong.app.practice.TraceProblem
import com.writealong.app.practice.WriteProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeCheckerTest {
    private val density = 2.5f
    private val w = 2000f
    private val h = 900f
    private val minTol = 12f * density

    @Test
    fun tracingTheModelIsAcceptedForEveryLetter() {
        for (script in Script.entries) for (size in PaperSize.entries) for (ch in Glyphs.lowercase + Glyphs.uppercase) {
            val g = Glyphs.require(script, ch)
            val sheet = SheetBuilder.lesson(g, w, h, density, size, trace = true)
            val cell = sheet.cells.first()
            cell.strokes.forEach { s ->
                val r = StrokeChecker.checkTrace(s.points, s, cell.unit, minTol)
                assertTrue("$script $size $ch stroke ${s.number}: ${r.problem}", r.ok)
            }
        }
    }

    @Test
    fun backwardsStrokeIsCaught() {
        for (ch in listOf('l', 'o', 'c', 'a')) {
            val g = Glyphs.require(Script.PRINT, ch)
            val cell = SheetBuilder.lesson(g, w, h, density, PaperSize.BIG, trace = true).cells.first()
            val s = cell.strokes.first()
            val r = StrokeChecker.checkTrace(s.points.reversed(), s, cell.unit, minTol)
            assertFalse("$ch reversed accepted", r.ok)
            assertEquals("$ch", TraceProblem.WRONG_WAY, r.problem)
        }
    }

    @Test
    fun scribbleIsRejected() {
        val g = Glyphs.require(Script.PRINT, 'b')
        val cell = SheetBuilder.lesson(g, w, h, density, PaperSize.BIG, trace = true).cells.first()
        val s = cell.strokes.first()
        val start = s.points.first()
        val scribble = List(30) { i -> Pt(start.x + 300f + (i % 2) * 120f, start.y + i * 4f) }
        assertFalse(StrokeChecker.checkTrace(scribble, s, cell.unit, minTol).ok)
    }

    @Test
    fun writingTheModelEarnsThreeStars() {
        for (script in Script.entries) for (ch in Glyphs.lowercase + Glyphs.uppercase) {
            val g = Glyphs.require(script, ch)
            val sheet = SheetBuilder.lesson(g, w, h, density, PaperSize.SMALL, trace = false)
            val cell = sheet.cells.last()
            // Write it a bit to the left of centre: horizontal position shouldn't matter.
            val user = cell.strokes.map { s -> s.points.map { Pt(it.x - cell.width * 0.15f, it.y) } }
            val score = StrokeChecker.scoreCell(user, cell, minTol)
            assertNotNull(score)
            assertEquals("$script $ch: ${score!!.tip}", 3, score.stars)
        }
    }

    @Test
    fun floatingLetterGetsLineTip() {
        val g = Glyphs.require(Script.PRINT, 'h')
        val cell = SheetBuilder.lesson(g, w, h, density, PaperSize.BIG, trace = false).cells.last()
        val lift = 30f * cell.unit
        val user = cell.strokes.map { s -> s.points.map { Pt(it.x, it.y - lift) } }
        val score = StrokeChecker.scoreCell(user, cell, minTol)!!
        assertTrue(score.problems.toString(), WriteProblem.FLOATING in score.problems)
        assertTrue(score.stars < 3)
    }

    @Test
    fun emptySpotIsNotScored() {
        val g = Glyphs.require(Script.PRINT, 'a')
        val cell = SheetBuilder.lesson(g, w, h, density, PaperSize.BIG, trace = false).cells.last()
        assertEquals(null, StrokeChecker.scoreCell(emptyList(), cell, minTol))
    }
}
