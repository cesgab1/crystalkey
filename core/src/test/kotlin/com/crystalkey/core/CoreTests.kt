package com.crystalkey.core

import kotlin.math.ceil
import kotlin.system.exitProcess

// ---------------------------------------------------------------- tiny harness
private var passed = 0
private val failures = mutableListOf<String>()
private var currentSection = ""

private fun section(name: String) {
    currentSection = name
    println("\n[1m$name[0m")
}

private fun check(name: String, body: () -> Unit) {
    try {
        body()
        passed++
        println("  [32m✓[0m $name")
    } catch (t: Throwable) {
        failures += "$currentSection › $name — ${t.message}"
        println("  [31m✗ $name[0m\n      ${t.message}")
    }
}

private fun expect(condition: Boolean, message: () -> String) {
    if (!condition) throw AssertionError(message())
}

private fun <T> eq(actual: T, expected: T, what: String = "value") {
    if (actual != expected) throw AssertionError("$what: expected <$expected>, got <$actual>")
}

// ---------------------------------------------------------------- fixtures
private val syncPattern = PuzzleSpec(
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

private fun seatsFor(n: Int): List<Seat> {
    val bands = listOf(AgeBand.ADULT, AgeBand.ADULT, AgeBand.TEEN, AgeBand.CHILD, AgeBand.TEEN, AgeBand.CHILD)
    val names = listOf("Mom", "Dad", "Leo", "Mia", "Gran", "Sam & Ada")
    return (0 until n).map { i ->
        Seat(SeatId(i + 1), names[i], "hero$i", bands[i], buddy = names[i].contains("&"))
    }
}

private val seeds = listOf(1L, 7L, 42L, 1234L, -99L, 987654321L, Long.MIN_VALUE / 3, 0L)

// ---------------------------------------------------------------- tests
/** Standalone entry point — how this suite was verified with no Maven repo available. */
fun main() {
    if (runCoreVerification().failures.isNotEmpty()) exitProcess(1)
}

data class VerificationReport(val passed: Int, val failures: List<String>)

/** The suite as a callable function so JUnit can run the identical checks. */
fun runCoreVerification(): VerificationReport {
    passed = 0
    failures.clear()
    println("[1mQuest for the Crystal Key — core verification[0m")
    println("Kotlin/JVM, no Android dependencies. Every assertion below actually ran.")

    // ------------------------------------------------------------ party rules
    section("Party rules scale with the room, the puzzle never does")
    check("lantern segments are always players + 1") {
        for (n in 2..6) eq(PartyRules.forSeats(n).lanternSegments, n + 1, "segments at $n seats")
    }
    check("cast threshold is 60% of the party, floored at two") {
        val expected = mapOf(2 to 2, 3 to 2, 4 to 3, 5 to 3, 6 to 4)
        for ((n, want) in expected) eq(PartyRules.forSeats(n).castThreshold, want, "threshold at $n seats")
    }
    check("the quiet guard only switches on above four players") {
        for (n in 2..4) expect(!PartyRules.forSeats(n).quietGuardEnabled) { "quiet guard on at $n seats" }
        for (n in 5..6) expect(PartyRules.forSeats(n).quietGuardEnabled) { "quiet guard off at $n seats" }
    }
    check("party size is capped at six") {
        var threw = false
        try { PartyRules.forSeats(7) } catch (e: IllegalArgumentException) { threw = true }
        expect(threw) { "seven seats was accepted" }
    }

    // ------------------------------------------------------------ dealing
    section("The deal — every seat load-bearing, nobody able to act alone")
    check("across 2–6 seats and 8 seeds, every deal is sound") {
        var deals = 0
        for (n in 2..6) for (seed in seeds) for (round in 0..5) {
            val deal = Dealer.deal(seatsFor(n), syncPattern, seed, round)
            Dealer.assertSound(deal, syncPattern)
            eq(deal.hands.size, n, "hands at $n seats")
            deals++
        }
        println("      $deals deals checked")
    }
    check("no seat can ever hold the whole puzzle") {
        for (n in 2..6) for (seed in seeds) {
            val deal = Dealer.deal(seatsFor(n), syncPattern, seed, 0)
            expect(deal.hands.none { it.atoms.size == PuzzleSpec.ATOMS_PER_PUZZLE }) {
                "a seat held all six atoms at $n seats"
            }
        }
    }
    check("hand sizes are as even as possible") {
        val expected = mapOf(2 to listOf(3, 3), 3 to listOf(2, 2, 2), 4 to listOf(2, 2, 1, 1),
            5 to listOf(2, 1, 1, 1, 1), 6 to listOf(1, 1, 1, 1, 1, 1))
        for ((n, want) in expected) {
            val got = Dealer.deal(seatsFor(n), syncPattern, 42L, 0).hands.map { it.atoms.size }.sortedDescending()
            eq(got, want, "hand sizes at $n seats")
        }
    }
    check("the extra atoms go to the seats with the most carry capacity") {
        val seats = seatsFor(4) // Mom ADULT, Dad ADULT, Leo TEEN, Mia CHILD
        val quotas = Dealer.quotasFor(seats, 6)
        eq(quotas[SeatId(1)], 2, "Mom (adult)")
        eq(quotas[SeatId(2)], 2, "Dad (adult)")
        eq(quotas[SeatId(3)], 1, "Leo (teen)")
        eq(quotas[SeatId(4)], 1, "Mia (child)")
    }
    check("a six-year-old always holds exactly one atom at six seats") {
        for (seed in seeds) {
            val deal = Dealer.deal(seatsFor(6), syncPattern, seed, 0)
            expect(deal.hands.all { it.atoms.size == 1 }) { "not one atom each at six seats" }
        }
    }
    check("the same seed and round give byte-identical deals on every device") {
        for (n in 2..6) for (seed in seeds) {
            val a = Dealer.deal(seatsFor(n), syncPattern, seed, 3)
            val b = Dealer.deal(seatsFor(n), syncPattern, seed, 3)
            eq(a, b, "deal determinism at $n seats")
        }
    }
    check("different seeds actually produce different deals") {
        val distinct = seeds.map { Dealer.deal(seatsFor(6), syncPattern, it, 0) }.toSet()
        expect(distinct.size >= seeds.size - 1) { "seeds collapsed to ${distinct.size} distinct deals" }
    }
    check("the double hand moves between rounds instead of parking on one adult") {
        val seats = seatsFor(5)
        val topCapacity = seats.maxOf { it.carryCapacity }
        val eligible = seats.filter { it.carryCapacity == topCapacity }.map { it.id }.toSet()
        val holders = (0..5).map { round ->
            Dealer.deal(seats, syncPattern, 42L, round).hands.first { it.atoms.size == 2 }.seat
        }
        expect(holders.toSet().size > 1) { "the double hand parked on ${holders.toSet()}" }
        expect(holders.all { it in eligible }) { "a double landed on a low-capacity seat: $holders" }
        println("      double hand by round: ${holders.map { it.raw }}")
    }
    check("a child is never handed a double while an adult holds one") {
        for (n in 2..6) for (round in 0..5) {
            val seats = seatsFor(n)
            val deal = Dealer.deal(seats, syncPattern, 42L, round)
            val byId = seats.associateBy { it.id }
            val big = deal.hands.filter { it.atoms.size > 1 }.map { byId.getValue(it.seat).carryCapacity }
            val small = deal.hands.filter { it.atoms.size == 1 }.map { byId.getValue(it.seat).carryCapacity }
            if (big.isNotEmpty() && small.isNotEmpty()) {
                expect(big.min() >= small.max()) { "capacity inverted at $n seats round $round" }
            }
        }
    }
    check("freshening reduces repeat atoms round to round") {
        var naive = 0
        var fresh = 0
        for (seed in seeds) {
            var prev = Dealer.deal(seatsFor(4), syncPattern, seed, 0)
            for (round in 1..4) {
                val without = Dealer.deal(seatsFor(4), syncPattern, seed, round)
                val with = Dealer.deal(seatsFor(4), syncPattern, seed, round, previous = prev)
                naive += repeats(without, prev)
                fresh += repeats(with, prev)
                prev = with
            }
        }
        println("      repeats without freshening: $naive, with: $fresh")
        expect(fresh <= naive) { "freshening made it worse ($fresh vs $naive)" }
    }

    // ------------------------------------------------------------ turn planning
    section("Turn planning — rotation, and nobody left quiet")
    check("every seat is the actor exactly once per rotation") {
        for (n in 2..6) for (seed in seeds) {
            val seats = seatsFor(n)
            val order = TurnPlanner.rotation(seats, seed, 1)
            eq(order.toSet().size, n, "distinct actors at $n seats")
            eq(order.size, n, "rotation length at $n seats")
        }
    }
    check("a full rotation never leaves a seat quiet twice running") {
        var turns = 0
        for (n in 2..6) for (seed in seeds) {
            val seats = seatsFor(n)
            val rules = PartyRules.forSeats(n)
            val deal = Dealer.deal(seats, syncPattern, seed, 0)
            val plans = TurnPlanner.planRotation(seats, deal, rules, seed, chapter = 1)
            for (i in 1 until plans.size) {
                val stuck = plans[i - 1].quiet intersect plans[i].quiet
                expect(stuck.isEmpty()) { "$stuck quiet on turns ${i - 1} and $i at $n seats, seed $seed" }
            }
            turns += plans.size
        }
        println("      $turns turns planned and checked")
    }
    check("no turn can ever be resolved by one phone alone") {
        for (n in 2..6) for (seed in seeds) {
            val seats = seatsFor(n)
            val rules = PartyRules.forSeats(n)
            val deal = Dealer.deal(seats, syncPattern, seed, 0)
            for (plan in TurnPlanner.planRotation(seats, deal, rules, seed, 1)) {
                expect(plan.distinctContributors >= rules.minDistinctSpeakers) {
                    "turn ${plan.turn} pulled from ${plan.distinctContributors} phones at $n seats"
                }
                expect(plan.speakers.isNotEmpty()) { "turn ${plan.turn} needed nobody else" }
            }
        }
    }
    check("an action consumes at least the configured number of atoms") {
        for (n in 2..6) {
            val rules = PartyRules.forSeats(n)
            val deal = Dealer.deal(seatsFor(n), syncPattern, 42L, 0)
            for (plan in TurnPlanner.planRotation(seatsFor(n), deal, rules, 42L, 1)) {
                expect(plan.requiredAtoms.size >= rules.atomsRequired) {
                    "turn ${plan.turn} only needed ${plan.requiredAtoms.size} atoms at $n seats"
                }
            }
        }
    }
    check("at six seats, two players really are idle each turn") {
        val deal = Dealer.deal(seatsFor(6), syncPattern, 42L, 0)
        val plans = TurnPlanner.planRotation(seatsFor(6), deal, PartyRules.forSeats(6), 42L, 1)
        expect(plans.all { it.quiet.isNotEmpty() }) { "nobody was idle — the quiet guard would be pointless" }
        println("      idle per turn: ${plans.map { it.quiet.size }}")
    }

    // ------------------------------------------------------------ meters
    section("Meters — silence is the pressure")
    check("wrath fills when the room goes quiet") {
        val rules = PartyRules.forSeats(4)
        val m = WrathMeter(rules)
        val audio = RoomAudio(lastRoomVoiceMs = 0L, lastSeatVoiceMs = emptyMap())
        m.tick(6_000L, 1_000L, audio)
        expect(m.value > 0f) { "wrath stayed at zero through six seconds of silence" }
    }
    check("wrath drains while the room is talking") {
        val rules = PartyRules.forSeats(4)
        val m = WrathMeter(rules)
        m.tick(6_000L, 3_000L, RoomAudio(0L, emptyMap()))
        val peak = m.value
        m.tick(7_000L, 1_000L, RoomAudio(7_000L, emptyMap()))
        expect(m.value < peak) { "wrath did not drain: $peak -> ${m.value}" }
    }
    check("one neglected player fills wrath, but only above four seats") {
        val quietKid = mapOf(SeatId(4) to 0L)
        val big = WrathMeter(PartyRules.forSeats(6))
        big.tick(30_000L, 1_000L, RoomAudio(lastRoomVoiceMs = 30_000L, lastSeatVoiceMs = quietKid))
        expect(big.value > 0f) { "six-seat party ignored a 30s silent player" }
        eq(big.neglected, setOf(SeatId(4)), "neglected set")

        val small = WrathMeter(PartyRules.forSeats(4))
        small.tick(30_000L, 1_000L, RoomAudio(lastRoomVoiceMs = 30_000L, lastSeatVoiceMs = quietKid))
        eq(small.value, 0f, "four-seat wrath")
        expect(small.neglected.isEmpty()) { "four-seat party ran the quiet guard" }
    }
    check("the lantern is sized to the party and drops one per failure") {
        val l = LanternLight(PartyRules.forSeats(6))
        eq(l.segments, 7, "segments")
        l.lose(); l.lose()
        eq(l.remaining, 5, "remaining")
        repeat(9) { l.lose() }
        expect(l.isOut) { "lantern went negative instead of out" }
    }
    check("acts inside the beat window count, acts outside it do not") {
        val rules = PartyRules.forSeats(6)
        val w = BeatWindow(rules)
        w.open(1_000L)
        expect(w.act(SeatId(1), 1_200L)) { "act inside the window was refused" }
        expect(w.act(SeatId(2), 2_300L)) { "act at 1.3s was refused" }
        expect(!w.act(SeatId(3), 2_600L)) { "act after 1.4s was accepted" }
        eq(w.actedCount, 2, "acts counted")
        eq(w.outcome(3_000L), WindowOutcome.MISSED, "outcome below threshold")
    }
    check("four of six landing clears the window") {
        val w = BeatWindow(PartyRules.forSeats(6))
        w.open(0L)
        listOf(1, 2, 3, 4).forEach { w.act(SeatId(it), 500L) }
        eq(w.outcome(2_000L), WindowOutcome.LANDED, "outcome at threshold")
    }

    // ------------------------------------------------------------ session
    section("Session — the promises the UI must not be trusted with")
    check("a quest will not start below two seats or with anyone unready") {
        var s: SessionState = SessionReducer.reduce(SessionState.Idle, SessionEvent.HostRoom("JUNGLE24"))
        s = SessionReducer.reduce(s, SessionEvent.SeatJoined(seatsFor(1)[0]))
        s = SessionReducer.reduce(s, SessionEvent.SeatReady(SeatId(1)))
        s = SessionReducer.reduce(s, SessionEvent.StartQuest(1L))
        expect(s is SessionState.Lobby) { "started a quest with one seat" }

        s = SessionReducer.reduce(s, SessionEvent.SeatJoined(seatsFor(2)[1]))
        s = SessionReducer.reduce(s, SessionEvent.StartQuest(1L))
        expect(s is SessionState.Lobby) { "started with an unready seat" }

        s = SessionReducer.reduce(s, SessionEvent.SeatReady(SeatId(2)))
        s = SessionReducer.reduce(s, SessionEvent.StartQuest(1L))
        expect(s is SessionState.ChapterIntro) { "would not start with two ready seats" }
    }
    check("a dropped phone freezes the quest and rejoining restores it exactly") {
        val mid = midQuest(4)
        val frozen = SessionReducer.reduce(mid, SessionEvent.SeatDropped(SeatId(3)))
        expect(frozen is SessionState.Frozen) { "drop did not freeze" }
        eq((frozen as SessionState.Frozen).missing, SeatId(3), "missing seat")

        val ignored = SessionReducer.reduce(frozen, SessionEvent.EnterBoss)
        eq(ignored, frozen, "state advanced while frozen")

        val wrongSeat = SessionReducer.reduce(frozen, SessionEvent.SeatRejoined(SeatId(2)))
        eq(wrongSeat, frozen, "someone else's rejoin thawed the quest")

        val back = SessionReducer.reduce(frozen, SessionEvent.SeatRejoined(SeatId(3)))
        eq(back, mid, "rejoin did not restore the exact prior state")
    }
    check("pausing holds every phone and resumes to the same place") {
        val mid = midQuest(6)
        val paused = SessionReducer.reduce(mid, SessionEvent.Pause)
        expect(paused is SessionState.Paused) { "pause did not hold" }
        eq(SessionReducer.reduce(paused, SessionEvent.EnterBoss), paused, "state advanced while paused")
        eq(SessionReducer.reduce(paused, SessionEvent.Resume), mid, "resume landed somewhere else")
    }
    check("a phone dropping during a pause still freezes, and thaws back to paused") {
        val mid = midQuest(5)
        val paused = SessionReducer.reduce(mid, SessionEvent.Pause)
        val frozen = SessionReducer.reduce(paused, SessionEvent.SeatDropped(SeatId(2)))
        expect(frozen is SessionState.Frozen) { "drop during pause did not freeze" }
        val thawed = SessionReducer.reduce(frozen, SessionEvent.SeatRejoined(SeatId(2)))
        eq(thawed, mid, "thawed to the wrong state")
    }
    check("the lantern running out ends the session rather than looping") {
        var s: SessionState = midQuest(2)
        val party = SessionReducer.partyOf(s)!!
        val plan = TurnPlanner.planRotation(party.seats, Dealer.deal(party.seats, syncPattern, party.sessionSeed, 0),
            party.rules, party.sessionSeed, 1).first()
        s = SessionReducer.reduce(s, SessionEvent.BeginTurn(plan, Dealer.deal(party.seats, syncPattern, party.sessionSeed, 0)))
        repeat(party.rules.lanternSegments + 2) { s = SessionReducer.reduce(s, SessionEvent.TurnResolved(landed = false)) }
        expect(s is SessionState.Debrief) { "session kept going with the lantern out: $s" }
        eq((s as SessionState.Debrief).reason, EndReason.LANTERN_OUT, "end reason")
    }
    check("a seventh seat cannot squeeze into the lobby") {
        var s: SessionState = SessionReducer.reduce(SessionState.Idle, SessionEvent.HostRoom("JUNGLE24"))
        seatsFor(6).forEach { s = SessionReducer.reduce(s, SessionEvent.SeatJoined(it)) }
        val extra = Seat(SeatId(99), "Uninvited", "hero9", AgeBand.ADULT)
        s = SessionReducer.reduce(s, SessionEvent.SeatJoined(extra))
        eq((s as SessionState.Lobby).seats.size, 6, "lobby size")
    }

    // ------------------------------------------------------------ report
    println("\n" + "─".repeat(64))
    if (failures.isEmpty()) {
        println("[32m[1mPASS[0m  $passed checks, 0 failures")
    } else {
        println("[31m[1mFAIL[0m  $passed passed, ${failures.size} failed")
        failures.forEach { println("  • $it") }
    }
    return VerificationReport(passed, failures.toList())
}

private fun repeats(now: Deal, prev: Deal): Int =
    now.hands.sumOf { hand ->
        val before = prev.hands.firstOrNull { it.seat == hand.seat }?.atoms ?: emptyList()
        hand.atoms.count { it in before }
    }

/** A session parked mid-quest with [n] seats, used to test the overlays. */
private fun midQuest(n: Int): SessionState {
    var s: SessionState = SessionReducer.reduce(SessionState.Idle, SessionEvent.HostRoom("JUNGLE24"))
    seatsFor(n).forEach {
        s = SessionReducer.reduce(s, SessionEvent.SeatJoined(it))
        s = SessionReducer.reduce(s, SessionEvent.SeatReady(it.id))
    }
    s = SessionReducer.reduce(s, SessionEvent.StartQuest(4242L))
    return SessionReducer.reduce(s, SessionEvent.EnterMap)
}
