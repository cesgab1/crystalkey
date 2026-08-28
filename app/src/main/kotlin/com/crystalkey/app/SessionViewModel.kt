package com.crystalkey.app

import androidx.lifecycle.ViewModel
import com.crystalkey.app.ui.Art
import com.crystalkey.core.AgeBand
import com.crystalkey.core.Atom
import com.crystalkey.core.AtomId
import com.crystalkey.core.Deal
import com.crystalkey.core.Dealer
import com.crystalkey.core.Interrogative
import com.crystalkey.core.PartyRules
import com.crystalkey.core.PuzzleSpec
import com.crystalkey.core.Seat
import com.crystalkey.core.SeatId
import com.crystalkey.core.SessionEvent
import com.crystalkey.core.SessionReducer
import com.crystalkey.core.SessionState
import com.crystalkey.core.TurnPlan
import com.crystalkey.core.TurnPlanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the session and forwards every change through the verified reducer.
 *
 * No game rule is decided here. This owns state, generates one seed, and asks
 * `core` for the deal and the turn plans — so the properties proved in the core
 * test suite are the properties the UI actually gets.
 */
class SessionViewModel : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _deal = MutableStateFlow<Deal?>(null)
    val deal: StateFlow<Deal?> = _deal.asStateFlow()

    private val _turn = MutableStateFlow<TurnPlan?>(null)
    val turn: StateFlow<TurnPlan?> = _turn.asStateFlow()

    /**
     * Which seat's screen is being shown.
     *
     * On a real table this is always "mine" and never changes. Until the local
     * transport exists every seat is on one device, so the cards view steps
     * between them — the clearest way to show why the room has to talk.
     */
    private val _viewingSeat = MutableStateFlow<SeatId?>(null)
    val viewingSeat: StateFlow<SeatId?> = _viewingSeat.asStateFlow()

    /** The cards view is an overlay, not a destination — closing it goes back. */
    private val _showCards = MutableStateFlow(false)
    val showCards: StateFlow<Boolean> = _showCards.asStateFlow()

    /**
     * Atoms that have been said out loud this turn.
     *
     * On one device "asking Dad" is a tap. With phones spread round a room it is
     * a sentence, and this set is what the microphone would fill instead.
     */
    private val _spoken = MutableStateFlow<Set<AtomId>>(emptySet())
    val spoken: StateFlow<Set<AtomId>> = _spoken.asStateFlow()

    private var rotation: List<TurnPlan> = emptyList()
    private var turnIndex: Int = 0

    // ------------------------------------------------------------- lifecycle

    fun dispatch(event: SessionEvent) {
        _state.value = SessionReducer.reduce(_state.value, event)
    }

    fun hostRoom(code: String = randomRoomCode()) = dispatch(SessionEvent.HostRoom(code))

    /** Seats get a hero from the roster in join order, so a portrait shows up at once. */
    fun joinSeat(displayName: String, band: AgeBand, buddy: Boolean = false) {
        val taken = seatsInLobby()
        val hero = Art.heroForSeat(taken)
        dispatch(SessionEvent.SeatJoined(Seat(SeatId(taken + 1), displayName, hero.id, band, buddy)))
    }

    fun toggleReady(seat: SeatId) = dispatch(SessionEvent.SeatReady(seat))

    fun viewAs(seat: SeatId) { _viewingSeat.value = seat }

    fun openCards() { _showCards.value = true }

    fun closeCards() { _showCards.value = false }

    fun reset() {
        _state.value = SessionState.Idle
        _deal.value = null
        _turn.value = null
        _viewingSeat.value = null
        _showCards.value = false
        _spoken.value = emptySet()
        rotation = emptyList()
        turnIndex = 0
    }

    // ------------------------------------------------------------- the quest

    /**
     * Starts the quest and derives the first deal and the whole turn rotation.
     *
     * The seed is generated once; everything after it is a pure function of it,
     * which is why no device has to be the authority on who holds what.
     */
    fun startQuest(seed: Long = System.nanoTime()) {
        dispatch(SessionEvent.StartQuest(seed))
        val party = SessionReducer.partyOf(_state.value) ?: return
        val dealt = Dealer.deal(party.seats, SYNC_PATTERN, party.sessionSeed, round = 0)
        _deal.value = dealt
        rotation = TurnPlanner.planRotation(party.seats, dealt, party.rules, party.sessionSeed, chapter = 1)
        turnIndex = 0
        _turn.value = rotation.firstOrNull()
        _viewingSeat.value = party.seats.firstOrNull()?.id
    }

    /** Moves from the map into the current turn. */
    fun beginTurn() {
        if (rotation.isEmpty()) return
        val plan = rotation[turnIndex % rotation.size]
        val dealt = _deal.value ?: return
        _turn.value = plan
        _spoken.value = emptySet()
        _viewingSeat.value = plan.actor
        dispatch(SessionEvent.BeginTurn(plan, dealt))
    }

    /** One atom said out loud — a tap here, a sentence at a real table. */
    fun speak(atom: AtomId) { _spoken.value = _spoken.value + atom }

    /**
     * The actor commits. Landing needs every required atom to have reached them,
     * either because they already hold it or because somebody said it.
     */
    fun lockIn() {
        val plan = _turn.value ?: return
        val dealt = _deal.value ?: return
        val held = dealt.hands.firstOrNull { it.seat == plan.actor }?.atoms.orEmpty().toSet()
        val landed = plan.requiredAtoms.all { it in held || it in _spoken.value }
        dispatch(SessionEvent.TurnResolved(landed))
        if (landed) {
            turnIndex += 1
            _spoken.value = emptySet()
        }
    }

    /** Required atoms the actor does not hold — the ones that have to be spoken. */
    fun missingForTurn(): List<Atom> {
        val plan = _turn.value ?: return emptyList()
        val dealt = _deal.value ?: return emptyList()
        val held = dealt.hands.firstOrNull { it.seat == plan.actor }?.atoms.orEmpty().toSet()
        return plan.requiredAtoms.filterNot { it in held }.map { SYNC_PATTERN.atom(it) }
    }

    /** Required atoms the actor already has on their own screen. */
    fun heldForTurn(): List<Atom> {
        val plan = _turn.value ?: return emptyList()
        val dealt = _deal.value ?: return emptyList()
        val held = dealt.hands.firstOrNull { it.seat == plan.actor }?.atoms.orEmpty().toSet()
        return plan.requiredAtoms.filter { it in held }.map { SYNC_PATTERN.atom(it) }
    }

    fun holderOf(atom: AtomId): SeatId? =
        _deal.value?.hands?.firstOrNull { atom in it.atoms }?.seat

    fun rules(): PartyRules? = SessionReducer.partyOf(_state.value)?.rules

    fun turnNumber(): Int = turnIndex + 1

    fun rotationSize(): Int = rotation.size

    private fun seatsInLobby(): Int = (_state.value as? SessionState.Lobby)?.seats?.size ?: 0

    companion object {

        fun randomRoomCode(): String {
            val word = listOf("JUNGLE", "CAVERN", "EMBER", "TIDE", "STAR", "STONE").random()
            return word + (10..99).random()
        }

        /** The worked example from the design canvas, authored at six atoms. */
        val SYNC_PATTERN = PuzzleSpec(
            id = "lanternwood.sync-pattern",
            title = "The Sync Pattern",
            atoms = listOf(
                Atom(AtomId(1), Interrogative.HOW, "the table — which stones exist and where"),
                Atom(AtomId(2), Interrogative.WHAT, "the order — the sequence they follow"),
                Atom(AtomId(3), Interrogative.WHEN, "the beat — the window to lock one in"),
                Atom(AtomId(4), Interrogative.WHERE, "the shape — what the door is asking for"),
                Atom(AtomId(5), Interrogative.WHY, "the trap — the stone that must stay dark"),
                Atom(AtomId(6), Interrogative.HOW_MANY, "the count — placements before the spike"),
            ),
        )
    }
}
