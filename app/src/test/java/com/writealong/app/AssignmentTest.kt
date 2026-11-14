package com.writealong.app

import com.writealong.app.glyph.Glyphs
import com.writealong.app.glyph.Pt
import com.writealong.app.practice.Assignments
import com.writealong.app.practice.PaperSize
import com.writealong.app.practice.SheetBuilder
import com.writealong.app.practice.StrokeChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignmentTest {
    private val density = 2.5f
    private val minTol = 12f * density

    @Test
    fun everyTaskUsesOnlyKnownLetters() {
        for (a in Assignments.all("mia")) for (item in a.items) {
            for (ch in item) {
                assertTrue("${a.id}: '$ch' in \"$item\"", ch == ' ' || Glyphs.get(a.script, ch) != null)
            }
        }
    }

    @Test
    fun nameIsTidied() {
        assertEquals("Mia Rose", Assignments.formatName("  mIA   rose "))
        assertTrue(Assignments.nameAssignments("   ").isEmpty())
        assertEquals(3, Assignments.nameAssignments("sam").size)
    }

    @Test
    fun copyingTheModelEarnsThreeStars() {
        for (size in PaperSize.entries) for (a in Assignments.all("Mia")) for (item in a.items) {
            // A phone in landscape and a big tablet.
            for ((w, h) in listOf(1600f to 700f, 2400f to 1500f)) {
                val sheet = SheetBuilder.assignment(item, a.script, w, h, density, size)
                val cell = sheet.cells.last()
                // Written a little wider and further right than the dotted model.
                val c = cell.strokes.flatMap { it.points }.let { pts -> (pts.minOf { it.x } + pts.maxOf { it.x }) / 2f }
                val user = cell.strokes.map { s -> s.points.map { Pt(c + (it.x - c) * 1.12f + 20f, it.y) } }
                val score = StrokeChecker.scoreCell(user, cell, minTol, words = true)
                assertNotNull(score)
                assertEquals("${a.id} \"$item\" $size ${w}x$h: ${score!!.tip}", 3, score.stars)
            }
        }
    }

    @Test
    fun wrongWordScoresLow() {
        val a = Assignments.list.first { it.id == "p-short-a" }
        val target = SheetBuilder.assignment("cat", a.script, 1600f, 700f, density, PaperSize.BIG).cells.last()
        val other = SheetBuilder.assignment("hop", a.script, 1600f, 700f, density, PaperSize.BIG).cells.last()
        val score = StrokeChecker.scoreCell(other.strokes.map { it.points }, target, minTol, words = true)!!
        assertTrue("stars=${score.stars}", score.stars <= 1)
    }

    @Test
    fun textAlwaysFitsOnTheLine() {
        for (size in PaperSize.entries) for (a in Assignments.all("Alexandra")) for (item in a.items) {
            val sheet = SheetBuilder.assignment(item, a.script, 1600f, 700f, density, size)
            sheet.cells.forEach { cell ->
                val xs = cell.strokes.flatMap { it.points }.map { it.x }
                assertTrue("${a.id} \"$item\" overflows", xs.min() >= cell.left - 1f && xs.max() <= cell.right + 1f)
            }
        }
    }
}
