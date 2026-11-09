package com.writealong.app.practice

import com.writealong.app.glyph.Box
import com.writealong.app.glyph.Pt
import com.writealong.app.glyph.distanceToPolyline
import com.writealong.app.glyph.pathLength
import com.writealong.app.glyph.resample
import kotlin.math.abs
import kotlin.math.max

enum class TraceProblem(val message: String) {
    WRONG_START("Start at the green dot."),
    WRONG_WAY("Oops, other way! Follow the arrows."),
    OFF_PATH("Stay on the gray path."),
    TOO_SHORT("Keep going to the end without lifting your pencil."),
    DOT("Just tap a little dot on the spot."),
}

class TraceResult(val ok: Boolean, val problem: TraceProblem? = null)

enum class WriteProblem(val message: String) {
    WRONG_WAY("Check which way your pencil goes. Follow the arrows on the dotted letter."),
    ORDER("Remember the order. Follow the numbers on the dotted letter."),
    TOO_SHORT("Make it taller. Reach up to the right line."),
    TOO_TALL("Not quite so tall. Watch where the dotted letters stop."),
    FLOATING("Sit your writing down on the bottom line."),
    SINKING("Stop on the bottom line."),
    NEEDS_TAIL("The tail goes down below the bottom line."),
    TOO_MANY("Try to make it with fewer pencil lifts."),
    LETTER_HEIGHTS("Look closely at each letter: which ones are tall, which are small, and which have tails?"),
}

class CellScore(val stars: Int, val tip: String, val problems: List<WriteProblem>, val shape: Float = 0f)

/**
 * Compares a child's strokes with the model letter.
 *
 * Tolerances are measured in letter units (100 units = top line to bottom line), with a floor
 * in pixels so tiny letters stay fair for small fingers.
 */
object StrokeChecker {
    private const val N = 48

    /** Tracing: is this one stroke the stroke we asked for? */
    fun checkTrace(user: List<Pt>, model: GhostStroke, unit: Float, minTolPx: Float): TraceResult {
        val tol = max(15f * unit, minTolPx)
        if (user.isEmpty()) return TraceResult(false, TraceProblem.OFF_PATH)

        if (model.isDot) {
            val target = model.points[0]
            val len = pathLength(user)
            return when {
                centroid(user).dist(target) > tol * 1.5f -> TraceResult(false, TraceProblem.OFF_PATH)
                len > 30f * unit + minTolPx -> TraceResult(false, TraceProblem.DOT)
                else -> TraceResult(true)
            }
        }

        val m = model.points
        val userLen = pathLength(user)
        val modelLen = pathLength(m)
        if (user.size < 2 || userLen < tol * 0.6f) {
            return TraceResult(false, if (user.first().dist(m.first()) < tol * 1.6f) TraceProblem.TOO_SHORT else TraceProblem.WRONG_START)
        }
        val ru = resample(user, N)
        val rm = resample(m, N)
        val coverage = rm.count { distanceToPolyline(it, user) <= tol }.toFloat() / rm.size
        val precision = ru.count { distanceToPolyline(it, m) <= tol }.toFloat() / ru.size
        var fwd = 0f
        var rev = 0f
        for (i in 0 until N) {
            fwd += ru[i].dist(rm[i])
            rev += ru[i].dist(rm[N - 1 - i])
        }
        fwd /= N
        rev /= N
        val startDist = user.first().dist(m.first())

        if (precision < 0.68f) return TraceResult(false, TraceProblem.OFF_PATH)
        if (coverage < 0.72f) {
            return TraceResult(false, if (userLen < modelLen * 0.9f) TraceProblem.TOO_SHORT else TraceProblem.OFF_PATH)
        }
        if (rev < fwd * 0.8f && fwd > tol * 0.7f) return TraceResult(false, TraceProblem.WRONG_WAY)
        if (startDist > tol * 1.7f) return TraceResult(false, TraceProblem.WRONG_START)
        return TraceResult(true)
    }

