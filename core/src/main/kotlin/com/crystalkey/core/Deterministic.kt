package com.crystalkey.core

/**
 * SplitMix64 — a tiny, exactly-specified PRNG.
 *
 * Why not [kotlin.random.Random]: every phone in the room has to derive the
 * *same* deal from the same seed without anyone sending it. That only works if
 * the generator is bit-for-bit specified rather than "whatever the platform
 * ships". SplitMix64 is 8 lines and identical everywhere, so the deal is
 * derived state, not network traffic — which is what lets the game run with no
 * server and survive the host's phone dropping.
 */
class SplitMix64(seed: Long) {
    private var state: Long = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL // golden gamma
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** Uniform in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        val r = (nextLong() ushr 1) % bound
        return r.toInt()
    }

    /** Fisher–Yates, so the order depends only on the seed. */
    fun <T> shuffled(items: List<T>): List<T> {
        val out = items.toMutableList()
        for (i in out.indices.reversed()) {
            val j = nextInt(i + 1)
            val tmp = out[i]; out[i] = out[j]; out[j] = tmp
        }
        return out
    }
}

/**
 * Derives a stable sub-seed. Mixing the session seed with a round number and a
 * purpose tag means the dealer and the turn planner never accidentally walk the
 * same sequence.
 */
fun deriveSeed(sessionSeed: Long, round: Int, purpose: String): Long {
    var h = sessionSeed xor (round.toLong() * 0x9E3779B97F4A7C15uL.toLong())
    for (c in purpose) {
        h = (h xor c.code.toLong()) * 0x100000001B3L
    }
    return h
}
