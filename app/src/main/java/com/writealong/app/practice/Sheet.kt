package com.writealong.app.practice

import com.writealong.app.glyph.Glyph
import com.writealong.app.glyph.Pt
import com.writealong.app.glyph.Script
import com.writealong.app.glyph.TextLayout
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class PaperSize(val label: String) { BIG("Big"), SMALL("Small") }

/** How much help a practice spot shows. */
enum class Guide {
    /** Thick gray "road" with numbered start dots and arrows: trace it. */
    TRACE,

    /** Dotted letter with numbered start dots: trace over the dots. */
    DOTTED,

    /** Very light outline: write on top of it. */
    FAINT,

    /** Empty: write it on your own. */
    NONE,
}

/** Vertical space of one set of writing lines, in letter units. */
object Paper {
    const val HEADROOM = 30f
    const val BODY = 150f
    const val FOOTROOM = 14f
    const val ROW = HEADROOM + BODY + FOOTROOM
}

/** One set of writing lines, in pixels. */
data class PaperRow(val top: Float, val unit: Float, val left: Float, val right: Float) {
    val topLine get() = top + Paper.HEADROOM * unit
    val midLine get() = topLine + 50f * unit
    val baseLine get() = topLine + 100f * unit
    val tailLine get() = topLine + 150f * unit
    val bottom get() = top + Paper.ROW * unit
}

/** A model stroke in pixels. [group] is the letter it belongs to within a word. */
class GhostStroke(val points: List<Pt>, val isDot: Boolean, val number: Int, val group: Int = 0)

/** One spot on the sheet where the child writes a letter (or a line of words). */
class Cell(
    val index: Int,
    val row: PaperRow,
    val rowIndex: Int,
    val left: Float,
    val right: Float,
    val guide: Guide,
    val strokes: List<GhostStroke>,
) {
    val unit get() = row.unit
    val centerX get() = (left + right) / 2f
    val width get() = right - left
}

class Sheet(val rows: List<PaperRow>, val cells: List<Cell>) {
    val unit: Float get() = rows.firstOrNull()?.unit ?: 1f
}

object SheetBuilder {

    /**
     * Practice sheet for a single letter.
     * trace = true: every spot is a tracing road (checked stroke by stroke).
     * trace = false: dotted, then faint, then empty spots (checked when the child taps Check).
     */
    fun lesson(glyph: Glyph, widthPx: Float, heightPx: Float, density: Float, size: PaperSize, trace: Boolean): Sheet {
        val margin = 14f * density
        val usableW = widthPx - 2 * margin
        val b = glyph.bounds
        val cellUnits = max(b.width + 46f, 86f)

        val rowsCount: Int
        val unit: Float
        val maxCells: Int
        val minCells: Int
        when (size) {
            PaperSize.BIG -> {
                rowsCount = 1
                minCells = if (trace) 1 else 3
                maxCells = if (trace) 2 else 6
                unit = min(min(heightPx / Paper.ROW, usableW / (cellUnits * minCells)), 3.4f * density)
            }
            PaperSize.SMALL -> {
                val smallRows = if (heightPx / density > 560f) 4 else 3
                rowsCount = if (trace) 1 else smallRows
                minCells = 3
                maxCells = if (trace) 5 else 9
                unit = min(min(heightPx / (smallRows * Paper.ROW), usableW / (cellUnits * minCells)), 1.2f * density)
            }
        }
        val perRow = floor(usableW / (cellUnits * unit)).toInt().coerceIn(1, maxCells)
        val rowsH = rowsCount * Paper.ROW * unit
        val top0 = (heightPx - rowsH) / 2f

        val rows = List(rowsCount) { r -> PaperRow(top0 + r * Paper.ROW * unit, unit, margin, widthPx - margin) }
        val cells = ArrayList<Cell>()
        val cellW = usableW / perRow
        rows.forEachIndexed { r, row ->
            for (c in 0 until perRow) {
                val guide = when {
                    trace -> Guide.TRACE
                    c == 0 -> Guide.DOTTED
                    c == 1 -> Guide.FAINT
                    else -> Guide.NONE
                }
                val left = margin + c * cellW
                cells += Cell(cells.size, row, r, left, left + cellW, guide, placeGlyph(glyph, row, left + cellW / 2f))
            }
        }
        return Sheet(rows, cells)
    }

