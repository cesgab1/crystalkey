package com.crystalkey.core

/**
 * Splits one puzzle across whoever is in the room.
 *
 * The contract, in order of importance:
 *
 *  1. **Every seat gets at least one atom.** A seat holding nothing is a person
 *     holding a phone for twenty minutes.
 *  2. **No seat gets the whole puzzle.** Acting needs all six atoms, so any deal
 *     across two or more seats already guarantees this — it is asserted anyway,
 *     because it is the property everything else assumes.
 *  3. **Hands differ in size by at most one**, and the extra atoms go to the
 *     seats with the most carry capacity. That is how a six-year-old ends up
 *     holding exactly one atom and still being the reason the stage clears:
 *     difficulty is dealt, never picked off a menu.
 *  4. **The same seed produces the same deal on every phone.** Nobody transmits
 *     the deal, so nobody has to be the authority on it.
 */
object Dealer {

    fun deal(
        seats: List<Seat>,
        puzzle: PuzzleSpec,
        sessionSeed: Long,
        round: Int,
        previous: Deal? = null,
    ): Deal {
        require(seats.size >= PartyRules.MIN_SEATS) { "need at least ${PartyRules.MIN_SEATS} seats" }
        require(seats.size <= PartyRules.MAX_SEATS) { "at most ${PartyRules.MAX_SEATS} seats" }
        require(seats.size <= puzzle.atoms.size) {
            "cannot deal ${puzzle.atoms.size} atoms to ${seats.size} seats and leave nobody empty"
        }
        require(seats.map { it.id }.toSet().size == seats.size) { "duplicate seat id" }

        val ordered = seats.sortedBy { it.id.raw }
        val rng = SplitMix64(deriveSeed(sessionSeed, round, "deal"))
        val atoms = rng.shuffled(puzzle.atoms.map { it.id })

        val quotas = quotasFor(ordered, puzzle.atoms.size, round)

        // Round-robin, but start the walk at a different seat each round so the
        // bigger hands travel around the table instead of parking on one adult.
        val start = if (ordered.isEmpty()) 0 else round.mod(ordered.size)
        val walk = List(ordered.size) { ordered[(start + it).mod(ordered.size)] }

        val buckets = LinkedHashMap<SeatId, MutableList<AtomId>>()
        ordered.forEach { buckets[it.id] = mutableListOf() }

        var cursor = 0
        for (atom in atoms) {
            // find the next seat in the walk that still has room
            var guard = 0
            while (true) {
                val seat = walk[cursor.mod(walk.size)]
                cursor++
                if (buckets.getValue(seat.id).size < quotas.getValue(seat.id)) {
                    buckets.getValue(seat.id).add(atom)
                    break
                }
                guard++
                check(guard <= walk.size * 2) { "quotas do not cover every atom" }
            }
        }

        val fresh = previous?.let { freshen(buckets, it) } ?: buckets

        val deal = Deal(
            puzzleId = puzzle.id,
            round = round,
            hands = ordered.map { Hand(it.id, fresh.getValue(it.id).sortedBy { a -> a.raw }) },
        )
        assertSound(deal, puzzle)
        return deal
    }

    /**
     * Hand sizes differ by at most one, and the remainder goes to the seats with
     * the most carry capacity — a child is never handed two atoms while an adult
     * holds one.
     *
     * Within a capacity band the order rotates with [round]. Without that, the
     * extra atom parks on the same adult every single round: capacity decides
     * who *can* carry more, it should not decide who *always* does.
     */
    internal fun quotasFor(seats: List<Seat>, atomCount: Int, round: Int = 0): Map<SeatId, Int> {
        val base = atomCount / seats.size
        val extra = atomCount % seats.size
        val priority = seats
            .groupBy { it.carryCapacity }
            .toSortedMap(compareByDescending { it })
            .flatMap { (_, band) ->
                val sorted = band.sortedBy { it.id.raw }
                val shift = if (sorted.isEmpty()) 0 else round.mod(sorted.size)
                List(sorted.size) { sorted[(shift + it).mod(sorted.size)] }
            }
        val out = LinkedHashMap<SeatId, Int>()
        seats.forEach { out[it.id] = base }
        priority.take(extra).forEach { out[it.id] = out.getValue(it.id) + 1 }
        return out
    }

    /**
     * Nudges the deal away from handing a seat the same atom it held last round.
     * Pure swaps, so quotas and the "everyone holds something" property survive.
     * Deterministic: candidates are visited in a fixed order.
     */
    private fun freshen(
        buckets: Map<SeatId, MutableList<AtomId>>,
        previous: Deal,
    ): Map<SeatId, MutableList<AtomId>> {
        fun repeatsFor(seat: SeatId, atom: AtomId): Boolean =
            previous.hands.any { it.seat == seat && atom in it.atoms }

        val seatIds = buckets.keys.toList()
        repeat(SWAP_PASSES) {
            for (i in seatIds.indices) {
                for (j in seatIds.indices) {
                    if (i == j) continue
                    val a = buckets.getValue(seatIds[i])
                    val b = buckets.getValue(seatIds[j])
                    for (ai in a.indices) {
                        for (bi in b.indices) {
                            val before = (if (repeatsFor(seatIds[i], a[ai])) 1 else 0) +
                                (if (repeatsFor(seatIds[j], b[bi])) 1 else 0)
                            val after = (if (repeatsFor(seatIds[i], b[bi])) 1 else 0) +
                                (if (repeatsFor(seatIds[j], a[ai])) 1 else 0)
                            if (after < before) {
                                val tmp = a[ai]; a[ai] = b[bi]; b[bi] = tmp
                            }
                        }
                    }
                }
            }
        }
        return buckets
    }

    /** The properties the rest of the game is allowed to assume. */
    internal fun assertSound(deal: Deal, puzzle: PuzzleSpec) {
        val all = deal.hands.flatMap { it.atoms }
        check(all.size == puzzle.atoms.size) { "deal lost or duplicated atoms" }
        check(all.toSet().size == all.size) { "an atom was dealt twice" }
        check(deal.hands.all { it.atoms.isNotEmpty() }) { "a seat was dealt nothing" }
        check(deal.hands.none { it.atoms.size == puzzle.atoms.size }) {
            "one seat holds the whole puzzle — it could act alone"
        }
        val sizes = deal.hands.map { it.atoms.size }
        check(sizes.max() - sizes.min() <= 1) { "hands are lopsided: $sizes" }
    }

    private const val SWAP_PASSES = 2
}
