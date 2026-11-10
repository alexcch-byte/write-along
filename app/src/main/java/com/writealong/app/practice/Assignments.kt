package com.writealong.app.practice

import com.writealong.app.glyph.Script

/**
 * A short writing assignment: a few things to copy (letters, words or a sentence).
 * Each task shows the model dotted on the first line; the child traces it, writes it again
 * on the empty line, and taps Check.
 */
class Assignment(
    val id: String,
    val title: String,
    val emoji: String,
    val script: Script,
    val kind: Kind,
    val items: List<String>,
) {
    enum class Kind { LETTERS, WORDS, SENTENCES, NAME }

    /** What we say and show for one task. */
    fun prompt(item: String): String = when (kind) {
        Kind.LETTERS -> "Write these letters: ${item.split(" ").joinToString(", ")}."
        Kind.WORDS -> "Write the word \"$item\"."
        Kind.SENTENCES -> "Write the sentence: $item. Start with a capital letter and leave a finger space between words."
        Kind.NAME -> "Write your name: $item. Names start with a capital letter."
    }

    val kindLabel: String
        get() = when (kind) {
            Kind.LETTERS -> "Letters"
            Kind.WORDS -> "Words"
            Kind.SENTENCES -> "Sentences"
            Kind.NAME -> "Name"
        }
}

object Assignments {
    private val P = Script.PRINT
    private val C = Script.CURSIVE
    private val W = Assignment.Kind.WORDS
    private val S = Assignment.Kind.SENTENCES
    private val L = Assignment.Kind.LETTERS

    val list: List<Assignment> = listOf(
        Assignment("p-tall", "Tall letters", "🦒", P, L, listOf("l t i", "b d h", "f k l")),
        Assignment("p-round", "Round letters", "🍩", P, L, listOf("o c a", "d g q", "e s o")),
        Assignment("p-short-a", "Short a words", "🐱", P, W, listOf("cat", "hat", "map", "bag")),
        Assignment("p-short-o", "Short o words", "🐶", P, W, listOf("dog", "top", "pot", "fox")),
        Assignment("p-sight", "Sight words", "👀", P, W, listOf("the", "and", "you", "said", "was")),
        Assignment("p-names", "Capital letters", "🏷️", P, W, listOf("Sam", "Mia", "Ben", "Ava")),
        Assignment("p-sentences", "Little sentences", "✏️", P, S, listOf("I can run", "The sun is hot", "We like to play")),
        Assignment("p-blends", "Blend words", "🐸", P, W, listOf("frog", "clap", "stop", "drum", "snail", "plant")),
        Assignment("p-long", "Long vowel words", "🚲", P, W, listOf("cake", "bike", "rain", "boat", "moon", "tree")),
        Assignment("p-below", "Tails below the line", "🐛", P, W, listOf("pig", "jump", "yellow", "puppy", "queen", "giggle")),
        Assignment("p-hard", "Challenge words", "🏔️", P, W, listOf("friend", "rainbow", "pumpkin", "birthday", "elephant", "butterfly")),
        Assignment("p-hard-sentences", "Longer sentences", "📖", P, S, listOf("My friend has a yellow kitten", "The brown fox can jump high")),
        Assignment("c-pairs", "Cursive joins", "🔗", C, W, listOf("an", "in", "it", "on", "up")),
        Assignment("c-words", "Cursive words", "🌈", C, W, listOf("cat", "sun", "fish", "jump")),
        Assignment("c-sight", "Cursive sight words", "🌟", C, W, listOf("the", "and", "was", "you")),
        Assignment("c-sentences", "Cursive sentences", "💌", C, S, listOf("I love you", "Have a nice day")),
        Assignment("c-blends", "Cursive blend words", "🐢", C, W, listOf("frog", "clap", "stop", "drum", "plant")),
        Assignment("c-hard", "Cursive challenge words", "🏆", C, W, listOf("rainbow", "monkey", "yellow", "garden", "pumpkin", "butterfly")),
    )

    /** Tidies a typed name: "mia rose" -> "Mia Rose". */
    fun formatName(raw: String): String =
        raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            .joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }

    fun nameAssignments(name: String): List<Assignment> {
        val n = formatName(name)
        if (n.isEmpty()) return emptyList()
        return listOf(
            Assignment("name-print", "My name", "😊", P, Assignment.Kind.NAME, listOf(n, n)),
            Assignment("name-cursive", "My name in cursive", "🖋️", C, Assignment.Kind.NAME, listOf(n, n)),
        )
    }

    fun all(name: String): List<Assignment> = nameAssignments(name) + list

    fun byId(id: String, name: String): Assignment? = all(name).firstOrNull { it.id == id }
}
