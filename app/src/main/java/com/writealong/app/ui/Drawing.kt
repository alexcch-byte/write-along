package com.writealong.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.writealong.app.glyph.Nib
import com.writealong.app.glyph.Pt
import com.writealong.app.glyph.pathLength
import com.writealong.app.glyph.pointAlong
import com.writealong.app.practice.PaperRow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

fun Pt.o() = Offset(x, y)

/** Straight-segment path through dense points (for model letters). */
fun polyPath(pts: List<Pt>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
    return path
}

/** Smoothed path through finger points (for the child's ink). */
fun inkPath(pts: List<Pt>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    if (pts.size < 3) {
        pts.drop(1).forEach { path.lineTo(it.x, it.y) }
        return path
    }
    for (i in 1 until pts.size - 1) {
        val a = pts[i]
        val b = pts[i + 1]
        path.quadraticTo(a.x, a.y, (a.x + b.x) / 2f, (a.y + b.y) / 2f)
    }
    path.lineTo(pts.last().x, pts.last().y)
    return path
}

/** Top line, dashed middle line, red bottom line and a faint tail line. */
fun DrawScope.drawPaperRow(row: PaperRow, showTail: Boolean = true) {
    val u = row.unit
    val heavy = max(1.7f * u, 1.5.dp.toPx())
    val light = max(1.3f * u, 1.2.dp.toPx())
    // Soft band where the small letters live.
    drawRect(Wa.Zone, topLeft = Offset(row.left, row.midLine), size = Size(row.right - row.left, row.baseLine - row.midLine))
    drawLine(Wa.TopLine, Offset(row.left, row.topLine), Offset(row.right, row.topLine), heavy)
    val dash = max(9f * u, 7.dp.toPx())
    drawLine(
        Wa.MidLine, Offset(row.left, row.midLine), Offset(row.right, row.midLine), light,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 0.75f)),
    )
    drawLine(Wa.BaseLine, Offset(row.left, row.baseLine), Offset(row.right, row.baseLine), heavy * 1.15f)
    if (showTail) {
        drawLine(
            Wa.TailLine, Offset(row.left, row.tailLine), Offset(row.right, row.tailLine), light * 0.8f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash * 0.4f, dash * 0.6f)),
        )
    }
}

fun DrawScope.drawRoad(pts: List<Pt>, isDot: Boolean, color: Color, width: Float) {
    if (isDot || pts.size < 2) {
        drawCircle(color, width * 0.62f, pts.first().o())
    } else {
        drawPath(polyPath(pts), color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

fun DrawScope.drawDotted(pts: List<Pt>, isDot: Boolean, color: Color, width: Float, gap: Float) {
    if (isDot || pts.size < 2) {
        drawCircle(color, width * 1.3f, pts.first().o())
    } else {
        drawPath(
            polyPath(pts), color,
            style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, gap))),
        )
    }
}

