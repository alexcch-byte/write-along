package com.writealong.app.glyph

/**
 * Cursive letters in a simple classroom style. Shapes are written upright here and slanted
 * forward by [SLANT] when drawn. Lowercase letters start on the bottom line with a lead-in
 * stroke and finish with an exit stroke so they can join into words.
 *
 * Teaching words used in the instructions:
 *  undercurve  – swing up from below, like the inside of a bowl
 *  overcurve   – swing up and over, like a rainbow
 *  downcurve   – curve back down and around, like the start of a "c"
 *  checkstroke – a tiny hook out to the right at the middle line
 */
object CursiveGlyphs {

    const val SLANT = 0.18f

    private fun lc(id: Char, exitStroke: Int = 0, block: GlyphBuilder.() -> Unit) =
        glyph(id, Script.CURSIVE, SLANT, leadIn = true, exitStroke = exitStroke, block = block)

    private fun uc(id: Char, joins: Boolean = true, exitStroke: Int = 0, block: GlyphBuilder.() -> Unit) =
        glyph(id, Script.CURSIVE, SLANT, leadIn = false, exitStroke = if (joins) exitStroke else null, block = block)

    /** Tall loop used by b, f, h, k, l: undercurve to the top line, loop back, start down. */
    private fun StrokeBuilder.tallLoop() {
        m(0, 100); c(12, 100, 22, 70, 27, 30); c(30, 8, 22, -1, 18, 8)
    }

    /** "a"-style oval: undercurve to the middle line, then downcurve all the way around. */
    private fun StrokeBuilder.oval() {
        m(0, 100); c(12, 100, 22, 58, 34.1, 50.9); arc(30, 75, 16, 25, -75, -285)
    }

    /** Short exit undercurve from the bottom of a downstroke at x. */
    private fun StrokeBuilder.exitFrom(x: Float) {
        c(x + 2, 100, x + 12, 100, x + 22, 86)
    }

    private const val DOT_SAY = "Lift your pencil and add a dot."

