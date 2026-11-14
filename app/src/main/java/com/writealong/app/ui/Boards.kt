package com.writealong.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.writealong.app.data.LocalServices
import com.writealong.app.glyph.Glyph
import com.writealong.app.glyph.Pt
import com.writealong.app.glyph.pathLength
import com.writealong.app.glyph.prefix
import com.writealong.app.practice.Cell
import com.writealong.app.practice.CellScore
import com.writealong.app.practice.Guide
import com.writealong.app.practice.Sheet
import com.writealong.app.practice.SheetBuilder
import com.writealong.app.practice.StrokeChecker
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class BoardMode { TRACE, WRITE, FREE }

class InkStroke(val points: List<Pt>, val color: Color)

/**
 * Everything happening on one writing surface: the sheet (lines + guides), the child's ink,
 * and — for tracing — which stroke we're waiting for.
 */
class BoardState(
    val sheet: Sheet,
    val mode: BoardMode,
    val glyph: Glyph?,
    private val minTolPx: Float,
    private val words: Boolean = false,
) {
    /** Overrides the opening instruction in WRITE mode. */
    var intro: String? = null
    var onMessage: (text: String, speak: Boolean) -> Unit = { _, _ -> }
    var onFinished: (stars: Int) -> Unit = {}

    val inks = mutableStateListOf<InkStroke>()
    val current = mutableStateListOf<Pt>()
    var penColor by mutableStateOf(Wa.Ink)
    var rejected by mutableStateOf<List<Pt>?>(null)
    var rejectTick by mutableIntStateOf(0)
    var cellIdx by mutableIntStateOf(0)
        private set
    var strokeIdx by mutableIntStateOf(0)
        private set
    var results by mutableStateOf<Map<Int, CellScore>>(emptyMap())
        private set

    private var stylusSeen = false
    private var praiseSeed = 0

    val traceDone: Boolean get() = mode == BoardMode.TRACE && cellIdx >= sheet.cells.size

    fun begin() {
        when (mode) {
            BoardMode.TRACE -> announce(speak = true)
            BoardMode.WRITE -> onMessage(
                intro ?: "Trace the dotted letter, then write it by yourself in the empty spaces. Tap Check when you're done.",
                true,
            )
            BoardMode.FREE -> {}
        }
    }

    private fun announce(speak: Boolean) {
        val g = glyph ?: return
        if (traceDone) return
        val n = g.strokeCount
        val prefix = if (n > 1) "Stroke ${strokeIdx + 1} of $n. " else ""
        onMessage(prefix + g.instruction(strokeIdx), speak)
    }

    /** Palm rejection: once a stylus has been used, ignore finger touches. */
    fun acceptsPointer(type: PointerType): Boolean {
        if (type == PointerType.Stylus || type == PointerType.Eraser) {
            stylusSeen = true
            return true
        }
        return !stylusSeen
    }

    fun down(p: Pt) {
        if (traceDone) return
        current.clear()
        current += p
    }

    fun move(p: Pt) {
        if (current.isEmpty()) return
        if (p.dist(current.last()) >= 1.5f) current += p
    }

    fun up() {
        if (current.isEmpty()) return
        val pts = current.toList()
        current.clear()
        when (mode) {
            BoardMode.TRACE -> handleTrace(pts)
            BoardMode.WRITE, BoardMode.FREE -> {
                inks += InkStroke(pts, penColor)
                if (results.isNotEmpty()) results = emptyMap()
            }
        }
    }

    private fun handleTrace(pts: List<Pt>) {
        val cell = sheet.cells.getOrNull(cellIdx) ?: return
        val model = cell.strokes[strokeIdx]
        val result = StrokeChecker.checkTrace(pts, model, cell.unit, minTolPx)
        if (!result.ok) {
            rejected = pts
            rejectTick++
            onMessage(result.problem?.message ?: "Try again!", true)
            return
        }
        rejected = null
        inks += InkStroke(pts, penColor)
        if (strokeIdx + 1 < cell.strokes.size) {
            strokeIdx++
            announce(speak = cellIdx == 0)
            if (cellIdx > 0) onMessage("Next stroke! " + (glyph?.instruction(strokeIdx) ?: ""), true)
            return
        }
        strokeIdx = 0
        cellIdx++
        if (cellIdx >= sheet.cells.size) {
            onMessage("You traced it! Great job!", true)
            onFinished(3)
        } else {
            val cheer = StrokeChecker.praise(praiseSeed++)
            onMessage("$cheer Now trace the next one.", true)
        }
    }

    fun undo() {
        if (mode == BoardMode.TRACE) return
        if (inks.isNotEmpty()) inks.removeAt(inks.lastIndex)
        results = emptyMap()
    }

    fun clear() {
        inks.clear()
        current.clear()
        results = emptyMap()
    }

    /** Score every practice spot that has writing in it. */
    fun check() {
        if (mode != BoardMode.WRITE) return
        val groups = HashMap<Int, MutableList<List<Pt>>>()
        for (ink in inks) {
            val cell = cellFor(ink.points) ?: continue
            groups.getOrPut(cell.index) { mutableListOf() } += ink.points
        }
        val scores = HashMap<Int, CellScore>()
        for (cell in sheet.cells) {
            val s = StrokeChecker.scoreCell(groups[cell.index] ?: emptyList(), cell, minTolPx, words) ?: continue
            scores[cell.index] = s
        }
        results = scores
        if (scores.isEmpty()) {
            onMessage(if (words) "Write it on the lines first, then tap Check." else "Write the letter in the spaces first, then tap Check.", true)
            return
        }
        // Three stars overall only when every letter earned three.
        val overall = scores.values.map { it.stars }.average().roundToInt().coerceIn(0, 3)
            .let { if (it == 3 && scores.values.any { s -> s.stars < 3 }) 2 else it }
        val tip = scores.values.filter { it.stars < 3 }
            .groupingBy { it.tip }.eachCount().maxByOrNull { it.value }?.key
        val msg = when {
            tip == null -> if (words) "Amazing! Your writing looks great!" else "Amazing! Every letter looks great!"
            overall >= 2 -> "Nice work! Tip: $tip"
            else -> "Good try! $tip"
        }
        onMessage(msg, true)
        onFinished(overall)
    }

    private fun cellFor(points: List<Pt>): Cell? {
        if (points.isEmpty()) return null
        val c = StrokeChecker.centroid(points)
        val row = sheet.rows.minByOrNull { r -> kotlin.math.abs(c.y - (r.topLine + 75f * r.unit)) } ?: return null
        return sheet.cells.filter { it.row == row }.minByOrNull { kotlin.math.abs(it.centerX - c.x) }
    }
}

