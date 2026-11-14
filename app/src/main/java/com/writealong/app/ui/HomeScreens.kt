package com.writealong.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.writealong.app.data.LocalServices
import com.writealong.app.glyph.Glyphs
import com.writealong.app.glyph.Script
import com.writealong.app.glyph.TextLayout
import com.writealong.app.practice.Assignments

@Composable
fun VoiceToggle(size: androidx.compose.ui.unit.Dp = 46.dp) {
    val services = LocalServices.current
    val on = services.progress.voiceOn
    RoundButton(
        emoji = if (on) "🔊" else "🔇",
        description = if (on) "Turn voice off" else "Turn voice on",
        onClick = {
            services.progress.updateVoice(!on)
            if (on) services.speaker.stop() else services.speaker.say("Voice is on.")
        },
        size = size,
    )
}

/** Left-hand control column used by the writing screens. */
@Composable
fun SidePanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .width(190.dp)
            .fillMaxHeight()
            .background(Wa.Panel)
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        content = content,
    )
}

@Composable
fun HomeScreen(onScript: (Script) -> Unit, onFree: () -> Unit, onAssignments: () -> Unit) {
    val progress = LocalServices.current.progress
    val all = Glyphs.lowercase + Glyphs.uppercase
    val maxStars = all.size * 3
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Write Along ✏️", style = MaterialTheme.typography.displaySmall, color = Wa.Primary)
                Text(
                    "Learn to write every letter the right way, in print, cursive and calligraphy.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Wa.SubText,
                )
            }
            VoiceToggle()
        }
        VGap(16.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val printSample = remember { TextLayout.layout("Aa", Script.PRINT) }
            val cursiveSample = remember { TextLayout.layout("Aa", Script.CURSIVE) }
            val calligraphySample = remember { TextLayout.layout("Aa", Script.CALLIGRAPHY) }
            val freeSample = remember { TextLayout.layout("hi", Script.CURSIVE) }
            val taskSample = remember { TextLayout.layout("I can", Script.PRINT) }
            HomeCard(
                title = "Print", subtitle = "Ball-and-stick letters", color = Wa.PrintColor,
                art = { LetterArt(printSample.strokes.map { it.points }, printSample.bounds, Wa.PrintColor, it) },
                footer = "⭐ ${progress.total(Script.PRINT, all)} / $maxStars",
                onClick = { onScript(Script.PRINT) }, modifier = Modifier.weight(1f),
            )
            HomeCard(
                title = "Cursive", subtitle = "Joined-up letters", color = Wa.CursiveColor,
                art = { LetterArt(cursiveSample.strokes.map { it.points }, cursiveSample.bounds, Wa.CursiveColor, it) },
                footer = "⭐ ${progress.total(Script.CURSIVE, all)} / $maxStars",
                onClick = { onScript(Script.CURSIVE) }, modifier = Modifier.weight(1f),
            )
            HomeCard(
                title = "Calligraphy", subtitle = "Fancy pen letters", color = Wa.CalligraphyColor,
                art = { LetterArt(calligraphySample.strokes.map { it.points }, calligraphySample.bounds, Wa.CalligraphyColor, it, nib = true) },
                footer = "⭐ ${progress.total(Script.CALLIGRAPHY, all)} / $maxStars",
                onClick = { onScript(Script.CALLIGRAPHY) }, modifier = Modifier.weight(1f),
            )
            HomeCard(
                title = "Free Write", subtitle = "Words on big or small lines", color = Wa.FreeColor,
                art = { LetterArt(freeSample.strokes.map { it.points }, freeSample.bounds, Wa.FreeColor, it) },
                footer = "📝 Practice paper",
                onClick = onFree, modifier = Modifier.weight(1f),
            )
            HomeCard(
                title = "Assignments", subtitle = "Copy words and sentences", color = Wa.Primary,
                art = { LetterArt(taskSample.strokes.map { it.points }, taskSample.bounds, Wa.Primary, it) },
                footer = "📋 ${Assignments.list.size} to do",
                onClick = onAssignments, modifier = Modifier.weight(1f),
            )
        }
        VGap(18.dp)
        Text("Before you write", style = MaterialTheme.typography.titleMedium, color = Wa.Text)
        VGap(6.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tip("🪑", "Sit up tall with your feet flat on the floor.", Modifier.weight(1f))
            Tip("✋", "Hold the tablet steady with your other hand.", Modifier.weight(1f))
            Tip("✏️", "Use one finger or a stylus. Hold it gently.", Modifier.weight(1f))
            Tip("🐢", "Go slowly. Neat beats fast!", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    color: Color,
    art: @Composable (Modifier) -> Unit,
    footer: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = BorderStroke(3.dp, color.copy(alpha = 0.6f)),
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            art(Modifier.fillMaxWidth().height(110.dp))
            VGap(6.dp)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = color)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Wa.SubText, textAlign = TextAlign.Center)
            VGap(6.dp)
            Text(footer, style = MaterialTheme.typography.labelLarge, color = Wa.Text)
        }
    }
}