fun DrawScope.drawInk(pts: List<Pt>, color: Color, width: Float, alpha: Float = 1f) {
    if (pts.isEmpty()) return
    if (pts.size == 1 || pathLength(pts) < width * 0.5f) {
        drawCircle(color, width * 0.6f, pts.first().o(), alpha = alpha)
        return
    }
    drawPath(inkPath(pts), color, alpha = alpha, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** How wide the flat nib is compared with the round pen of the same ink width. */
private const val NIB_RATIO = 1.5f

/**
 * Ink or pencil: with [nib] the stroke is written with a flat, broad-edge calligraphy pen
 * (thick across the nib, hair thin along it); otherwise it is a round pen.
 */
fun DrawScope.drawPen(nib: Boolean, pts: List<Pt>, color: Color, width: Float, alpha: Float = 1f) {
    if (nib) drawNibStroke(pts, color, width * NIB_RATIO, alpha) else drawInk(pts, color, width, alpha)
}

/**
 * Sweeps a flat nib along [pts]. The nib is a short straight edge [nibLen] long, held at
 * [Nib.ANGLE_DEG] (rising to the right). Every step of the path becomes a parallelogram, so a
 * stroke that runs along the nib edge is hair thin and one that crosses it is thick.
 */
fun DrawScope.drawNibStroke(pts: List<Pt>, color: Color, nibLen: Float, alpha: Float = 1f) {
    if (pts.isEmpty()) return
    val ang = Nib.ANGLE_DEG * PI.toFloat() / 180f
    val hx = cos(ang) * nibLen / 2f
    val hy = -sin(ang) * nibLen / 2f
    val first = pts.first()
    if (pts.size == 1 || pathLength(pts) < nibLen * 0.3f) {
        // A dot is a small diamond-ish stamp of the nib.
        drawLine(
            color, Offset(first.x - hx * 0.9f, first.y - hy * 0.9f), Offset(first.x + hx * 0.9f, first.y + hy * 0.9f),
            nibLen * 0.34f, cap = StrokeCap.Butt, alpha = alpha,
        )
        return
    }
    // Light smoothing, then drop points that are too close together to matter.
    val sm = ArrayList<Pt>(pts.size)
    for (i in pts.indices) {
        val a = pts[max(i - 1, 0)]
        val b = pts[i]
        val c = pts[minOf(i + 1, pts.lastIndex)]
        sm += if (i == 0 || i == pts.lastIndex) b else Pt((a.x + 2 * b.x + c.x) / 4f, (a.y + 2 * b.y + c.y) / 4f)
    }
    val minStep = max(nibLen / 14f, 1f)
    val line = ArrayList<Pt>(sm.size)
    line += sm.first()
    for (i in 1 until sm.size) {
        if (sm[i].dist(line.last()) >= minStep || i == sm.lastIndex) line += sm[i]
    }
    val body = Path()
    for (i in 1 until line.size) {
        val p = line[i - 1]
        val q = line[i]
        // Corners in an order that keeps every piece wound the same way, so pieces never cancel.
        val v = arrayOf(
            Offset(p.x - hx, p.y - hy), Offset(p.x + hx, p.y + hy),
            Offset(q.x + hx, q.y + hy), Offset(q.x - hx, q.y - hy),
        )
        var area = 0f
        for (k in 0 until 4) {
            val s = v[k]
            val e = v[(k + 1) % 4]
            area += s.x * e.y - e.x * s.y
        }
        if (area < 0f) v.reverse()
        body.moveTo(v[0].x, v[0].y)
        for (k in 1 until 4) body.lineTo(v[k].x, v[k].y)
        body.close()
    }
    // Hair line under the body so strokes along the nib edge never vanish.
    drawPath(
        polyPath(line), color, alpha = alpha,
        style = Stroke(max(nibLen * Nib.MIN_FRACTION, 1f), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(body, color, alpha = alpha)
}

/** Chevrons along a stroke showing the direction to move. */
fun DrawScope.drawArrows(pts: List<Pt>, color: Color, size: Float, spacing: Float, startAt: Float = spacing * 0.6f) {
    if (pts.size < 2) return
    val len = pathLength(pts)
    var d = startAt
    val stroke = max(size * 0.28f, 1.5.dp.toPx())
    while (d < len - size * 0.8f) {
        val (p, dir) = pointAlong(pts, d)
        val back = Offset(-dir.x * size, -dir.y * size)
        val side = Offset(-dir.y * size * 0.62f, dir.x * size * 0.62f)
        val tip = Offset(p.x + dir.x * size * 0.45f, p.y + dir.y * size * 0.45f)
        drawLine(color, tip, tip + back + side, stroke, cap = StrokeCap.Round)
        drawLine(color, tip, tip + back - side, stroke, cap = StrokeCap.Round)
        d += spacing
    }
}

/** Round badge with a stroke number in it. */
fun DrawScope.drawBadge(center: Pt, radius: Float, number: Int, color: Color, measurer: TextMeasurer, ring: Color? = Color.White) {
    ring?.let { drawCircle(it, radius * 1.18f, center.o()) }
    drawCircle(color, radius, center.o())
    val style = TextStyle(color = Color.White, fontSize = (radius * 1.15f).toSp(), fontWeight = FontWeight.ExtraBold)
    val layout = measurer.measure(number.toString(), style)
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

fun starPath(cx: Float, cy: Float, outer: Float): Path {
    val inner = outer * 0.48f
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

fun DrawScope.drawStars(centerX: Float, centerY: Float, radius: Float, earned: Int, total: Int = 3) {
    val gap = radius * 2.25f
    val start = centerX - gap * (total - 1) / 2f
    for (i in 0 until total) {
        val path = starPath(start + i * gap, centerY, radius)
        drawPath(path, if (i < earned) Wa.Star else Wa.StarEmpty)
        drawPath(path, Color(0x33000000), style = Stroke(max(radius * 0.08f, 1f), join = StrokeJoin.Round))
    }
}
