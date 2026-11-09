package com.writealong.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.writealong.app.data.LocalServices
import com.writealong.app.glyph.Script
import com.writealong.app.practice.Assignments
import com.writealong.app.ui.AssignmentScreen
import com.writealong.app.ui.AssignmentsScreen
import com.writealong.app.ui.FreeWriteScreen
import com.writealong.app.ui.HomeScreen
import com.writealong.app.ui.LessonScreen
import com.writealong.app.ui.LettersScreen
import com.writealong.app.ui.Wa

sealed interface Screen {
    data object Home : Screen
    data class Letters(val script: Script) : Screen
    data class Lesson(val script: Script, val letter: Char) : Screen
    data class Free(val script: Script) : Screen
    data object AssignmentList : Screen
    data class AssignmentTask(val id: String) : Screen
}

@Composable
fun WriteAlongApp() {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    fun push(s: Screen) {
        stack.add(s)
    }
    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Wa.Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (val s = stack.last()) {
            Screen.Home -> HomeScreen(
                onScript = { push(Screen.Letters(it)) },
                onFree = { push(Screen.Free(Script.PRINT)) },
                onAssignments = { push(Screen.AssignmentList) },
            )
            is Screen.Letters -> LettersScreen(
                script = s.script,
                onBack = ::pop,
                onLetter = { push(Screen.Lesson(s.script, it)) },
                onFree = { push(Screen.Free(s.script)) },
            )
            is Screen.Lesson -> LessonScreen(
                script = s.script,
                letter = s.letter,
                onBack = ::pop,
                onGoTo = { ch -> stack[stack.lastIndex] = Screen.Lesson(s.script, ch) },
            )
            is Screen.Free -> FreeWriteScreen(startScript = s.script, onBack = ::pop)
            Screen.AssignmentList -> AssignmentsScreen(onBack = ::pop, onOpen = { push(Screen.AssignmentTask(it)) })
            is Screen.AssignmentTask -> {
                val name = LocalServices.current.progress.childName
                val assignment = remember(s.id, name) { Assignments.byId(s.id, name) }
                if (assignment == null) {
                    LaunchedEffect(s.id) { pop() }
                } else {
                    AssignmentScreen(assignment, onBack = ::pop)
                }
            }
        }
    }
}
