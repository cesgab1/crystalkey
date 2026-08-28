package com.crystalkey.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.crystalkey.app.SessionViewModel
import com.crystalkey.app.theme.CrystalKeyTheme
import com.crystalkey.core.Dealer
import com.crystalkey.core.Party
import com.crystalkey.core.PartyRules
import com.crystalkey.core.Seat
import com.crystalkey.core.SeatId
import com.crystalkey.core.SessionState
import com.crystalkey.core.TurnPlanner

/**
 * Previews render in the design pane with no device and no emulator — the
 * fastest way to look at a screen, and the only way to see all party sizes at
 * once without playing four sessions.
 */
private fun previewSeats(n: Int): List<Seat> {
    val names = listOf("Mom", "Dad", "Leo", "Mia", "Gran", "Sam & Ada")
    return (0 until n).map { i ->
        val hero = Art.heroForSeat(i)
        Seat(SeatId(i + 1), names[i], hero.id, hero.band, buddy = i == 5)
    }
}

private fun previewLobby(n: Int, allReady: Boolean) = SessionState.Lobby(
    roomCode = "JUNGLE24",
    seats = previewSeats(n),
    ready = if (allReady) previewSeats(n).map { it.id }.toSet() else setOf(SeatId(1), SeatId(2)),
)

@Preview(name = "1 · Title", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun TitlePreview() {
    CrystalKeyTheme { TitleScreen(onHost = {}, onJoin = {}) }
}

@Preview(name = "2 · Lobby, 4 seats", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LobbyFourPreview() {
    val state = previewLobby(4, allReady = false)
    CrystalKeyTheme {
        LobbyScreen(
            state = state,
            deal = Dealer.deal(state.seats, SessionViewModel.SYNC_PATTERN, 42L, 0),
            onAddSeat = { _, _ -> }, onReady = {}, onStart = {},
        )
    }
}

@Preview(name = "2b · Lobby, 6 seats", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun LobbySixPreview() {
    val state = previewLobby(6, allReady = true)
    CrystalKeyTheme {
        LobbyScreen(
            state = state,
            deal = Dealer.deal(state.seats, SessionViewModel.SYNC_PATTERN, 42L, 0),
            onAddSeat = { _, _ -> }, onReady = {}, onStart = {},
        )
    }
}

@Preview(name = "3 · Chapter intro", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun ChapterPreview() {
    CrystalKeyTheme { ChapterScreen(chapter = 1, onEnter = {}) }
}

@Preview(name = "4 · Cards, seat 1", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun CardsFirstPreview() {
    val seats = previewSeats(4)
    CrystalKeyTheme {
        SeatCardsScreen(
            seats = seats,
            deal = Dealer.deal(seats, SessionViewModel.SYNC_PATTERN, 42L, 0),
            viewing = seats[0].id, onView = {}, onBack = {},
        )
    }
}

@Preview(name = "4b · Cards, seat 3 — different", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun CardsThirdPreview() {
    val seats = previewSeats(4)
    CrystalKeyTheme {
        SeatCardsScreen(
            seats = seats,
            deal = Dealer.deal(seats, SessionViewModel.SYNC_PATTERN, 42L, 0),
            viewing = seats[2].id, onView = {}, onBack = {},
        )
    }
}

private fun previewParty(n: Int): Party {
    val seats = previewSeats(n)
    val rules = PartyRules.forSeats(n)
    return Party("JUNGLE24", seats, rules, 42L, rules.lanternSegments)
}

@Preview(name = "5 · Quest map", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun QuestMapPreview() {
    val party = previewParty(4)
    val deal = Dealer.deal(party.seats, SessionViewModel.SYNC_PATTERN, 42L, 0)
    val plan = TurnPlanner.planRotation(party.seats, deal, party.rules, 42L, 1).first()
    CrystalKeyTheme {
        QuestMapScreen(party, plan, 1, party.seats.size, onBeginTurn = {}, onCards = {}, onLeave = {})
    }
}

@Preview(name = "6 · Turn — nothing asked yet", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun TurnPreview() {
    val party = previewParty(4)
    val deal = Dealer.deal(party.seats, SessionViewModel.SYNC_PATTERN, 42L, 0)
    val plan = TurnPlanner.planRotation(party.seats, deal, party.rules, 42L, 1).first()
    val held = deal.hands.first { it.seat == plan.actor }.atoms.toSet()
    CrystalKeyTheme {
        PuzzleTurnScreen(
            party = party,
            plan = plan,
            held = plan.requiredAtoms.filter { it in held }.map { SessionViewModel.SYNC_PATTERN.atom(it) },
            missing = plan.requiredAtoms.filterNot { it in held }.map { SessionViewModel.SYNC_PATTERN.atom(it) },
            spoken = emptySet(),
            holderOf = { a -> deal.hands.firstOrNull { a in it.atoms }?.seat },
            onSpeak = {}, onLockIn = {}, onCards = {},
        )
    }
}

@Preview(name = "6b · Turn — everything asked", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun TurnReadyPreview() {
    val party = previewParty(4)
    val deal = Dealer.deal(party.seats, SessionViewModel.SYNC_PATTERN, 42L, 0)
    val plan = TurnPlanner.planRotation(party.seats, deal, party.rules, 42L, 1).first()
    val held = deal.hands.first { it.seat == plan.actor }.atoms.toSet()
    val missing = plan.requiredAtoms.filterNot { it in held }
    CrystalKeyTheme {
        PuzzleTurnScreen(
            party = party,
            plan = plan,
            held = plan.requiredAtoms.filter { it in held }.map { SessionViewModel.SYNC_PATTERN.atom(it) },
            missing = missing.map { SessionViewModel.SYNC_PATTERN.atom(it) },
            spoken = missing.toSet(),
            holderOf = { a -> deal.hands.firstOrNull { a in it.atoms }?.seat },
            onSpeak = {}, onLockIn = {}, onCards = {},
        )
    }
}

@Preview(name = "7 · Objective complete", widthDp = 390, heightDp = 845, showBackground = true)
@Composable
private fun CompletePreview() {
    CrystalKeyTheme { ObjectiveCompleteScreen(previewParty(4), onContinue = {}, onCards = {}) }
}