private fun Offset.toPt() = Pt(x, y)

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.boardInput(state: BoardState): Modifier = pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!state.acceptsPointer(down.type)) return@awaitEachGesture
        down.consume()
        state.down(down.position.toPt())
        val id = down.id
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == id } ?: break
            change.historical.forEach { state.move(it.position.toPt()) }
            state.move(change.position.toPt())
            change.consume()
            if (!change.pressed) break
        }
        state.up()
    }
}

fun DrawScope.inkWidth(unit: Float) = max(5.5f * unit, 3.5.dp.toPx())

/** Builds (and rebuilds on resize) the sheet + state for a writing surface. */
@Composable
fun rememberBoard(
    key: Any,
    mode: BoardMode,
    glyph: Glyph?,
    width: Float,
    height: Float,
    words: Boolean = false,
    build: (w: Float, h: Float, density: Float) -> Sheet,
): BoardState {
    val density = LocalDensity.current.density
    return remember(key, mode, width, height) {
        BoardState(build(width, height, density), mode, glyph, minTolPx = 12f * density, words = words)
    }
}

@Composable
fun WritingBoard(state: BoardState, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "pulseValue",
    )
    val rejectAlpha = remember(state) { Animatable(0f) }
    LaunchedEffect(state, state.rejectTick) {
        if (state.rejectTick > 0) {
            rejectAlpha.snapTo(1f)
            delay(300)
            rejectAlpha.animateTo(0f, tween(700))
        }
    }
    Canvas(
        modifier
            .fillMaxSize()
            .systemGestureExclusion()
            .boardInput(state),
    ) {
        drawBoard(state, measurer, if (state.mode == BoardMode.TRACE) pulse else 0f, rejectAlpha.value)
    }
}

