package com.crystalkey.core

/**
 * Who has hands on the mechanism this turn, whose information they need, and
 * who is therefore idle.
 */
data class TurnPlan(
    val turn: Int,
    val actor: SeatId,
    /** The atoms this particular action consumes. */
    val requiredAtoms: List<AtomId>,
    /** Seats other than the actor holding a required atom — they have to speak. */
    val speakers: Set<SeatId>,
    /** Seats genuinely not needed this turn. The quiet guard exists for these. */
    val quiet: Set<SeatId>,
) {
    val distinctContributors: Int get() = speakers.size + 1
}

/**
 * Builds the turn order for a chapter.
 *
 * Two fairness properties are enforced rather than hoped for:
 *
 *  - **Every seat is the actor exactly once** per full rotation, so having hands
 *    on the puzzle is not something the loudest player accumulates.
 *  - **No seat is quiet on two consecutive turns.** The planner reaches for
 *    atoms held by whoever was idle last turn before it reaches for anything
 *    else, so the puzzle itself goes and gets the quiet child. It is a schedule,
 *    not a grown-up enforcing a rule.
 *
 * It also refuses to build a turn whose required atoms all sit on too few
 * phones — see [PartyRules.minDistinctSpeakers].
 */
object TurnPlanner {

    fun rotation(seats: List<Seat>, sessionSeed: Long, chapter: Int): List<SeatId> {
        val rng = SplitMix64(deriveSeed(sessionSeed, chapter, "actors"))
        return rng.shuffled(seats.sortedBy { it.id.raw }.map { it.id })
    }

    /** Plans one full rotation: [seats].size turns, starting at [firstTurn]. */
    fun planRotation(
        seats: List<Seat>,
        deal: Deal,
        rules: PartyRules,
        sessionSeed: Long,
        chapter: Int,
        firstTurn: Int = 0,
    ): List<TurnPlan> {
        val order = rotation(seats, sessionSeed, chapter)
        val plans = mutableListOf<TurnPlan>()
        var previousQuiet: Set<SeatId> = emptySet()
        for (i in order.indices) {
            val plan = planTurn(
                turn = firstTurn + i,
                actor = order[i],
                deal = deal,
                rules = rules,
                previousQuiet = previousQuiet,
                sessionSeed = sessionSeed,
            )
            plans += plan
            previousQuiet = plan.quiet
        }
        return plans
    }

    fun planTurn(
        turn: Int,
        actor: SeatId,
        deal: Deal,
        rules: PartyRules,
        previousQuiet: Set<SeatId>,
        sessionSeed: Long,
    ): TurnPlan {
        val actorHand = deal.handOf(actor).atoms
        val required = LinkedHashSet<AtomId>()
        val represented = LinkedHashSet<SeatId>()

        // The actor's own atoms are on the table already.
        required += actorHand
        represented += actor

        val rng = SplitMix64(deriveSeed(sessionSeed, turn, "required"))
        val candidates = rng.shuffled(
            deal.hands.filter { it.seat != actor }.flatMap { hand -> hand.atoms.map { hand.seat to it } }
        )

        fun priority(seat: SeatId): Int = when {
            seat in previousQuiet && seat !in represented -> 0 // idle last turn — pull them in first
            seat !in represented -> 1                          // widen the relay
            else -> 2                                          // already talking
        }

        // Take atoms until the action is covered AND enough phones are involved.
        while (required.size < rules.atomsRequired || represented.size < rules.minDistinctSpeakers) {
            val next = candidates
                .filter { it.second !in required }
                .minByOrNull { (seat, atom) -> priority(seat) * 1_000_000 + atom.raw }
                ?: break
            required += next.second
            represented += next.first
        }

        val speakers = deal.seatsHolding(required) - actor
        val quiet = deal.hands.map { it.seat }.toSet() - speakers - actor

        val plan = TurnPlan(turn, actor, required.toList(), speakers, quiet)
        assertSound(plan, rules, previousQuiet)
        return plan
    }

    internal fun assertSound(plan: TurnPlan, rules: PartyRules, previousQuiet: Set<SeatId>) {
        check(plan.actor !in plan.speakers) { "the actor cannot also be a speaker" }
        check(plan.actor !in plan.quiet) { "the actor cannot be quiet" }
        check(plan.distinctContributors >= rules.minDistinctSpeakers) {
            "turn ${plan.turn} only pulls from ${plan.distinctContributors} phones, " +
                "below the ${rules.minDistinctSpeakers} needed — someone could act alone"
        }
        val stillQuiet = previousQuiet intersect plan.quiet
        check(stillQuiet.isEmpty()) {
            "turn ${plan.turn} leaves $stillQuiet quiet for a second turn running"
        }
    }
}