@Composable
private fun Tip(emoji: String, text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = Wa.Panel) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 24.sp)
            Gap(8.dp)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Wa.Text)
        }
    }
}

@Composable
fun LettersScreen(script: Script, onBack: () -> Unit, onLetter: (Char) -> Unit, onFree: () -> Unit) {
    var upper by rememberSaveable(script) { mutableStateOf(false) }
    val letters = if (upper) Glyphs.uppercase else Glyphs.lowercase
    val color = Wa.scriptColor(script)
    val progress = LocalServices.current.progress
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("⬅️", "Back", onBack)
            Gap(12.dp)
            Column(Modifier.weight(1f)) {
                Text("${script.label} letters", style = MaterialTheme.typography.headlineMedium, color = color)
                Text(if (script == Script.CALLIGRAPHY) "Pick a letter. Picture a flat pen tilted like a slanted roof." else "Pick a letter to learn.", style = MaterialTheme.typography.bodyMedium, color = Wa.SubText)
            }
            ChoiceRow(
                options = listOf(false, true), selected = upper,
                label = { if (it) "ABC" else "abc" }, onSelect = { upper = it },
                color = color, modifier = Modifier.width(190.dp),
            )
            Gap(10.dp)
            KidButton("Free Write", onFree, emoji = "📝", color = Wa.FreeColor)
            Gap(10.dp)
            VoiceToggle()
        }
        VGap(10.dp)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(98.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(letters, key = { it }) { ch ->
                val glyph = Glyphs.require(script, ch)
                Surface(
                    onClick = { onLetter(ch) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                ) {
                    Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        LetterArt(glyph.polylines, glyph.bounds, color, Modifier.fillMaxWidth().height(84.dp), padUnits = 44f, nib = script == Script.CALLIGRAPHY)
                        StarRow(progress.stars(script, ch), 15.dp)
                    }
                }
            }
        }
    }
}

/** Big friendly pop-up over the writing area. */
@Composable
fun Celebration(
    emoji: String,
    title: String,
    message: String,
    stars: Int,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>,
    color: Color,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xAAFFF8EC))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 8.dp, border = BorderStroke(3.dp, color.copy(alpha = 0.5f))) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emoji, fontSize = 44.sp)
                Text(title, style = MaterialTheme.typography.headlineMedium, color = color)
                VGap(4.dp)
                StarRow(stars, 34.dp)
                VGap(6.dp)
                Text(message, style = MaterialTheme.typography.bodyLarge, color = Wa.Text, textAlign = TextAlign.Center)
                VGap(12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KidButton(secondary.first, secondary.second, color = color)
                    KidButton(primary.first, primary.second, color = color, filled = true)
                }
            }
        }
    }
}