    val lowercase: List<Glyph> = listOf(
        lc('a') {
            stroke("Undercurve up to the middle line, downcurve back around, push up, slide down, and undercurve out.") {
                oval(); l(46, 50); l(46, 92); exitFrom(46f)
            }
        },
        lc('b') {
            stroke("Undercurve up to the top line, loop back, slant down, undercurve up, and checkstroke out.") {
                tallLoop(); c(16, 16, 20, 60, 20, 92); c(22, 101, 36, 102, 40, 84); c(43, 70, 42, 56, 37, 51)
                c(33, 48, 31, 54, 37, 55); c(44, 56, 50, 52, 58, 48)
            }
        },
        lc('c') {
            stroke("Undercurve up to the middle line, downcurve around, and undercurve out.") {
                m(0, 100); c(8, 100, 12, 62, 24, 52); c(30, 48, 38, 49, 37, 54); c(36, 57, 30, 55, 26, 52)
                c(18, 50, 10, 66, 13, 82); c(16, 98, 30, 101, 40, 98); c(48, 96, 54, 92, 60, 86)
            }
        },
        lc('d') {
            stroke("Undercurve, downcurve back around, push up to the top line, slide down, and undercurve out.") {
                oval(); l(46, 0); l(46, 92); exitFrom(46f)
            }
        },
        lc('e') {
            stroke("Undercurve up, loop back around, and undercurve out.") {
                m(0, 100); c(14, 100, 30, 86, 34, 70); c(37, 56, 30, 48, 24, 52); c(16, 57, 14, 76, 16, 88)
                c(18, 98, 24, 100, 32, 100); c(42, 100, 50, 95, 58, 86)
            }
        },
        lc('f') {
            stroke("Undercurve up to the top line, loop back, slant down below the bottom line, loop forward, and undercurve out.") {
                tallLoop(); c(16, 16, 20, 60, 20, 138); c(20, 152, 34, 150, 32, 134); c(30, 120, 24, 106, 20, 100)
                c(28, 100, 38, 98, 48, 86)
            }
        },
        lc('g') {
            stroke("Undercurve, downcurve around, push up, slide down below the bottom line, loop back, and overcurve out.") {
                oval(); l(46, 50); l(46, 134); c(46, 154, 26, 154, 26, 138); c(26, 122, 46, 108, 62, 90)
            }
        },
        lc('h') {
            stroke("Undercurve up to the top line, loop back, slant down, overcurve, slant down, and undercurve out.") {
                tallLoop(); c(16, 16, 20, 60, 20, 100); l(20, 74); c(24, 46, 44, 44, 44, 70); l(44, 92); exitFrom(44f)
            }
        },
        lc('i') {
            stroke("Undercurve up to the middle line, slant down, and undercurve out.") {
                m(0, 100); c(10, 100, 18, 72, 20, 50); l(19, 92); exitFrom(19f)
            }
            dotStroke(20.5, 30, DOT_SAY)
        },
        lc('j') {
            stroke("Undercurve up to the middle line, slant down below the bottom line, loop back, and overcurve out.") {
                m(0, 100); c(10, 100, 18, 72, 20, 50); l(20, 136); c(20, 154, 4, 154, 4, 140); c(4, 124, 24, 108, 40, 90)
            }
            dotStroke(20.5, 30, DOT_SAY)
        },
        lc('k') {
            stroke("Undercurve up, loop back, slant down, overcurve into a small loop, slant down, and undercurve out.") {
                tallLoop(); c(16, 16, 20, 60, 20, 100); l(20, 74); c(24, 48, 46, 48, 42, 64); c(40, 72, 30, 74, 22, 72)
                c(32, 76, 36, 90, 38, 94); c(40, 100, 50, 100, 60, 86)
            }
        },
        lc('l') {
            stroke("Undercurve up to the top line, loop back, slant down, and undercurve out.") {
                tallLoop(); c(16, 16, 19, 70, 20, 92); exitFrom(20f)
            }
        },
        lc('m') {
            stroke("Overcurve, slant down, overcurve, slant down, overcurve, slant down, and undercurve out.") {
                m(0, 100); c(3, 70, 8, 50, 14, 50); c(18, 50, 18, 62, 18, 100)
                c(18, 70, 22, 50, 30, 50); c(36, 50, 36, 62, 36, 100)
                c(36, 70, 40, 50, 48, 50); c(54, 50, 54, 62, 54, 92); exitFrom(54f)
            }
        },
        lc('n') {
            stroke("Overcurve, slant down, overcurve, slant down, and undercurve out.") {
                m(0, 100); c(3, 70, 8, 50, 14, 50); c(18, 50, 18, 62, 18, 100)
                c(18, 70, 22, 50, 30, 50); c(36, 50, 36, 62, 36, 92); exitFrom(36f)
            }
        },
        lc('o') {
            stroke("Undercurve up, downcurve all the way around, and checkstroke out at the middle line.") {
                m(0, 100); c(12, 100, 22, 58, 29.3, 50.1); arc(28, 75, 15, 25, -85, -360); c(34, 49, 42, 49, 50, 48)
            }
        },
        lc('p') {
            stroke("Undercurve up, slant down below the bottom line, push up, overcurve around, and undercurve out.") {
                m(0, 100); c(10, 100, 18, 72, 20, 48); l(18, 150); l(18, 74)
                c(22, 50, 46, 50, 44, 74); c(42, 98, 26, 102, 18, 96); c(28, 100, 40, 100, 50, 86)
            }
        },
        lc('q') {
            stroke("Undercurve, downcurve around, push up, slide down below the bottom line, loop forward, and undercurve out.") {
                oval(); l(46, 50); l(46, 136); c(46, 152, 60, 152, 60, 138); c(60, 124, 52, 108, 46, 100)
                c(52, 104, 62, 98, 68, 88)
            }
        },
        lc('r') {
            stroke("Undercurve up past the middle line, checkstroke, slant down, and undercurve out.") {
                m(0, 100); c(10, 100, 18, 70, 20, 46); c(24, 47, 28, 49, 32, 52); c(32, 64, 31, 84, 31, 92); exitFrom(31f)
            }
        },
        lc('s') {
            stroke("Undercurve up past the middle line, curve down and around, close it, and undercurve out.") {
                m(0, 100); c(10, 100, 18, 68, 22, 46); c(34, 62, 38, 82, 28, 96); c(22, 102, 14, 100, 14, 94)
                c(18, 100, 30, 100, 42, 88)
            }
        },
        lc('t') {
            stroke("Undercurve up halfway to the top line, slant down, and undercurve out.") {
                m(0, 100); c(10, 100, 18, 62, 21, 20); l(19, 92); exitFrom(19f)
            }
            stroke("Lift your pencil. Slide right to cross the t.") { m(6, 46); l(36, 46) }
        },
        lc('u') {
            stroke("Undercurve up, slant down, undercurve up, slant down, and undercurve out.") {
                m(0, 100); c(8, 100, 15, 72, 17, 50); c(16, 70, 16, 90, 22, 98); c(30, 104, 38, 80, 39, 50)
                c(38, 70, 38, 90, 40, 94); exitFrom(40f)
            }
        },
        lc('v') {
            stroke("Overcurve, slant down, undercurve up, and checkstroke out.") {
                m(0, 100); c(3, 70, 8, 50, 14, 50); c(18, 50, 20, 80, 24, 98); c(30, 104, 40, 78, 42, 52)
                c(46, 50, 52, 50, 58, 48)
            }
        },
        lc('w') {
            stroke("Undercurve, slant down, undercurve, slant down, undercurve, and checkstroke out.") {
                m(0, 100); c(8, 100, 15, 72, 17, 50); c(16, 70, 16, 90, 22, 98); c(30, 104, 36, 80, 37, 52)
                c(36, 72, 36, 90, 42, 98); c(50, 104, 58, 78, 60, 52); c(64, 50, 70, 50, 76, 48)
            }
        },
        lc('x') {
            stroke("Overcurve, slant down, and undercurve out.") {
                m(0, 100); c(3, 70, 8, 50, 14, 50); c(20, 50, 24, 80, 28, 94); c(30, 100, 38, 100, 48, 86)
            }
            stroke("Lift your pencil. Slant down across to make the x.") { m(36, 50); l(12, 100) }
        },
        lc('y') {
            stroke("Overcurve, slant down, undercurve up, slant down below the bottom line, loop back, and overcurve out.") {
                m(0, 100); c(3, 70, 8, 50, 14, 50); c(18, 50, 18, 80, 22, 96); c(30, 104, 38, 78, 40, 50)
                l(38, 136); c(37, 154, 20, 154, 20, 140); c(20, 124, 40, 108, 56, 90)
            }
        },
        lc('z') {
            stroke("Overcurve, curve down to the bottom line, curve forward below the line, loop back, and overcurve out.") {
                m(0, 100); c(3, 70, 8, 50, 14, 50); c(30, 50, 34, 62, 18, 98); c(36, 96, 40, 112, 34, 132)
                c(30, 152, 12, 152, 14, 138); c(16, 124, 34, 110, 50, 92)
            }
        },
    )