    /** Writing: score everything the child wrote in one practice spot. Null if the spot is empty. */
    fun scoreCell(user: List<List<Pt>>, cell: Cell, minTolPx: Float, words: Boolean = false): CellScore? {
        if (user.isEmpty() || cell.strokes.isEmpty()) return null
        val u = cell.unit
        val tol = max(14f * u, minTolPx)
        // Tighter, direction-aware tolerance for judging shape.
        val shapeTol = max(9f * u, minTolPx * 0.6f)

        val modelAll = cell.strokes.flatMap { it.points }
        val userAll = user.flatten()
        val mb = Box.of(modelAll)
        val ub = Box.of(userAll)

        // Where along the line the letter sits doesn't matter; how it sits on the lines does.
        // Words are also stretched to the child's width, since letter spacing varies a lot.
        val model = if (words && mb.width > 1f) {
            val k = (ub.width / mb.width).coerceIn(0.7f, 1.4f)
            cell.strokes.map { s -> s.points.map { Pt(ub.centerX + (it.x - mb.centerX) * k, it.y) } }
        } else {
            val dx = (ub.centerX - mb.centerX).coerceIn(-cell.width / 2f, cell.width / 2f)
            cell.strokes.map { s -> s.points.map { Pt(it.x + dx, it.y) } }
        }

        val denseUser = user.map { s ->
            if (s.size < 2) s else resample(s, max(2, (pathLength(s) / (1.5f * u)).toInt()))
        }
        val modelPts = oriented(model)
        val userPts = oriented(denseUser)
        // Coverage per part (each letter of a word, or each stroke of a letter), so one wrong or
        // missing part pulls the score down instead of hiding inside the average.
        val partOf = if (words) cell.strokes.map { it.group } else cell.strokes.indices.toList()
        val parts = model.indices.groupBy { partOf[it] }.values.map { idx -> oriented(idx.map { model[it] }) }
        val partCov: List<Float>
        val precision: Float
        var zoneMisses = 0
        if (words) {
            // Children space letters unevenly, so each letter may slide sideways a little to find
            // its best fit. That lets us compare the shapes themselves more strictly.
            val wordTol = max(7f * u, minTolPx * 0.5f)
            val slide = 20f * u
            val step = 2.5f * u
            val fitted = parts.map { part ->
                val x0 = part.minOf { it.x } - slide - wordTol
                val x1 = part.maxOf { it.x } + slide + wordTol
                val near = userPts.filter { it.x in x0..x1 }
                var best = -1f
                var bestDx = 0f
                var dx = -slide
                while (dx <= slide + 0.01f) {
                    val c = matchedFraction(part, near, wordTol, dx)
                    if (c > best + 1e-4f || (abs(c - best) <= 1e-4f && abs(dx) < abs(bestDx))) {
                        best = c
                        bestDx = dx
                    }
                    dx += step
                }
                best to part.map { it.shifted(bestDx) }
            }
            partCov = fitted.map { it.first }
            precision = matchedFraction(userPts, fitted.flatMap { it.second }, wordTol)
            // Does each letter reach the same zones (tall / small / tail) as the model letter?
            // Each bit of ink belongs to the nearest model letter.
            val top = cell.row.topLine
            val n = fitted.size
            val inkTop = FloatArray(n) { Float.MAX_VALUE }
            val inkBot = FloatArray(n) { -Float.MAX_VALUE }
            val cap2 = (22f * u) * (22f * u)
            for (p in userPts) {
                var bestG = -1
                var bestD = cap2
                fitted.forEachIndexed { gi, (_, part) ->
                    for (q in part) {
                        val dx = p.x - q.x
                        val dy = p.y - q.y
                        val d2 = dx * dx + dy * dy
                        if (d2 < bestD) {
                            bestD = d2
                            bestG = gi
                        }
                    }
                }
                if (bestG >= 0) {
                    if (p.y < inkTop[bestG]) inkTop[bestG] = p.y
                    if (p.y > inkBot[bestG]) inkBot[bestG] = p.y
                }
            }
            zoneMisses = fitted.indices.count { gi ->
                val part = fitted[gi].second
                if (inkTop[gi] == Float.MAX_VALUE) {
                    true
                } else {
                    val mTop = (part.minOf { it.y } - top) / u
                    val mBot = (part.maxOf { it.y } - top) / u
                    abs((inkTop[gi] - top) / u - mTop) > 20f || abs((inkBot[gi] - top) / u - mBot) > 20f
                }
            }
        } else {
            partCov = parts.map { matchedFraction(it, userPts, shapeTol) }
            precision = matchedFraction(userPts, modelPts, shapeTol)
        }
        val meanCov = parts.indices.sumOf { (partCov[it] * parts[it].size).toDouble() }.toFloat() / max(1, modelPts.size)
        val minCov = partCov.minOrNull() ?: 0f
        val coverage = if (words) 0.6f * meanCov + 0.4f * minCov else 0.75f * meanCov + 0.25f * minCov
        val zoneMissRate = if (parts.isEmpty()) 0f else zoneMisses.toFloat() / parts.size
        val shape = (0.5f * coverage + 0.5f * precision) * (1f - 0.6f * zoneMissRate)

        val problems = ArrayList<WriteProblem>()
        if (zoneMisses > 0) problems += WriteProblem.LETTER_HEIGHTS

        // Stroke direction and order, when the stroke count matches.
        if (words) {
            // Stroke order across a whole word is too fiddly to judge fairly.
        } else if (user.size == model.size && model.size > 0) {
            var wrongWay = false
            var wrongOrder = false
            for (i in model.indices) {
                if (cell.strokes[i].isDot || user[i].size < 2) continue
                val ru = resample(user[i], N)
                val rm = resample(model[i], N)
                var fwd = 0f
                var rev = 0f
                for (k in 0 until N) {
                    fwd += ru[k].dist(rm[k]); rev += ru[k].dist(rm[N - 1 - k])
                }
                fwd /= N; rev /= N
                if (rev < fwd * 0.75f && fwd > tol * 0.8f) wrongWay = true
            }
            if (model.size > 1) {
                for (i in user.indices) {
                    val c = centroid(user[i])
                    val nearest = model.indices.minBy { j -> centroid(model[j]).dist(c) }
                    if (nearest != i && centroid(model[nearest]).dist(centroid(model[i])) > tol) wrongOrder = true
                }
            }
            if (wrongWay) problems += WriteProblem.WRONG_WAY
            if (wrongOrder) problems += WriteProblem.ORDER
        } else if (user.size > model.size + 2) {
            problems += WriteProblem.TOO_MANY
        }

        // Sitting on the lines.
        val topLine = cell.row.topLine
        fun units(y: Float) = (y - topLine) / u
        val mTop = units(mb.minY)
        val mBot = units(mb.maxY)
        val uTop = units(ub.minY)
        val uBot = units(ub.maxY)
        when {
            uTop - mTop > 16f -> problems += WriteProblem.TOO_SHORT
            mTop - uTop > 18f -> problems += WriteProblem.TOO_TALL
        }
        when {
            mBot > 120f && mBot - uBot > 18f -> problems += WriteProblem.NEEDS_TAIL
            mBot - uBot > 14f -> problems += WriteProblem.FLOATING
            mBot < 110f && uBot - mBot > 16f -> problems += WriteProblem.SINKING
        }

        val cut = if (words) floatArrayOf(0.74f, 0.56f, 0.36f) else floatArrayOf(0.80f, 0.62f, 0.40f)
        var stars = when {
            shape >= cut[0] -> 3
            shape >= cut[1] -> 2
            shape >= cut[2] -> 1
            else -> 0
        }
        if (stars == 3 && problems.isNotEmpty()) stars = 2

        val tip = when {
            stars == 0 -> "Let's try that one again. Trace the dotted ${if (words) "writing" else "letter"} first."
            problems.isNotEmpty() -> problems.first().message
            stars == 3 -> praise(abs(ub.centerX.toInt()))
            stars == 2 -> "Good! Try to match the dotted ${if (words) "writing" else "letter"} even more closely."
            else -> "Nice try! Look at the dotted ${if (words) "writing" else "letter"} and try again."
        }
        return CellScore(stars, tip, problems, shape)
    }

