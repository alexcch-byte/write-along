package com.writealong.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.writealong.app.data.LocalServices
import com.writealong.app.glyph.Glyphs
import com.writealong.app.glyph.Script
import com.writealong.app.practice.PaperSize
import com.writealong.app.practice.SheetBuilder

enum class LessonMode(val label: String, val emoji: String) {
    WATCH("Watch", "👀"),
    TRACE("Trace", "✏️"),
    WRITE("Write", "⭐"),
}

/**
 * One letter: watch it being written, trace it stroke by stroke, then write it alone.
 */
@Composable
fun LessonScreen(script: Script, letter: Char, onBack: () -> Unit, onGoTo: (Char) -> Unit) {
    val services = LocalServices.current
    val glyph = remember(script, letter) { Glyphs.require(script, letter) }
    val color = Wa.scriptColor(script)
    val letters = if (letter.isUpperCase()) Glyphs.uppercase else Glyphs.lowercase
    val idx = letters.indexOf(letter)
    val prev = letters[(idx - 1 + letters.size) % letters.size]
    val next = letters[(idx + 1) % letters.size]

    var mode by remember(script, letter) { mutableStateOf(LessonMode.WATCH) }
    var size by rememberSaveable { mutableStateOf(PaperSize.BIG) }
    var banner by remember(script, letter, mode) { mutableStateOf("") }
    var runKey by remember(script, letter, mode, size) { mutableIntStateOf(0) }
    var celebrate by remember(script, letter, mode, size, runKey) { mutableStateOf<Int?>(null) }
    var board by remember { mutableStateOf<BoardState?>(null) }

    DisposableEffect(Unit) { onDispose { services.speaker.stop() } }

    Row(Modifier.fillMaxSize()) {
        SidePanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundButton("⬅️", "Back to letters", onBack)
                Spacer(Modifier.weight(1f))
                VoiceToggle()
            }
            VGap(6.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundButton("◀", "Previous letter", { onGoTo(prev) }, size = 36.dp)
                LetterArt(glyph.polylines, glyph.bounds, color, Modifier.weight(1f).height(80.dp), padUnits = 34f, nib = script == Script.CALLIGRAPHY)
                RoundButton("▶", "Next letter", { onGoTo(next) }, size = 36.dp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${script.label} $letter",
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                StarRow(services.progress.stars(script, letter), 14.dp)
            }
            SectionLabel("Steps")
            LessonMode.entries.forEach { m ->
                KidButton(
                    text = "${m.ordinal + 1}. ${m.label}",
                    onClick = { mode = m },
                    emoji = m.emoji,
                    selected = mode == m,
                    color = color,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                )
            }
            SectionLabel("Line size")
            ChoiceRow(
                options = PaperSize.entries, selected = size, label = { it.label },
                onSelect = { size = it }, color = color, enabled = mode != LessonMode.WATCH,
            )
            SectionLabel(if (mode == LessonMode.WRITE) "When you're done" else "Again")
            when (mode) {
                LessonMode.WATCH -> KidButton("Watch again", { runKey++ }, emoji = "🔁", color = color, modifier = Modifier.fillMaxWidth())
                LessonMode.TRACE -> KidButton("Start over", { runKey++ }, emoji = "🔁", color = color, modifier = Modifier.fillMaxWidth())
                LessonMode.WRITE -> {
                    KidButton("Check", { board?.check() }, emoji = "✅", color = Wa.Good, filled = true, modifier = Modifier.fillMaxWidth())
                    VGap(6.dp)
                    Row {
                        KidButton("Undo", { board?.undo() }, emoji = "↩️", color = color, modifier = Modifier.weight(1f))
                        Gap(6.dp)
                        KidButton("Clear", { runKey++ }, emoji = "🧽", color = color, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
        ) {
            InstructionBanner(banner, onSpeak = { services.speaker.say(banner) }, color = color)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (mode == LessonMode.WATCH) {
                    WatchBoard(glyph, runKey, color, onCaption = { banner = it })
                } else {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val w = constraints.maxWidth.toFloat()
                        val h = constraints.maxHeight.toFloat()
                        val boardMode = if (mode == LessonMode.TRACE) BoardMode.TRACE else BoardMode.WRITE
                        val state = rememberBoard(listOf(script, letter, size, runKey), boardMode, glyph, w, h) { ww, hh, d ->
                            SheetBuilder.lesson(glyph, ww, hh, d, size, trace = boardMode == BoardMode.TRACE)
                        }
                        state.onMessage = { text, speak ->
                            banner = text
                            if (speak) services.speaker.say(text)
                        }
                        state.onFinished = { stars ->
                            if (boardMode == BoardMode.TRACE) {
                                services.progress.record(script, letter, 1)
                                celebrate = 3
                            } else {
                                services.progress.record(script, letter, stars)
                                if (stars == 3) celebrate = 3
                            }
                        }
                        SideEffect { board = state }
                        LaunchedEffect(state) { state.begin() }
                        WritingBoard(state)
                    }
                }

                if (celebrate != null) {
                    if (mode == LessonMode.TRACE) {
                        Celebration(
                            emoji = "🎉",
                            title = "You traced it!",
                            message = "Now try writing $letter by yourself.",
                            stars = 3,
                            primary = "⭐ Write it" to { mode = LessonMode.WRITE },
                            secondary = "🔁 Trace again" to { runKey++ },
                            color = color,
                        )
                    } else {
                        Celebration(
                            emoji = "🏆",
                            title = "Super writing!",
                            message = "All your letters look great.",
                            stars = 3,
                            primary = "Next letter ▶" to { onGoTo(next) },
                            secondary = "🔁 Practice more" to { runKey++ },
                            color = color,
                        )
                    }
                }
            }
        }
    }
}