private fun DrawScope.drawBoard(state: BoardState, measurer: TextMeasurer, pulse: Float, rejectAlpha: Float) {
    val sheet = state.sheet
    // Result tint behind each checked spot.
    state.results.forEach { (idx, score) ->
        val cell = sheet.cells[idx]
        val tint = when (score.stars) {
            3 -> Wa.Good
            2 -> Wa.Star
            1 -> Wa.Warn
            else -> Wa.Bad
        }
        drawRoundRect(
            tint.copy(alpha = 0.13f),
            topLeft = Offset(cell.left + 3.dp.toPx(), cell.row.top),
            size = Size(cell.width - 6.dp.toPx(), cell.row.bottom - cell.row.top),
            cornerRadius = CornerRadius(14.dp.toPx()),
        )
    }
    sheet.rows.forEach { drawPaperRow(it) }
    sheet.cells.forEach { drawGuide(state, it, measurer, pulse) }

    val inkW = inkWidth(sheet.unit)
    state.inks.forEach { drawPen(sheet.nib, it.points, it.color, inkW) }
    state.rejected?.let { if (rejectAlpha > 0.01f) drawPen(sheet.nib, it, Wa.Bad, inkW, rejectAlpha) }
    if (state.current.isNotEmpty()) drawPen(sheet.nib, state.current.toList(), state.penColor, inkW)

    state.results.forEach { (idx, score) ->
        val cell = sheet.cells[idx]
        val u = cell.unit
        val r = min(max(8f * u, 7.dp.toPx()), cell.width / 9f)
        drawStars(cell.centerX, cell.row.top + max(13f * u, r * 1.2f), r, score.stars)
    }
}

private fun DrawScope.drawGuide(state: BoardState, cell: Cell, measurer: TextMeasurer, pulse: Float) {
    val u = cell.unit
    when (cell.guide) {
        Guide.TRACE -> {
            val roadW = max(13f * u, 15.dp.toPx())
            val active = state.mode == BoardMode.TRACE && cell.index == state.cellIdx
            val done = state.mode == BoardMode.TRACE && cell.index < state.cellIdx
            cell.strokes.forEachIndexed { i, s ->
                val color = when {
                    done || (active && i < state.strokeIdx) -> Wa.RoadDone
                    active && i == state.strokeIdx -> Wa.RoadActive
                    else -> Wa.Road
                }
                drawRoad(s.points, s.isDot, color, roadW)
            }
            if (active) {
                val badgeR = max(7.5f * u, 10.dp.toPx())
                // Upcoming strokes get small gray numbers.
                for (i in state.strokeIdx + 1 until cell.strokes.size) {
                    val s = cell.strokes[i]
                    drawBadge(s.points.first(), badgeR * 0.72f, s.number, Wa.Dotted, measurer)
                }
                val s = cell.strokes[state.strokeIdx]
                if (!s.isDot) {
                    val arrow = max(5f * u, 6.dp.toPx())
                    drawArrows(s.points, Wa.Arrow, arrow, spacing = max(26f * u, 30.dp.toPx()), startAt = badgeR * 2.2f)
                    drawCircle(Color.White, badgeR * 0.55f, s.points.last().o())
                    drawCircle(Wa.StopDot, badgeR * 0.42f, s.points.last().o())
                }
                drawBadge(s.points.first(), badgeR * (1f + 0.18f * pulse), s.number, Wa.StartDot, measurer)
            }
        }
        Guide.DOTTED -> {
            val w = max(3.4f * u, 2.8.dp.toPx())
            val gap = max(8f * u, 6.dp.toPx())
            cell.strokes.forEach { drawDotted(it.points, it.isDot, Wa.Dotted, w, gap) }
            if (state.mode != BoardMode.FREE) {
                val badgeR = max(6f * u, 8.dp.toPx())
                cell.strokes.forEach { s ->
                    if (!s.isDot) {
                        val len = pathLength(s.points)
                        val arrow = max(4.5f * u, 5.dp.toPx())
                        drawArrows(s.points, Wa.StartDot, arrow, spacing = len * 2f, startAt = min(len * 0.45f, badgeR * 3.2f))
                    }
                }
                cell.strokes.asReversed().forEach { s -> drawBadge(s.points.first(), badgeR, s.number, Wa.StartDot, measurer) }
            }
        }
        Guide.FAINT -> {
            val w = max(3.2f * u, 2.4.dp.toPx())
            if (state.sheet.nib) {
                val nibLen = inkWidth(state.sheet.unit) * 1.5f
                cell.strokes.forEach { drawNibStroke(it.points, Wa.Faint, nibLen) }
            } else {
                cell.strokes.forEach { drawRoad(it.points, it.isDot, Wa.Faint, if (it.isDot) w * 1.8f else w) }
            }
        }
        Guide.NONE -> {}
    }
}

