package com.writealong.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.writealong.app.data.LocalServices
import com.writealong.app.glyph.Script
import com.writealong.app.glyph.TextLayout
import com.writealong.app.practice.Assignment
import com.writealong.app.practice.Assignments
import com.writealong.app.practice.PaperSize
import com.writealong.app.practice.SheetBuilder
import kotlin.math.max
import kotlin.math.roundToInt

/** The list of assignments, plus a box for the child's name. */
@Composable
fun AssignmentsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val progress = LocalServices.current.progress
    val focus = LocalFocusManager.current
    val name = progress.childName
    val assignments = remember(name) { Assignments.all(name) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("⬅️", "Back", onBack)
            Gap(12.dp)
            Column(Modifier.weight(1f)) {
                Text("Writing Assignments", style = MaterialTheme.typography.headlineMedium, color = Wa.Primary)
                Text(
                    "Copy the dotted writing, then write it again on your own and tap Check.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Wa.SubText,
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = { v -> progress.updateName(v.filter { it in 'a'..'z' || it in 'A'..'Z' || it == ' ' }.take(20)) },
                label = { Text("Your first name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier = Modifier.width(240.dp),
            )
            Gap(10.dp)
            VoiceToggle()
        }
        VGap(10.dp)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(250.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(assignments, key = { it.id }) { a ->
                AssignmentCard(a, progress.assignmentStars(a.id)) { onOpen(a.id) }
            }
        }
    }
}

@Composable
private fun AssignmentCard(a: Assignment, stars: Int, onClick: () -> Unit) {
    val color = Wa.scriptColor(a.script)
    val sample = remember(a.id, a.items) { TextLayout.layout(a.items.first(), a.script) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(2.dp, color.copy(alpha = 0.45f)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.emoji, fontSize = 30.sp)
                Gap(10.dp)
                Column(Modifier.weight(1f)) {
                    Text(a.title, style = MaterialTheme.typography.titleMedium, color = Wa.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${a.script.label} · ${a.kindLabel} · ${a.items.size} tasks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                }
                StarRow(stars, 16.dp)
            }
            LetterArt(sample.strokes.map { it.points }, sample.bounds, color, Modifier.fillMaxWidth().height(64.dp), padUnits = 40f, nib = a.script == Script.CALLIGRAPHY)
        }
    }
}

/** Work through one assignment, task by task. */
@Composable
fun AssignmentScreen(assignment: Assignment, onBack: () -> Unit) {
    val services = LocalServices.current
    val color = Wa.scriptColor(assignment.script)
    val count = assignment.items.size

    var itemIdx by rememberSaveable(assignment.id) { mutableIntStateOf(0) }
    var size by rememberSaveable { mutableStateOf(PaperSize.BIG) }
    var runKey by remember(assignment.id, itemIdx, size) { mutableIntStateOf(0) }
    val scores = remember(assignment.id) { mutableStateMapOf<Int, Int>() }
    var checked by remember(assignment.id, itemIdx, size, runKey) { mutableStateOf(false) }
    var finished by remember(assignment.id) { mutableStateOf<Int?>(null) }
    var board by remember { mutableStateOf<BoardState?>(null) }

    val item = assignment.items[itemIdx.coerceIn(0, count - 1)]
    val prompt = assignment.prompt(item)
    val intro = "$prompt Trace the dotted line first, then write it again on the empty line."
    var banner by remember(assignment.id, itemIdx) { mutableStateOf(intro) }
    val preview = remember(item, assignment.script) { TextLayout.layout(item, assignment.script) }

    DisposableEffect(Unit) { onDispose { services.speaker.stop() } }

    fun next() {
        if (itemIdx < count - 1) {
            itemIdx++
        } else {
            val avg = scores.values.average().roundToInt().coerceIn(0, 3)
            services.progress.recordAssignment(assignment.id, avg)
            finished = avg
            services.speaker.say(if (avg >= 2) "You finished the assignment! Great work!" else "You finished the assignment! Keep practicing!")
        }
    }

    Row(Modifier.fillMaxSize()) {
        SidePanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundButton("⬅️", "Back to assignments", onBack)
                Spacer(Modifier.weight(1f))
                VoiceToggle()
            }
            VGap(6.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(assignment.emoji, fontSize = 24.sp)
                Gap(6.dp)
                Text(assignment.title, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 2)
            }
            VGap(6.dp)
            Text("Task ${itemIdx + 1} of $count", style = MaterialTheme.typography.labelLarge, color = Wa.Text, modifier = Modifier.padding(start = 4.dp))
            VGap(4.dp)
            Row(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until count) {
                    val done = scores.containsKey(i)
                    Box(
                        Modifier
                            .size(if (i == itemIdx) 16.dp else 12.dp)
                            .background(
                                when {
                                    done -> Wa.Good
                                    i == itemIdx -> color
                                    else -> Wa.StarEmpty
                                },
                                CircleShape,
                            ),
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                LetterArt(preview.strokes.map { it.points }, preview.bounds, color, Modifier.fillMaxWidth().height(64.dp), padUnits = 24f, nib = assignment.script == Script.CALLIGRAPHY)
            }
            SectionLabel("Line size")
            ChoiceRow(PaperSize.entries, size, { it.label }, { size = it }, color)
            SectionLabel("When you're done")
            KidButton("Check", { board?.check() }, emoji = "✅", color = Wa.Good, filled = true, modifier = Modifier.fillMaxWidth())
            VGap(6.dp)
            Row {
                KidButton("Undo", { board?.undo() }, emoji = "↩️", color = color, modifier = Modifier.weight(1f))
                Gap(6.dp)
                KidButton("Clear", { runKey++ }, emoji = "🧽", color = color, modifier = Modifier.weight(1f))
            }
            VGap(6.dp)
            KidButton(
                text = if (itemIdx < count - 1) "Next task" else "Finish",
                onClick = ::next,
                emoji = if (itemIdx < count - 1) "▶️" else "🏁",
                color = color,
                filled = checked,
                enabled = checked,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!checked) {
                Text(
                    "Tap Check to unlock the next task.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Wa.SubText,
                    modifier = Modifier.padding(4.dp),
                )
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
                    val state = rememberBoard(
                        listOf(assignment.id, itemIdx, size, runKey), BoardMode.WRITE, null, w, h, words = true,
                    ) { ww, hh, d -> SheetBuilder.assignment(item, assignment.script, ww, hh, d, size) }
                    state.intro = intro
                    state.onMessage = { text, speak ->
                        banner = text
                        if (speak) services.speaker.say(text)
                    }
                    state.onFinished = { stars ->
                        scores[itemIdx] = max(scores[itemIdx] ?: 0, stars)
                        checked = true
                    }
                    SideEffect { board = state }
                    LaunchedEffect(state) { state.begin() }
                    WritingBoard(state)
                }

                finished?.let { stars ->
                    Celebration(
                        emoji = if (stars >= 2) "🏆" else "🎈",
                        title = "Assignment done!",
                        message = if (stars >= 2) "Wonderful writing. Pick another assignment!" else "Good effort! Try it again for more stars.",
                        stars = stars,
                        primary = "Done" to onBack,
                        secondary = "🔁 Do it again" to {
                            scores.clear()
                            itemIdx = 0
                            finished = null
                        },
                        color = color,
                    )
                }
            }
        }
    }
}
