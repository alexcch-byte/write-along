package com.writealong.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.writealong.app.glyph.Box as GBox
import com.writealong.app.glyph.Pt
import kotlin.math.max
import kotlin.math.min

/** Big, friendly button. Selected buttons fill with [color]. */
@Composable
fun KidButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    color: Color = Wa.Primary,
    selected: Boolean = false,
    filled: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 46.dp,
) {
    val solid = selected || filled
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = height),
        shape = RoundedCornerShape(16.dp),
        color = if (solid) color else Color.White,
        contentColor = if (solid) Color.White else Wa.Text,
        border = if (solid) null else BorderStroke(2.dp, color.copy(alpha = if (enabled) 0.55f else 0.2f)),
        shadowElevation = if (solid) 3.dp else 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (emoji != null) {
                Text(emoji, fontSize = 17.sp)
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (enabled) Color.Unspecified else Wa.SubText,
            )
        }
    }
}

/** Round emoji button (back, sound, arrows). */
@Composable
fun RoundButton(emoji: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Color.White, size: Dp = 46.dp) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = color,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = (size.value * 0.42f).sp, textAlign = TextAlign.Center) }
    }
}

/** Row of mutually exclusive choices. */
@Composable
fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { opt ->
            KidButton(
                text = label(opt),
                onClick = { onSelect(opt) },
                selected = opt == selected,
                color = color,
                enabled = enabled,
                height = 40.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Wa.SubText,
        modifier = modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp),
    )
}

@Composable
fun StarRow(earned: Int, size: Dp, modifier: Modifier = Modifier, total: Int = 3) {
    Canvas(modifier.size(width = size * total + size * 0.25f * (total - 1), height = size)) {
        val r = this.size.height / 2f
        drawStars(this.size.width / 2f, r, r * 0.95f, earned, total)
    }
}

/** Banner across the top of the writing area with the current instruction. */
@Composable
fun InstructionBanner(text: String, onSpeak: () -> Unit, modifier: Modifier = Modifier, color: Color = Wa.Primary) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text.ifBlank { " " },
                style = MaterialTheme.typography.bodyLarge,
                color = Wa.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            RoundButton("🔊", "Say it again", onSpeak, size = 40.dp)
        }
    }
}

/**
 * Draws letter strokes (in letter units) on a little set of writing lines, scaled to fit.
 */
@Composable
fun LetterArt(
    strokes: List<List<Pt>>,
    bounds: GBox,
    color: Color,
    modifier: Modifier = Modifier,
    lines: Boolean = true,
    weight: Float = 8f,
    padUnits: Float = 30f,
) {
    Canvas(modifier) {
        val top = -18f
        val bottom = 160f
        val u = min(size.height / (bottom - top), size.width / (bounds.width + padUnits))
        val oy = (size.height - (bottom - top) * u) / 2f - top * u
        val ox = size.width / 2f - bounds.centerX * u
        if (lines) {
            val lw = max(1.2f * u, 1f)
            drawLine(Wa.TopLine.copy(alpha = 0.7f), Offset(0f, oy), Offset(size.width, oy), lw)
            drawLine(
                Wa.MidLine.copy(alpha = 0.8f), Offset(0f, oy + 50f * u), Offset(size.width, oy + 50f * u), lw,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * u + 3f, 5f * u + 2f)),
            )
            drawLine(Wa.BaseLine.copy(alpha = 0.8f), Offset(0f, oy + 100f * u), Offset(size.width, oy + 100f * u), lw * 1.2f)
        }
        val w = max(weight * u, 2.dp.toPx())
        strokes.forEach { line -> drawInk(line.map { Pt(ox + it.x * u, oy + it.y * u) }, color, w) }
    }
}

@Composable
fun RowScope.Gap(w: Dp = 8.dp) = Spacer(Modifier.width(w))

@Composable
fun VGap(h: Dp = 8.dp) = Spacer(Modifier.height(h))
