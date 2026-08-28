package com.crystalkey.core

/**
 * The session state machine.
 *
 * Two promises the product makes out loud are enforced here rather than in the
 * UI, because the UI is the wrong place to keep a promise:
 *
 *  - **A dropped phone freezes the quest and holds the seat.** [Frozen] wraps
 *    whatever was happening, and rejoining restores it exactly. Nobody loses a
 *    chapter because a kid's phone slept.
 *  - **Pausing holds all four (or six) phones.** [Paused] does the same, so
 *    nothing ticks for anybody.
 */
sealed interface SessionState {
    data object Idle : SessionState
    data class Hosting(val roomCode: String) : SessionState
    data class Joining(val roomCode: String) : SessionState
    data class Lobby(val roomCode: String, val seats: List<Seat>, val ready: Set<SeatId>) : SessionState {
        val everyoneReady: Boolean get() = seats.isNotEmpty() && ready.size == seats.size
        val canStart: Boolean get() = seats.size >= PartyRules.MIN_SEATS && everyoneReady
    }
    data class ChapterIntro(val chapter: Int, val party: Party) : SessionState
    data class QuestMap(val chapter: Int, val party: Party) : SessionState
    data class PuzzleTurn(val chapter: Int, val party: Party, val plan: TurnPlan, val deal: Deal) : SessionState
    data class BossStage(val chapter: Int, val party: Party, val bossHealth: Float) : SessionState
    data class ObjectiveComplete(val chapter: Int, val party: Party) : SessionState
    data class Debrief(val party: Party, val reason: EndReason) : SessionState

    /** Overlays: they remember exactly what they interrupted. */
    data class Paused(val resumeTo: SessionState) : SessionState
    data class Frozen(val missing: SeatId, val resumeTo: SessionState) : SessionState
}

data class Party(
    val roomCode: String,
    val seats: List<Seat>,
    val rules: PartyRules,
    val sessionSeed: Long,
    val lanternRemaining: Int,
)

enum class EndReason { LANTERN_OUT, SESSION_LIMIT, CLOSED_BY_HOST, CHAPTER_FINISHED }

sealed interface SessionEvent {
    data class HostRoom(val roomCode: String) : SessionEvent
    data class JoinRoom(val roomCode: String) : SessionEvent
    data class SeatJoined(val seat: Seat) : SessionEvent
    data class SeatReady(val seat: SeatId) : SessionEvent
    data class StartQuest(val sessionSeed: Long) : SessionEvent
    data object EnterMap : SessionEvent
    data class BeginTurn(val plan: TurnPlan, val deal: Deal) : SessionEvent
    data class TurnResolved(val landed: Boolean) : SessionEvent
    data object EnterBoss : SessionEvent
    data class BossResolved(val defeated: Boolean) : SessionEvent
    data object Pause : SessionEvent
    data object Resume : SessionEvent
    data class SeatDropped(val seat: SeatId) : SessionEvent
    data class SeatRejoined(val seat: SeatId) : SessionEvent
    data class End(val reason: EndReason) : SessionEvent
}

object SessionReducer {

