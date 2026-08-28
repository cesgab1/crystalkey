package com.crystalkey.core

/**
 * The six interrogatives a puzzle can be split along.
 *
 * These are NOT roles. A seat is dealt atoms; the interrogative only describes
 * what kind of question the atom answers, which is what the UI labels.
 */
enum class Interrogative(val label: String) {
    WHERE("Where"),
    WHAT("What"),
    WHEN("When"),
    HOW("How"),
    WHY("Why"),
    HOW_MANY("How many"),
}

/** Age band drives how many atoms a seat can comfortably carry, nothing else. */
enum class AgeBand(val carryCapacity: Int) {
    CHILD(1),
    TEEN(2),
    ADULT(3),
}

@JvmInline
value class SeatId(val raw: Int) {
    override fun toString() = "seat$raw"
}

@JvmInline
value class AtomId(val raw: Int) {
    override fun toString() = "atom$raw"
}

/**
 * One player's place at the table.
 *
 * [buddy] marks a shared phone: two humans, one lane in the relay. It changes
 * nothing mechanically — it exists so the UI can render two portraits and so
 * the quiet timer knows to be gentler.
 */
data class Seat(
    val id: SeatId,
    val displayName: String,
    val heroId: String,
    val band: AgeBand,
    val buddy: Boolean = false,
) {
    val carryCapacity: Int get() = band.carryCapacity
}

/**
 * An indivisible piece of information. A puzzle is authored once as a fixed
 * number of atoms and *acting on it requires every one of them* — that is the
 * property the whole design rests on, so it is asserted here rather than
 * assumed downstream.
 */
data class Atom(
    val id: AtomId,
    val interrogative: Interrogative,
    val label: String,
)

/**
 * A puzzle as the content team authors it: always [ATOMS_PER_PUZZLE] atoms,
 * whatever the party size. Merging atoms down onto fewer seats is always safe;
 * splitting them up is not — which is why six is the authoring floor.
 */
data class PuzzleSpec(
    val id: String,
    val title: String,
    val atoms: List<Atom>,
) {
    init {
        require(atoms.size == ATOMS_PER_PUZZLE) {
            "a puzzle must be authored as exactly $ATOMS_PER_PUZZLE atoms, got ${atoms.size} in '$id'"
        }
        require(atoms.map { it.id }.toSet().size == atoms.size) { "duplicate atom id in '$id'" }
    }

    fun atom(id: AtomId): Atom = atoms.first { it.id == id }

    companion object {
        const val ATOMS_PER_PUZZLE = 6
    }
}

/** The atoms one seat is holding for one round. */
data class Hand(val seat: SeatId, val atoms: List<AtomId>) {
    init { require(atoms.isNotEmpty()) { "$seat was dealt nothing — every seat must be load-bearing" } }
}

/** The full deal for one round: every seat's hand. */
data class Deal(
    val puzzleId: String,
    val round: Int,
    val hands: List<Hand>,
) {
    val seatCount: Int get() = hands.size

    fun handOf(seat: SeatId): Hand = hands.first { it.seat == seat }

    fun holderOf(atom: AtomId): SeatId =
        hands.first { atom in it.atoms }.seat

    fun seatsHolding(atoms: Collection<AtomId>): Set<SeatId> =
        atoms.map { holderOf(it) }.toSet()
}
