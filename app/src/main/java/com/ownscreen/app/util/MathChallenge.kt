package com.ownscreen.app.util

import kotlin.random.Random

/**
 * Deliberate friction before unblocking an app: hard enough to need a moment of real mental
 * effort (not "2 + 2"), easy enough to still solve in your head without reaching for a
 * calculator — that gap is the point, since a calculator defeats the purpose of the friction.
 */
data class MathChallenge(val question: String, val answer: Int) {
    companion object {
        fun random(): MathChallenge = when (Random.nextInt(2)) {
            0 -> {
                val a = Random.nextInt(12, 49)
                val b = Random.nextInt(6, 10)
                MathChallenge("$a × $b", a * b)
            }
            else -> {
                val a = Random.nextInt(120, 881)
                val b = Random.nextInt(130, 881)
                MathChallenge("$a + $b", a + b)
            }
        }
    }
}
