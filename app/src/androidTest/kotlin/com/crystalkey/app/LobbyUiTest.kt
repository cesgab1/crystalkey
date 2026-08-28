package com.crystalkey.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crystalkey.app.theme.CrystalKeyTheme
import com.crystalkey.app.ui.LobbyScreen
import com.crystalkey.app.ui.TitleScreen
import com.crystalkey.core.AgeBand
import com.crystalkey.core.Dealer
import com.crystalkey.core.Seat
import com.crystalkey.core.SeatId
import com.crystalkey.core.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Thin on purpose. The rules are proved in `:core` on the JVM, where they run in
 * milliseconds; these only check that the screens put the core's answers on
 * screen and that the primary buttons are wired.
 */
class LobbyUiTest {

    @get:Rule val compose = createComposeRule()

    private fun seats(n: Int) = (0 until n).map {
        val hero = com.crystalkey.app.ui.Art.heroForSeat(it)
        Seat(SeatId(it + 1), "Player ${it + 1}", hero.id, hero.band)
    }

    @Test
    fun titleScreenOffersBothWaysIn() {
        var hosted = false
        compose.setContent { CrystalKeyTheme { TitleScreen(onHost = { hosted = true }, onJoin = {}) } }
        compose.onNodeWithText("Crystal Key").assertIsDisplayed()
        compose.onNodeWithText("START A NEW QUEST").performClick()
        assertTrue("the primary button did not fire its callback", hosted)
    }

    @Test
    fun lobbyShowsWhatEachSeatIsCarrying() {
        val party = seats(6)
        val deal = Dealer.deal(party, SessionViewModel.SYNC_PATTERN, sessionSeed = 42L, round = 0)
        compose.setContent {
            CrystalKeyTheme {
                LobbyScreen(
                    state = SessionState.Lobby("JUNGLE24", party, party.map { it.id }.toSet()),
                    deal = deal,
                    onAddSeat = { _, _ -> },
                    onReady = {},
                    onStart = {},
                )
            }
        }
        compose.onNodeWithText("JUNGLE24").assertIsDisplayed()
        // six seats, six atoms — every seat should read "1 atom"
        compose.onAllNodesWithTextCount("1 atom", expected = 6)
    }
}

/** Small readability helper so the assertion above reads as one line. */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(
    text: String,
    expected: Int,
) {
    val found = onAllNodesWithText(text).fetchSemanticsNodes().size
    assertEquals("nodes reading \"$text\"", expected, found)
}
