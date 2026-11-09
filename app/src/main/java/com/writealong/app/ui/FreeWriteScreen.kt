package com.writealong.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.writealong.app.data.LocalServices
import com.writealong.app.glyph.Script
import com.writealong.app.practice.PaperSize
import com.writealong.app.practice.SheetBuilder

private val starterWords = listOf("", "cat", "dog", "sun", "fish", "mom", "dad", "love", "I can write")

/** Lined paper for anything: blank, or with a word to trace on the first line. */
@Composable
fun FreeWriteScreen(startScript: Script, onBack: () -> Unit) {
    val services = LocalServices.current
    val focus = LocalFocusManager.current
    var script by rememberSaveable { mutableStateOf(startScript) }
    var size by rememberSaveable { mutableStateOf(PaperSize.BIG) }
    var word by rememberSaveable { mutableStateOf("") }
    var custom by rememberSaveable { mutableStateOf("") }
    var penIndex by rememberSaveable { mutableIntStateOf(0) }
    var clearKey by remember { mutableIntStateOf(0) }
    var board by remember { mutableStateOf<BoardState?>(null) }
    val color = Wa.FreeColor

    val banner = if (word.isBlank()) {
        "Write anything! Tall letters touch the top line. Small letters fit under the middle line."
    } else {
        "Trace \"$word\" on the dotted line. Then write it again by yourself."
    }
    LaunchedEffect(banner) { services.speaker.say(banner) }
    DisposableEffect(Unit) { onDispose { services.speaker.stop() } }

    Row(Modifier.fillMaxSize()) {
        SidePanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundButton("⬅️", "Back", onBack)
                Spacer(Modifier.weight(1f))
                VoiceToggle()
            }
            VGap(6.dp)
            Text("Free Write", style = MaterialTheme.typography.titleLarge, color = color, modifier = Modifier.padding(start = 4.dp))
            SectionLabel("Letters")
            ChoiceRow(Script.entries, script, { it.label }, { script = it }, color)
            SectionLabel("Line size")
            ChoiceRow(PaperSize.entries, size, { it.label }, { size = it }, color)
            SectionLabel("Trace a word")
            starterWords.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pair.forEach { w ->
                        KidButton(
                            text = w.ifEmpty { "Blank" },
                            onClick = { word = w; custom = "" },
                            selected = word == w && custom.isEmpty(),
                            color = color,
                            height = 38.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            OutlinedTextField(
                value = custom,
                onValueChange = { v ->
                    custom = v.filter { it in 'a'..'z' || it in 'A'..'Z' || it == ' ' }.take(30)
                    word = custom
                },
                label = { Text("Type a word") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            SectionLabel("Pencil color")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Wa.PenColors.forEachIndexed { i, c ->
                    PenDot(c, selected = i == penIndex) { penIndex = i }
                }
            }
            SectionLabel("Fix it")
            Row {
                KidButton("Undo", { board?.undo() }, emoji = "↩️", color = color, modifier = Modifier.weight(1f))
                Gap(6.dp)
                KidButton("Clear", { clearKey++ }, emoji = "🧽", color = color, modifier = Modifier.weight(1f))
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
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val w = constraints.maxWidth.toFloat()
                    val h = constraints.maxHeight.toFloat()
                    val state = rememberBoard(listOf(script, size, word, clearKey), BoardMode.FREE, null, w, h) { ww, hh, d ->
                        SheetBuilder.free(word, script, ww, hh, d, size)
                    }
                    LaunchedEffect(state, penIndex) { state.penColor = Wa.PenColors[penIndex] }
                    SideEffect { board = state }
                    WritingBoard(state)
                }
            }
        }
    }
}

@Composable
private fun PenDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color,
        border = if (selected) BorderStroke(4.dp, Color.White) else null,
        shadowElevation = if (selected) 4.dp else 1.dp,
        modifier = Modifier
            .size(if (selected) 32.dp else 28.dp)
            .semantics { contentDescription = "Pencil color" },
    ) {}
}
