package com.writealong.app.data

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import com.writealong.app.glyph.Script
import kotlinx.coroutines.delay
import java.util.Locale

/** Best stars earned per letter, saved on the device. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private val stars = mutableStateMapOf<String, Int>()

    var voiceOn by mutableStateOf(prefs.getBoolean(KEY_VOICE, true))
        private set

    /** The child's name, for the "My name" assignment. */
    var childName by mutableStateOf(prefs.getString(KEY_NAME, "") ?: "")
        private set

    init {
        prefs.all.forEach { (k, v) -> if ((k.startsWith("s_") || k.startsWith("a_")) && v is Int) stars[k] = v }
    }

    private fun key(script: Script, ch: Char) = "s_${script.name}_${ch.code}"

    fun stars(script: Script, ch: Char): Int = stars[key(script, ch)] ?: 0

    fun record(script: Script, ch: Char, earned: Int) {
        val k = key(script, ch)
        if (earned > (stars[k] ?: 0)) {
            stars[k] = earned
            prefs.edit { putInt(k, earned) }
        }
    }

    fun assignmentStars(id: String): Int = stars["a_$id"] ?: 0

    fun recordAssignment(id: String, earned: Int) {
        val k = "a_$id"
        if (earned > (stars[k] ?: 0)) {
            stars[k] = earned
            prefs.edit { putInt(k, earned) }
        }
    }

    fun updateName(name: String) {
        childName = name
        prefs.edit { putString(KEY_NAME, name) }
    }

    fun total(script: Script, letters: List<Char>): Int = letters.sumOf { stars(script, it) }

    fun updateVoice(on: Boolean) {
        voiceOn = on
        prefs.edit { putBoolean(KEY_VOICE, on) }
    }

    fun resetAll() {
        stars.clear()
        prefs.edit {
            prefs.all.keys.filter { it.startsWith("s_") || it.startsWith("a_") }.forEach { remove(it) }
        }
    }

    private companion object {
        const val KEY_VOICE = "voice_on"
        const val KEY_NAME = "child_name"
    }
}

/** Reads instructions out loud with the phone's built-in text-to-speech voice. */
class Speaker(context: Context, private val isEnabled: () -> Boolean) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pending: String? = null
    private var counter = 0

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val result = tts.setLanguage(Locale.US)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.getDefault())
        }
        tts.setSpeechRate(0.9f)
        tts.setPitch(1.1f)
        ready = true
        pending?.let { say(it) }
        pending = null
    }

    fun say(text: String) {
        if (!isEnabled() || text.isBlank()) return
        if (!ready) {
            pending = text
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wa-${counter++}")
    }

    fun stop() {
        pending = null
        if (ready) tts.stop()
    }

    private val speaking: Boolean get() = ready && isEnabled() && tts.isSpeaking

    /** Wait (up to [maxMs]) for the current sentence to finish. */
    suspend fun awaitDone(maxMs: Long = 9000) {
        delay(200)
        var waited = 0L
        while (speaking && waited < maxMs) {
            delay(100)
            waited += 100
        }
    }

    fun shutdown() {
        tts.shutdown()
    }
}

class AppServices(val progress: ProgressStore, val speaker: Speaker)

val LocalServices = staticCompositionLocalOf<AppServices> { error("AppServices not provided") }
