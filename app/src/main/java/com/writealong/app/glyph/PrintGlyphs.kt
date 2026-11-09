package com.writealong.app.glyph

/**
 * Manuscript ("ball and stick") print letters, in the stroke order and direction
 * commonly taught in US elementary classrooms: tall sticks pull down from the top,
 * circles go "back" (counter-clockwise), and the pencil lifts between strokes.
 */
object PrintGlyphs {

    private fun g(id: Char, block: GlyphBuilder.() -> Unit) = glyph(id, Script.PRINT, block = block)

    private const val DOWN = "Pull down straight to the bottom line."

    val lowercase: List<Glyph> = listOf(
        g('a') {
            stroke("Circle back all the way around, push up to the middle line, then pull down straight to the bottom line.") {
                circle(25, 75, 25, -40, -320); l(50, 50); l(50, 100)
            }
        },
        g('b') {
            stroke("Pull down straight to the bottom line, push up, and circle forward.") {
                m(0, 0); l(0, 100); l(0, 75); circle(25, 75, 25, 180, 360)
            }
        },
        g('c') {
            stroke("Circle back and stop.") { circle(25, 75, 25, -45, -270) }
        },
        g('d') {
            stroke("Circle back all the way around, push up to the top line, then pull down straight to the bottom line.") {
                circle(25, 75, 25, -40, -320); l(50, 0); l(50, 100)
            }
        },
        g('e') {
            stroke("Slide right, then circle back and stop.") {
                m(0, 75); l(50, 75); circle(25, 75, 25, 0, -315)
            }
        },
        g('f') {
            stroke("Curve back, then pull down straight to the bottom line.") {
                m(42, 10); c(36, 1, 20, 1, 20, 18); l(20, 100)
            }
            stroke("Lift your pencil. Slide right across the middle line.") { m(4, 50); l(38, 50) }
        },
        g('g') {
            stroke("Circle back all the way around, push up, pull down below the bottom line, and curve back.") {
                circle(25, 75, 25, -40, -320); l(50, 50); l(50, 128); c(50, 150, 16, 154, 4, 138)
            }
        },
        g('h') {
            stroke("Pull down straight to the bottom line, push up, curve forward, and pull down straight.") {
                m(0, 0); l(0, 100); l(0, 72); circle(22, 72, 22, 180, 180); l(44, 100)
            }
        },
        g('i') {
            stroke(DOWN) { m(0, 50); l(0, 100) }
            dotStroke(0, 30)
        },
        g('j') {
            stroke("Pull down straight below the bottom line, then curve back.") {
                m(25, 50); l(25, 130); c(25, 152, 2, 154, -6, 138)
            }
            dotStroke(25, 30)
        },
        g('k') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Slant left to the stick, then slant right to the bottom line.") {
                m(40, 50); l(2, 78); l(42, 100)
            }
        },
        g('l') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
        },
        g('m') {
            stroke("Pull down straight, push up, curve forward and pull down, push up, curve forward and pull down.") {
                m(0, 50); l(0, 100); l(0, 65); circle(15, 65, 15, 180, 180); l(30, 100)
                l(30, 65); circle(45, 65, 15, 180, 180); l(60, 100)
            }
        },
        g('n') {
            stroke("Pull down straight, push up, curve forward, and pull down straight.") {
                m(0, 50); l(0, 100); l(0, 72); circle(22, 72, 22, 180, 180); l(44, 100)
            }
        },
        g('o') {
            stroke("Circle back all the way around.") { circle(25, 75, 25, -90, -360) }
        },
        g('p') {
            stroke("Pull down straight below the bottom line, push up, and circle forward.") {
                m(0, 50); l(0, 150); l(0, 75); circle(25, 75, 25, 180, 360)
            }
        },
        g('q') {
            stroke("Circle back all the way around, push up, then pull down straight below the bottom line.") {
                circle(25, 75, 25, -40, -320); l(50, 50); l(50, 150)
            }
        },
        g('r') {
            stroke("Pull down straight, push up, and curve forward.") {
                m(0, 50); l(0, 100); l(0, 72); circle(22, 72, 22, 180, 130)
            }
        },
        g('s') {
            stroke("Curve back, then curve forward.") {
                arc(25, 62.5, 18, 12.5, -20, -250); arc(25, 87.5, 20, 12.5, -90, 250)
            }
        },
        g('t') {
            stroke(DOWN) { m(20, 22); l(20, 100) }
            stroke("Lift your pencil. Slide right across the middle line.") { m(4, 50); l(36, 50) }
        },
        g('u') {
            stroke("Pull down, curve forward, push up to the middle line, then pull down straight.") {
                m(0, 50); l(0, 75); circle(25, 75, 25, 180, -180); l(50, 50); l(50, 100)
            }
        },
        g('v') {
            stroke("Slant right down to the bottom line, then slant up to the middle line.") {
                m(0, 50); l(25, 100); l(50, 50)
            }
        },
        g('w') {
            stroke("Slant right, slant up, slant right, slant up.") {
                m(0, 50); l(17, 100); l(35, 50); l(53, 100); l(70, 50)
            }
        },
        g('x') {
            stroke("Slant right to the bottom line.") { m(0, 50); l(45, 100) }
            stroke("Lift your pencil. Slant left to the bottom line.") { m(45, 50); l(0, 100) }
        },
        g('y') {
            stroke("Slant right to the bottom line.") { m(0, 50); l(33, 100) }
            stroke("Lift your pencil. Slant left all the way below the bottom line.") { m(55, 50); l(12, 150) }
        },
        g('z') {
            stroke("Slide right, slant left to the bottom line, then slide right.") {
                m(0, 50); l(45, 50); l(0, 100); l(45, 100)
            }
        },
    )

    val uppercase: List<Glyph> = listOf(
        g('A') {
            stroke("Slant left to the bottom line.") { m(40, 0); l(0, 100) }
            stroke("Lift your pencil. Go back to the top and slant right.") { m(40, 0); l(80, 100) }
            stroke("Lift your pencil. Slide right across the middle.") { m(16, 62); l(64, 62) }
        },
        g('B') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Slide right, curve forward to the middle, slide left. Slide right, curve forward to the bottom, slide left.") {
                m(0, 0); l(30, 0); arc(30, 25, 24, 25, -90, 180); l(0, 50)
                l(34, 50); arc(34, 75, 26, 25, -90, 180); l(0, 100)
            }
        },
        g('C') {
            stroke("Circle back and stop.") { circle(50, 50, 50, -45, -270) }
        },
        g('D') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Slide right, curve forward all the way down, and slide left.") {
                m(0, 0); l(25, 0); arc(25, 50, 45, 50, -90, 180); l(0, 100)
            }
        },
        g('E') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Slide right on the top line.") { m(0, 0); l(55, 0) }
            stroke("Lift your pencil. Slide right on the middle line.") { m(0, 50); l(45, 50) }
            stroke("Lift your pencil. Slide right on the bottom line.") { m(0, 100); l(55, 100) }
        },
        g('F') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Slide right on the top line.") { m(0, 0); l(55, 0) }
            stroke("Lift your pencil. Slide right on the middle line.") { m(0, 50); l(45, 50) }
        },
        g('G') {
            stroke("Circle back, then slide left at the middle.") {
                circle(50, 50, 50, -45, -315); l(55, 50)
            }
        },
        g('H') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Pull down straight on the other side.") { m(65, 0); l(65, 100) }
            stroke("Lift your pencil. Slide right across the middle.") { m(0, 50); l(65, 50) }
        },
        g('I') {
            stroke(DOWN) { m(25, 0); l(25, 100) }
            stroke("Lift your pencil. Slide right across the top.") { m(0, 0); l(50, 0) }
            stroke("Lift your pencil. Slide right across the bottom.") { m(0, 100); l(50, 100) }
        },
        g('J') {
            stroke("Pull down straight, then curve back.") {
                m(45, 0); l(45, 72); arc(22.5, 72, 22.5, 28, 0, 180)
            }
        },
        g('K') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Slant left to the middle of the stick, then slant right to the bottom line.") {
                m(60, 0); l(2, 52); l(62, 100)
            }
        },
        g('L') {
            stroke("Pull down straight, then slide right on the bottom line.") { m(0, 0); l(0, 100); l(50, 100) }
        },
        g('M') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Slant right down to the bottom line, slant up, and pull down straight.") {
                m(0, 0); l(40, 100); l(80, 0); l(80, 100)
            }
        },
        g('N') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Slant right to the bottom line, then push up straight.") {
                m(0, 0); l(65, 100); l(65, 0)
            }
        },
        g('O') {
            stroke("Circle back all the way around.") { circle(50, 50, 50, -90, -360) }
        },
        g('P') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Slide right, curve forward, and slide left to the middle.") {
                m(0, 0); l(30, 0); arc(30, 25, 25, 25, -90, 180); l(0, 50)
            }
        },
        g('Q') {
            stroke("Circle back all the way around.") { circle(50, 50, 50, -90, -360) }
            stroke("Lift your pencil. Slant right across the bottom.") { m(58, 72); l(98, 106) }
        },
        g('R') {
            stroke(DOWN) { m(0, 0); l(0, 100) }
            stroke("Lift your pencil. Slide right, curve forward, slide left, then slant right to the bottom line.") {
                m(0, 0); l(30, 0); arc(30, 25, 25, 25, -90, 180); l(12, 50); l(58, 100)
            }
        },
        g('S') {
            stroke("Curve back, then curve forward.") {
                arc(38, 25, 30, 25, -25, -245); arc(38, 75, 34, 25, -90, 250)
            }
        },
        g('T') {
            stroke(DOWN) { m(35, 0); l(35, 100) }
            stroke("Lift your pencil. Slide right across the top.") { m(0, 0); l(70, 0) }
        },
        g('U') {
            stroke("Pull down, curve forward, and push up to the top line.") {
                m(0, 0); l(0, 65); arc(35, 65, 35, 35, 180, -180); l(70, 0)
            }
        },
        g('V') {
            stroke("Slant right to the bottom line, then slant up to the top line.") { m(0, 0); l(40, 100); l(80, 0) }
        },
        g('W') {
            stroke("Slant right, slant up, slant right, slant up.") {
                m(0, 0); l(25, 100); l(50, 0); l(75, 100); l(100, 0)
            }
        },
        g('X') {
            stroke("Slant right to the bottom line.") { m(0, 0); l(70, 100) }
            stroke("Lift your pencil. Slant left to the bottom line.") { m(70, 0); l(0, 100) }
        },
        g('Y') {
            stroke("Slant right to the middle line.") { m(0, 0); l(35, 50) }
            stroke("Lift your pencil. Slant left to the middle, then pull down straight.") {
                m(70, 0); l(35, 50); l(35, 100)
            }
        },
        g('Z') {
            stroke("Slide right, slant left to the bottom line, then slide right.") {
                m(0, 0); l(65, 0); l(0, 100); l(65, 100)
            }
        },
    )
}