    /** A point plus the direction the line runs there (null for dots). */
    private class OPt(val x: Float, val y: Float, val tx: Float, val ty: Float, val hasDir: Boolean) {
        fun shifted(dx: Float) = OPt(x + dx, y, tx, ty, hasDir)
    }

    private fun oriented(strokes: List<List<Pt>>): List<OPt> {
        val out = ArrayList<OPt>()
        for (s in strokes) {
            if (s.size < 2) {
                s.forEach { out += OPt(it.x, it.y, 0f, 0f, false) }
                continue
            }
            for (i in s.indices) {
                val a = s[max(0, i - 2)]
                val b = s[minOf(s.size - 1, i + 2)]
                val d = (b - a)
                val l = d.length()
                if (l < 1e-3f) out += OPt(s[i].x, s[i].y, 0f, 0f, false)
                else out += OPt(s[i].x, s[i].y, d.x / l, d.y / l, true)
            }
        }
        return out
    }

    /**
     * Fraction of [from] points that have a point of [to] nearby whose line runs roughly the
     * same way (either direction). Dots match anything close.
     */
    private fun matchedFraction(from: List<OPt>, to: List<OPt>, tol: Float, shiftFrom: Float = 0f): Float {
        if (from.isEmpty()) return 0f
        val tol2 = tol * tol
        var hit = 0
        for (p in from) {
            var ok = false
            for (q in to) {
                val dx = p.x + shiftFrom - q.x
                val dy = p.y - q.y
                if (dx * dx + dy * dy > tol2) continue
                if (!p.hasDir || !q.hasDir || abs(p.tx * q.tx + p.ty * q.ty) >= 0.6f) {
                    ok = true
                    break
                }
            }
            if (ok) hit++
        }
        return hit.toFloat() / from.size
    }

    private val praises = listOf("Excellent!", "Super writing!", "Perfect!", "Wow, great job!", "Beautiful!")
    fun praise(seed: Int): String = praises[seed.mod(praises.size)]

    fun centroid(pts: List<Pt>): Pt {
        if (pts.isEmpty()) return Pt(0f, 0f)
        var x = 0f
        var y = 0f
        pts.forEach { x += it.x; y += it.y }
        return Pt(x / pts.size, y / pts.size)
    }
}