    /** A single big spot for the "Watch" demonstration. */
    fun demo(glyph: Glyph, widthPx: Float, heightPx: Float, density: Float): Sheet {
        val margin = 14f * density
        val b = glyph.bounds
        val unit = min(min(heightPx / Paper.ROW, (widthPx - 2 * margin) / (b.width + 80f)), 3.6f * density)
        val row = PaperRow((heightPx - Paper.ROW * unit) / 2f, unit, margin, widthPx - margin)
        val cell = Cell(0, row, 0, margin, widthPx - margin, Guide.FAINT, placeGlyph(glyph, row, widthPx / 2f))
        return Sheet(listOf(row), listOf(cell))
    }

    fun placeGlyph(glyph: Glyph, row: PaperRow, centerX: Float): List<GhostStroke> {
        val u = row.unit
        val ox = centerX - glyph.bounds.centerX * u
        val oy = row.topLine
        return glyph.polylines.mapIndexed { i, line ->
            GhostStroke(line.map { Pt(ox + it.x * u, oy + it.y * u) }, glyph.strokes[i].isDot, i + 1)
        }
    }

    /**
     * Free-writing paper. If [text] is given it is shown dotted on the first row(s) and faint on
     * the next row(s); long text wraps by word.
     */
    fun free(text: String, script: Script, widthPx: Float, heightPx: Float, density: Float, size: PaperSize): Sheet {
        val margin = 14f * density
        val rowsCount = when (size) {
            PaperSize.BIG -> 2
            PaperSize.SMALL -> if (heightPx / density > 560f) 5 else 4
        }
        val unit = heightPx / (rowsCount * Paper.ROW)
        val top0 = (heightPx - rowsCount * Paper.ROW * unit) / 2f
        val rows = List(rowsCount) { r -> PaperRow(top0 + r * Paper.ROW * unit, unit, margin, widthPx - margin) }

        val cells = ArrayList<Cell>()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Sheet(rows, cells)

        val pad = 16f
        val maxUnits = (widthPx - 2 * margin) / unit - 2 * pad
        val lines = wrap(trimmed, script, maxUnits)
        val plan = ArrayList<Pair<String, Guide>>()
        lines.forEach { plan += it to Guide.DOTTED }
        lines.forEach { plan += it to Guide.FAINT }

        plan.take(rowsCount).forEachIndexed { r, (lineText, guide) ->
            val row = rows[r]
            val laid = TextLayout.layout(lineText, script)
            val ox = margin + pad * unit - laid.bounds.minX * unit
            val ghosts = laid.strokes.map { s ->
                GhostStroke(s.points.map { Pt(ox + it.x * unit, row.topLine + it.y * unit) }, s.isDot, s.strokeIndex + 1, s.glyphIndex)
            }
            cells += Cell(cells.size, row, r, row.left, row.right, guide, ghosts)
        }
        return Sheet(rows, cells)
    }

    /**
     * One assignment task: the text dotted on the first row, faint on the second (small lines
     * only), then empty rows to write it alone. Text never wraps: the lines shrink to fit it.
     */
    fun assignment(text: String, script: Script, widthPx: Float, heightPx: Float, density: Float, size: PaperSize): Sheet {
        val margin = 14f * density
        val rowsCount = when (size) {
            PaperSize.BIG -> 2
            PaperSize.SMALL -> if (heightPx / density > 560f) 4 else 3
        }
        val laid = TextLayout.layout(text, script)
        val pad = 20f
        val unitH = heightPx / (rowsCount * Paper.ROW)
        val unitW = (widthPx - 2 * margin) / (laid.bounds.width + 2 * pad)
        val unit = min(min(unitH, unitW), 3.4f * density)
        val top0 = (heightPx - rowsCount * Paper.ROW * unit) / 2f
        val rows = List(rowsCount) { r -> PaperRow(top0 + r * Paper.ROW * unit, unit, margin, widthPx - margin) }
        val cells = rows.mapIndexed { r, row ->
            val guide = when {
                r == 0 -> Guide.DOTTED
                r == 1 && rowsCount > 2 -> Guide.FAINT
                else -> Guide.NONE
            }
            val ox = margin + pad * unit - laid.bounds.minX * unit
            val ghosts = laid.strokes.map { s ->
                GhostStroke(s.points.map { Pt(ox + it.x * unit, row.topLine + it.y * unit) }, s.isDot, s.strokeIndex + 1, s.glyphIndex)
            }
            Cell(r, row, r, row.left, row.right, guide, ghosts)
        }
        return Sheet(rows, cells)
    }

    private fun wrap(text: String, script: Script, maxUnits: Float): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val lines = ArrayList<String>()
        var current = ""
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (current.isNotEmpty() && TextLayout.layout(candidate, script).bounds.width > maxUnits) {
                lines += current
                current = w
            } else {
                current = candidate
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }
}