    val uppercase: List<Glyph> = listOf(
        uc('A') {
            stroke("Downcurve all the way around, push up to the top line, slant down, and undercurve out.") {
                arc(40, 52, 26, 48, -70, -290); l(66, 4); l(66, 92); exitFrom(66f)
            }
        },
        uc('B', joins = false) {
            stroke("Slant down, retrace up, curve forward to the middle, then curve forward again to the bottom line.") {
                m(10, 4); c(10, 40, 8, 80, 6, 100); c(8, 80, 10, 40, 10, 4); c(54, -6, 58, 40, 16, 50)
                c(66, 52, 64, 104, 14, 100)
            }
        },
        uc('C') {
            stroke("Downcurve around and undercurve out.") {
                m(70, 16); c(62, 0, 28, -4, 14, 24); c(2, 48, 6, 90, 30, 99); c(46, 104, 64, 96, 74, 86)
            }
        },
        uc('D', joins = false) {
            stroke("Slant down, make a small loop, then curve up and around to the top.") {
                m(18, 4); c(16, 40, 14, 80, 10, 100); c(4, 108, -4, 96, 10, 94); c(42, 94, 74, 80, 72, 46)
                c(70, 10, 40, -2, 12, 8)
            }
        },
        uc('E') {
            stroke("Downcurve to the middle, make a small loop, downcurve again, and undercurve out.") {
                m(56, 14); c(48, 0, 18, 0, 18, 22); c(18, 40, 36, 48, 40, 50); c(28, 48, 6, 58, 8, 78)
                c(10, 98, 30, 104, 50, 96); c(56, 94, 62, 90, 68, 86)
            }
        },
        uc('F', joins = false) {
            stroke("Make a wavy line across the top.") { m(10, 8); c(26, 0, 48, 12, 72, 4) }
            stroke("Lift your pencil. Slant down and curve back.") {
                m(44, 6); c(42, 40, 40, 80, 30, 96); c(24, 104, 8, 100, 6, 90)
            }
            stroke("Lift your pencil. Slide right across the middle.") { m(26, 54); l(54, 54) }
        },
        uc('G') {
            stroke("Downcurve around, push up to the middle line, slant down below the bottom line, loop back, and overcurve out.") {
                m(64, 14); c(56, -2, 18, -2, 10, 40); c(4, 82, 36, 106, 56, 88); c(60, 84, 62, 70, 60, 52)
                l(58, 132); c(56, 154, 34, 152, 38, 136); c(42, 120, 60, 106, 76, 92)
            }
        },
        uc('H', exitStroke = 1) {
            stroke("Curve up, then slant down to the bottom line.") {
                m(4, 12); c(10, 0, 20, 0, 18, 16); c(16, 44, 14, 80, 10, 100)
            }
            stroke("Lift your pencil. Slant down and undercurve out.") {
                m(58, 2); c(56, 40, 54, 80, 54, 92); exitFrom(54f)
            }
            stroke("Lift your pencil. Slide across the middle.") { m(14, 52); c(26, 46, 42, 56, 56, 50) }
        },
        uc('I', joins = false) {
            stroke("Curve up to the top line, slant down, and curve back.") {
                m(4, 52); c(10, 20, 40, -6, 52, 4); c(46, 30, 40, 80, 30, 96); c(24, 104, 8, 102, 6, 90)
            }
        },
        uc('J') {
            stroke("Curve up to the top line, slant down below the bottom line, loop back, and overcurve out.") {
                m(10, 44); c(16, 14, 44, -6, 46, 8); c(46, 50, 40, 110, 32, 138); c(26, 156, 8, 152, 12, 136)
                c(16, 120, 34, 106, 50, 92)
            }
        },
        uc('K', exitStroke = 1) {
            stroke("Curve up, then slant down to the bottom line.") {
                m(4, 12); c(10, 0, 20, 0, 18, 16); c(16, 44, 14, 80, 10, 100)
            }
            stroke("Lift your pencil. Slant in to the middle, then slant down and undercurve out.") {
                m(60, 4); c(44, 20, 28, 44, 16, 56); c(30, 52, 38, 64, 40, 86); c(42, 100, 52, 100, 62, 86)
            }
        },
        uc('L') {
            stroke("Curve up into a loop at the top, slant down, make a small loop on the bottom line, and undercurve out.") {
                m(20, 44); c(10, 30, 30, -4, 48, 4); c(56, 8, 48, 22, 40, 30); c(30, 50, 26, 80, 16, 96)
                c(10, 104, 0, 100, 4, 94); c(10, 88, 22, 100, 38, 100); c(52, 100, 62, 96, 72, 88)
            }
        },
        uc('M') {
            stroke("Overcurve, slant down, overcurve, slant down, overcurve, slant down, and undercurve out.") {
                m(0, 24); c(6, 4, 14, 0, 18, 6); c(22, 14, 20, 60, 16, 100)
                c(22, 40, 30, 0, 38, 4); c(46, 8, 42, 60, 40, 100)
                c(46, 40, 54, 0, 62, 4); c(68, 8, 66, 60, 66, 92); exitFrom(66f)
            }
        },
        uc('N') {
            stroke("Overcurve, slant down, overcurve, slant down, and undercurve out.") {
                m(0, 24); c(6, 4, 14, 0, 18, 6); c(22, 14, 20, 60, 16, 100)
                c(22, 40, 30, 0, 38, 4); c(46, 8, 44, 60, 44, 92); exitFrom(44f)
            }
        },
        uc('O', joins = false) {
            stroke("Downcurve all the way around and make a small loop at the top.") {
                arc(36, 50, 30, 48, -80, -360); c(46, 0, 58, 2, 68, 6)
            }
        },
        uc('P', joins = false) {
            stroke("Slant down, retrace up, then curve forward and back to the middle.") {
                m(12, 4); c(12, 40, 10, 80, 8, 100); c(10, 80, 12, 40, 12, 4); c(56, -4, 62, 44, 14, 54)
            }
        },
        uc('Q') {
            stroke("Curve up and over, slant down to the bottom line, make a small loop, and undercurve out.") {
                m(10, 26); c(12, 0, 58, -6, 54, 30); c(50, 62, 24, 88, 6, 98); c(0, 102, 8, 90, 20, 92)
                c(34, 94, 48, 104, 64, 90)
            }
        },
        uc('R') {
            stroke("Slant down, retrace up, curve forward and back, slant down, and undercurve out.") {
                m(12, 4); c(12, 40, 10, 80, 8, 100); c(10, 80, 12, 40, 12, 4); c(56, -4, 62, 44, 18, 52)
                c(32, 56, 38, 78, 40, 92); exitFrom(40f)
            }
        },
        uc('S', joins = false) {
            stroke("Undercurve up to the top line, curve down and around, and curve back.") {
                m(0, 100); c(16, 92, 40, 40, 50, 2); c(32, 28, 62, 54, 58, 78); c(54, 104, 16, 104, 8, 88)
            }
        },
        uc('T', joins = false) {
            stroke("Make a wavy line across the top.") { m(4, 10); c(24, 0, 48, 12, 76, 2) }
            stroke("Lift your pencil. Slant down and curve back.") {
                m(44, 4); c(42, 40, 40, 80, 32, 96); c(26, 104, 10, 100, 8, 90)
            }
        },
        uc('U') {
            stroke("Curve up, slant down, undercurve up to the top line, slant down, and undercurve out.") {
                m(4, 12); c(10, 0, 20, 0, 18, 16); c(16, 50, 14, 92, 28, 98); c(42, 104, 52, 80, 54, 4)
                c(54, 40, 52, 80, 54, 92); exitFrom(54f)
            }
        },
        uc('V', joins = false) {
            stroke("Curve up, slant down to the bottom line, undercurve up to the top line, and checkstroke.") {
                m(4, 12); c(10, 0, 20, 0, 20, 16); c(24, 50, 30, 90, 36, 100); c(44, 70, 54, 20, 66, 4)
                c(70, 0, 76, 2, 82, 6)
            }
        },
        uc('W', joins = false) {
            stroke("Curve up, slant down, undercurve up, slant down, undercurve up, and checkstroke.") {
                m(2, 12); c(8, 0, 16, 0, 16, 16); c(16, 50, 18, 90, 24, 100); c(30, 70, 36, 30, 42, 6)
                c(44, 40, 50, 90, 58, 100); c(64, 70, 72, 30, 82, 4); c(86, 2, 92, 4, 96, 8)
            }
        },
        uc('X', joins = false) {
            stroke("Curve up and over, slant down, and undercurve out.") {
                m(4, 10); c(14, 0, 24, 4, 30, 30); c(36, 60, 40, 96, 56, 98); c(62, 98, 68, 94, 74, 88)
            }
            stroke("Lift your pencil. Slant down across to make the X.") { m(58, 4); l(8, 98) }
        },
        uc('Y') {
            stroke("Curve up, slant down, undercurve up, slant down below the bottom line, loop back, and overcurve out.") {
                m(4, 12); c(10, 0, 20, 0, 18, 16); c(16, 50, 14, 92, 28, 98); c(42, 104, 52, 80, 54, 4)
                c(54, 60, 50, 110, 46, 136); c(42, 156, 22, 152, 26, 136); c(30, 120, 50, 106, 66, 92)
            }
        },
        uc('Z') {
            stroke("Curve over the top, slant down to the middle, curve down below the bottom line, loop back, and overcurve out.") {
                m(6, 14); c(20, -2, 58, -4, 50, 20); c(44, 40, 30, 52, 22, 56); c(48, 56, 54, 80, 46, 110)
                c(40, 150, 14, 152, 18, 134); c(22, 118, 44, 104, 62, 92)
            }
        },
    )
}
