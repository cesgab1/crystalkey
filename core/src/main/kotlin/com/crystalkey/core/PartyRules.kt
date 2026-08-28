package com.crystalkey.core

import kotlin.math.ceil
import kotlin.math.max

/**
 * Everything that scales with how many people are actually in the room.
 *
 * The puzzle never scales — it is always six atoms. Only these numbers move,
 * which is what keeps 2..6 players on one content pipeline.
 */
data class PartyRules(
    val seatCount: Int,
    /** Shared health. A bigger party is not a tougher party. */
    val lanternSegments: Int,
    /** How many players must tap inside the same beat window. */
    val castThreshold: Int,
    /** How many of the six atoms a single action consumes. */
    val atomsRequired: Int,
    /** The loud-sibling guard. Only earns its place above four players. */
    val quietGuardEnabled: Boolean,
    val roomSilenceMs: Long,
    val seatSilenceMs: Long,
    val beatWindowMs: Long,
) {
    init {
        require(seatCount in MIN_SEATS..MAX_SEATS) {
            "party size $seatCount is outside $MIN_SEATS..$MAX_SEATS"
        }
    }

    /**
     * The smallest number of distinct seats an action must pull from.
     *
     * This is the invariant that stops the game collapsing back into one person
     * solving quietly: a required atom set that all sits on one phone would let
     * a single player act alone, so the planner is never allowed to choose one.
     */
    val minDistinctSpeakers: Int get() = minOf(seatCount, castThreshold)

    companion object {
        const val MIN_SEATS = 2
        const val MAX_SEATS = 6

        /**
         * Past six seats the call-and-answer takes longer than a beat window,
         * so the relay cannot land and the game just feels slow. Seven people
         * play as six seats with one buddy lane.
         */
        fun forSeats(seatCount: Int): PartyRules = PartyRules(
            seatCount = seatCount,
            lanternSegments = seatCount + 1,
            castThreshold = max(2, ceil(seatCount * 0.6).toInt()),
            atomsRequired = 4,
            quietGuardEnabled = seatCount > 4,
            roomSilenceMs = 5_000L,
            seatSilenceMs = 25_000L,
            beatWindowMs = 1_400L,
        )
    }
}