    fun reduce(state: SessionState, event: SessionEvent): SessionState {
        // Overlays are handled first: while frozen or paused nothing else lands.
        when (state) {
            is SessionState.Paused -> return when (event) {
                is SessionEvent.Resume -> state.resumeTo
                is SessionEvent.End -> endFrom(state.resumeTo, event.reason)
                is SessionEvent.SeatDropped -> SessionState.Frozen(event.seat, state.resumeTo)
                else -> state
            }
            is SessionState.Frozen -> return when (event) {
                is SessionEvent.SeatRejoined ->
                    if (event.seat == state.missing) state.resumeTo else state
                is SessionEvent.End -> endFrom(state.resumeTo, event.reason)
                else -> state
            }
            else -> Unit
        }

        return when (event) {
            is SessionEvent.Pause -> SessionState.Paused(state)
            is SessionEvent.SeatDropped -> SessionState.Frozen(event.seat, state)
            is SessionEvent.End -> endFrom(state, event.reason)

            is SessionEvent.HostRoom -> SessionState.Hosting(event.roomCode)
            is SessionEvent.JoinRoom -> SessionState.Joining(event.roomCode)

            is SessionEvent.SeatJoined -> when (state) {
                is SessionState.Hosting -> SessionState.Lobby(state.roomCode, listOf(event.seat), emptySet())
                is SessionState.Joining -> SessionState.Lobby(state.roomCode, listOf(event.seat), emptySet())
                is SessionState.Lobby ->
                    if (state.seats.size >= PartyRules.MAX_SEATS) state
                    else state.copy(seats = state.seats + event.seat)
                else -> state
            }

            is SessionEvent.SeatReady -> when (state) {
                is SessionState.Lobby -> state.copy(ready = state.ready + event.seat)
                else -> state
            }

            is SessionEvent.StartQuest -> when {
                state is SessionState.Lobby && state.canStart -> {
                    val rules = PartyRules.forSeats(state.seats.size)
                    SessionState.ChapterIntro(
                        chapter = 1,
                        party = Party(state.roomCode, state.seats, rules, event.sessionSeed, rules.lanternSegments),
                    )
                }
                else -> state
            }

            is SessionEvent.EnterMap -> when (state) {
                is SessionState.ChapterIntro -> SessionState.QuestMap(state.chapter, state.party)
                is SessionState.ObjectiveComplete ->
                    SessionState.ChapterIntro(state.chapter + 1, state.party)
                else -> state
            }

            is SessionEvent.BeginTurn -> when (state) {
                is SessionState.QuestMap -> SessionState.PuzzleTurn(state.chapter, state.party, event.plan, event.deal)
                is SessionState.PuzzleTurn -> state.copy(plan = event.plan, deal = event.deal)
                else -> state
            }

            is SessionEvent.TurnResolved -> when (state) {
                is SessionState.PuzzleTurn ->
                    if (event.landed) {
                        SessionState.QuestMap(state.chapter, state.party)
                    } else {
                        val party = state.party.copy(lanternRemaining = state.party.lanternRemaining - 1)
                        if (party.lanternRemaining <= 0) SessionState.Debrief(party, EndReason.LANTERN_OUT)
                        else SessionState.PuzzleTurn(state.chapter, party, state.plan, state.deal)
                    }
                else -> state
            }

            is SessionEvent.EnterBoss -> when (state) {
                is SessionState.QuestMap -> SessionState.BossStage(state.chapter, state.party, 1f)
                else -> state
            }

            is SessionEvent.BossResolved -> when (state) {
                is SessionState.BossStage ->
                    if (event.defeated) SessionState.ObjectiveComplete(state.chapter, state.party)
                    else {
                        val party = state.party.copy(lanternRemaining = state.party.lanternRemaining - 1)
                        if (party.lanternRemaining <= 0) SessionState.Debrief(party, EndReason.LANTERN_OUT)
                        else SessionState.BossStage(state.chapter, party, state.bossHealth)
                    }
                else -> state
            }

            is SessionEvent.Resume, is SessionEvent.SeatRejoined -> state
        }
    }

    private fun endFrom(state: SessionState, reason: EndReason): SessionState =
        partyOf(state)?.let { SessionState.Debrief(it, reason) } ?: SessionState.Idle

    fun partyOf(state: SessionState): Party? = when (state) {
        is SessionState.ChapterIntro -> state.party
        is SessionState.QuestMap -> state.party
        is SessionState.PuzzleTurn -> state.party
        is SessionState.BossStage -> state.party
        is SessionState.ObjectiveComplete -> state.party
        is SessionState.Debrief -> state.party
        is SessionState.Paused -> partyOf(state.resumeTo)
        is SessionState.Frozen -> partyOf(state.resumeTo)
        else -> null
    }
}
