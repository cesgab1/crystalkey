package com.crystalkey.core

import kotlin.math.max
import kotlin.math.min

/** What the microphone layer reports up. No audio ever leaves the device. */
data class RoomAudio(
    /** Wall clock of the last time *anyone* was heard. */
    val lastRoomVoiceMs: Long,
    /** Wall clock of the last time each seat was heard. */
    val lastSeatVoiceMs: Map<SeatId, Long>,
)

/**
 * Wrath — the boss's patience.
 *
 * It fills on silence, which is the inversion the whole product rests on: in
 * most co-op games pressure comes from the enemy acting, here it comes from the
 * room going quiet. Two separate sources:
 *
 *  - the **room** saying nothing for [PartyRules.roomSilenceMs]
 *  - **any one player** saying nothing for [PartyRules.seatSilenceMs], which
 *    only applies above four seats and exists solely to stop two loud siblings
 *    running the table
 */
class WrathMeter(
    private val rules: PartyRules,
    private val fillPerSecond: Float = 0.14f,
    private val seatFillPerSecond: Float = 0.09f,
    private val drainPerSecond: Float = 0.07f,
) {
    var value: Float = 0f
        private set

    /** Seats currently triggering the individual-silence rule. */
    var neglected: Set<SeatId> = emptySet()
        private set

    fun reset() {
        value = 0f
        neglected = emptySet()
    }

    fun tick(nowMs: Long, deltaMs: Long, audio: RoomAudio): Float {
        val seconds = deltaMs / 1000f
        val roomSilent = nowMs - audio.lastRoomVoiceMs >= rules.roomSilenceMs

        neglected = if (!rules.quietGuardEnabled) emptySet() else
            audio.lastSeatVoiceMs
                .filterValues { nowMs - it >= rules.seatSilenceMs }
                .keys

        var delta = 0f
        if (roomSilent) delta += fillPerSecond * seconds
        if (neglected.isNotEmpty()) delta += seatFillPerSecond * seconds * neglected.size
        if (!roomSilent && neglected.isEmpty()) delta -= drainPerSecond * seconds

        value = min(1f, max(0f, value + delta))
        return value
    }

    val isFull: Boolean get() = value >= 1f
}

/**
 * Lantern Light — the party's shared health, and the only thing a failed window
 * actually costs. Sized at seats + 1 so a bigger party is not a tougher party.
 */
class LanternLight(rules: PartyRules) {
    val segments: Int = rules.lanternSegments
    var remaining: Int = segments
        private set

    fun lose(n: Int = 1): Int {
        remaining = max(0, remaining - n)
        return remaining
    }

    fun restore() { remaining = segments }

    val isOut: Boolean get() = remaining == 0
}

/**
 * The 1.4-second window a cast has to land in.
 *
 * Everything about it is counted in players, not in damage: [PartyRules.castThreshold]
 * of the party has to act inside the same window, so coordination scales with
 * party size while the difficulty each individual faces does not.
 */
class BeatWindow(private val rules: PartyRules) {
    private var openedAtMs: Long = -1
    private val acted = LinkedHashSet<SeatId>()

    fun open(nowMs: Long) {
        openedAtMs = nowMs
        acted.clear()
    }

    fun isOpen(nowMs: Long): Boolean =
        openedAtMs >= 0 && nowMs - openedAtMs < rules.beatWindowMs

    /** Returns true if this act was inside the window and counted. */
    fun act(seat: SeatId, nowMs: Long): Boolean {
        if (!isOpen(nowMs)) return false
        acted += seat
        return true
    }

    val actedCount: Int get() = acted.size

    fun outcome(nowMs: Long): WindowOutcome = when {
        isOpen(nowMs) -> WindowOutcome.OPEN
        acted.size >= rules.castThreshold -> WindowOutcome.LANDED
        else -> WindowOutcome.MISSED
    }
}

enum class WindowOutcome { OPEN, LANDED, MISSED }