/**
 * "Watch" mode: the letter draws itself stroke by stroke while the steps are read aloud.
 */
@Composable
fun WatchBoard(glyph: Glyph, playKey: Int, color: Color, onCaption: (String) -> Unit, modifier: Modifier = Modifier) {
    val services = LocalServices.current
    val caption by rememberUpdatedState(onCaption)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current.density
    BoxWithConstraints(modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val sheet = remember(glyph, w, h) { SheetBuilder.demo(glyph, w, h, density) }
        var strokeNow by remember(glyph, playKey) { mutableIntStateOf(-1) }
        var frac by remember(glyph, playKey) { mutableFloatStateOf(0f) }

        LaunchedEffect(glyph, playKey, sheet) {
            strokeNow = -1
            frac = 0f
            delay(450)
            for (i in 0 until glyph.strokeCount) {
                strokeNow = i
                frac = 0f
                val n = glyph.strokeCount
                caption((if (n > 1) "${i + 1}. " else "") + glyph.instruction(i))
                services.speaker.say(glyph.instruction(i))
                delay(400)
                val lenUnits = pathLength(glyph.polylines[i])
                val dur = if (glyph.strokes[i].isDot) 300 else (700 + lenUnits * 11f).toInt().coerceAtMost(4500)
                animate(0f, 1f, animationSpec = tween(dur, easing = LinearEasing)) { v, _ -> frac = v }
                services.speaker.awaitDone()
                delay(250)
            }
            strokeNow = glyph.strokeCount
            val msg = "Your turn! Tap Trace to try it."
            caption(msg)
            services.speaker.say(msg)
        }

        Canvas(Modifier.fillMaxSize()) {
            val cell = sheet.cells[0]
            val u = cell.unit
            drawPaperRow(sheet.rows[0])
            val roadW = max(11f * u, 12.dp.toPx())
            cell.strokes.forEach { drawRoad(it.points, it.isDot, Wa.Road, roadW) }
            val inkW = max(6.5f * u, 4.dp.toPx())
            cell.strokes.forEachIndexed { i, s ->
                when {
                    i < strokeNow -> drawPen(sheet.nib, s.points, color, inkW)
                    i == strokeNow -> {
                        if (s.isDot) {
                            if (frac > 0.3f) drawPen(sheet.nib, s.points, color, inkW)
                        } else {
                            val part = prefix(s.points, pathLength(s.points) * frac)
                            drawPen(sheet.nib, part, color, inkW)
                            val tip = part.last()
                            drawCircle(Color.White, inkW * 1.25f, tip.o())
                            drawCircle(Wa.Text, inkW * 0.85f, tip.o())
                        }
                    }
                }
            }
            if (strokeNow in 0 until cell.strokes.size) {
                val s = cell.strokes[strokeNow]
                if (!s.isDot) {
                    drawArrows(s.points, Wa.Arrow.copy(alpha = 0.55f), max(5f * u, 6.dp.toPx()), max(26f * u, 30.dp.toPx()))
                }
            }
            val badgeR = max(8f * u, 11.dp.toPx())
            cell.strokes.asReversed().forEach { s ->
                val isNow = s.number - 1 == strokeNow
                drawBadge(s.points.first(), badgeR, s.number, if (isNow) Wa.StartDot else Wa.Dotted, measurer)
            }
        }
    }
}
