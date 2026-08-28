package com.crystalkey.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crystalkey.app.theme.CrystalKeyTheme
import com.crystalkey.app.ui.ChapterScreen
import com.crystalkey.app.ui.DebriefScreen
import com.crystalkey.app.ui.LobbyScreen
import com.crystalkey.app.ui.ObjectiveCompleteScreen
import com.crystalkey.app.ui.PuzzleTurnScreen
import com.crystalkey.app.ui.QuestMapScreen
import com.crystalkey.app.ui.SeatCardsScreen
import com.crystalkey.app.ui.TitleScreen
import com.crystalkey.core.EndReason
import com.crystalkey.core.SessionEvent
import com.crystalkey.core.SessionReducer
import com.crystalkey.core.SessionState

/** Bumped by hand each time a build is handed over, so it is obvious which one is on the device. */
private const val BUILD_TAG = "build 7 · turn loop"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrystalKeyTheme {
                // If the last run died, show the reason instead of a black screen.
                val context = LocalContext.current
                var crash by remember { mutableStateOf(CrystalKeyApp.lastCrash(context)) }
                val reported = crash
                if (reported != null) {
                    CrashScreen(reported) {
                        CrystalKeyApp.clearLastCrash(context)
                        crash = null
                    }
                } else {
                    // Deliberate teal ground behind everything. If the device
                    // shows pure black, this build is not the one running —
                    // the problem is install or theme, not the screens. If it
                    // shows teal with nothing on it, composition runs but the
                    // screens draw nothing. One glance separates the two.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF10323A)),
                    ) {
                        App()
                        Text(
                            BUILD_TAG,
                            color = Color(0x99FFFFFF),
                            fontSize = 9.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Routing.
 *
 * Two rules here are load-bearing and were both broken before:
 *
 *  - **No early `return` anywhere in this function.** A non-local return out of
 *    a composable — especially from inside an argument list — can leave the
 *    Compose runtime's group bookkeeping unbalanced, and the symptom is a
 *    screen that renders nothing at all. Everything below is plain nesting.
 *  - **The cards view is an overlay, not a destination.** It draws over what is
 *    underneath and closing it returns there. Wiring it as a state is what made
 *    "Back to the room" end the session and appear to do nothing.
 */
@Composable
private fun App(vm: SessionViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val deal by vm.deal.collectAsState()
    val turn by vm.turn.collectAsState()
    val viewing by vm.viewingSeat.collectAsState()
    val showCards by vm.showCards.collectAsState()
    val spoken by vm.spoken.collectAsState()

    val party = SessionReducer.partyOf(state)

    BackHandler(enabled = showCards) { vm.closeCards() }

    if (showCards && party != null) {
        SeatCardsScreen(
            seats = party.seats,
            deal = deal,
            viewing = viewing ?: party.seats.first().id,
            onView = { vm.viewAs(it) },
            onBack = { vm.closeCards() },
        )
    } else {
        when (val s = state) {
            is SessionState.Idle ->
                TitleScreen(onHost = { vm.hostRoom() }, onJoin = { vm.hostRoom() })

            is SessionState.Hosting, is SessionState.Joining, is SessionState.Lobby ->
                LobbyScreen(
                    state = s,
                    deal = deal,
                    onAddSeat = { name, band -> vm.joinSeat(name, band) },
                    onReady = { vm.toggleReady(it) },
                    onStart = { vm.startQuest() },
                )

            is SessionState.ChapterIntro ->
                ChapterScreen(chapter = s.chapter, onEnter = { vm.dispatch(SessionEvent.EnterMap) })

            is SessionState.QuestMap ->
                QuestMapScreen(
                    party = s.party,
                    plan = turn,
                    turnNumber = vm.turnNumber(),
                    rotationSize = vm.rotationSize(),
                    onBeginTurn = { vm.beginTurn() },
                    onCards = { vm.openCards() },
                    onLeave = { vm.dispatch(SessionEvent.End(EndReason.CLOSED_BY_HOST)) },
                )

            is SessionState.PuzzleTurn ->
                PuzzleTurnScreen(
                    party = s.party,
                    plan = s.plan,
                    held = vm.heldForTurn(),
                    missing = vm.missingForTurn(),
                    spoken = spoken,
                    holderOf = { vm.holderOf(it) },
                    onSpeak = { vm.speak(it) },
                    onLockIn = { vm.lockIn() },
                    onCards = { vm.openCards() },
                )

            is SessionState.BossStage ->
                QuestMapScreen(
                    party = s.party,
                    plan = turn,
                    turnNumber = vm.turnNumber(),
                    rotationSize = vm.rotationSize(),
                    onBeginTurn = { vm.beginTurn() },
                    onCards = { vm.openCards() },
                    onLeave = { vm.dispatch(SessionEvent.End(EndReason.CLOSED_BY_HOST)) },
                )

            is SessionState.ObjectiveComplete ->
                ObjectiveCompleteScreen(
                    party = s.party,
                    onContinue = { vm.dispatch(SessionEvent.End(EndReason.CHAPTER_FINISHED)) },
                    onCards = { vm.openCards() },
                )

            is SessionState.Debrief ->
                DebriefScreen(
                    party = s.party,
                    reasonText = when (s.reason) {
                        EndReason.LANTERN_OUT ->
                            "The lantern went out. Too many guesses landed before the room had all the pieces."
                        EndReason.CHAPTER_FINISHED -> "Chapter one is done. The next realm is waiting."
                        EndReason.SESSION_LIMIT -> "Time's up — stopped at a chapter end rather than mid-fight."
                        EndReason.CLOSED_BY_HOST -> "The room was closed. Nothing was lost; the quest can start again."
                    },
                    onDone = { vm.reset() },
                )

            is SessionState.Paused ->
                if (party == null) {
                    Diagnostic("Paused with no party — this should be impossible.") { vm.reset() }
                } else {
                    QuestMapScreen(
                        party = party,
                        plan = turn,
                        turnNumber = vm.turnNumber(),
                        rotationSize = vm.rotationSize(),
                        onBeginTurn = { vm.dispatch(SessionEvent.Resume) },
                        onCards = { vm.openCards() },
                        onLeave = { vm.dispatch(SessionEvent.End(EndReason.CLOSED_BY_HOST)) },
                    )
                }

            is SessionState.Frozen ->
                if (party == null) {
                    Diagnostic("A phone dropped before the quest started.") { vm.reset() }
                } else {
                    QuestMapScreen(
                        party = party,
                        plan = turn,
                        turnNumber = vm.turnNumber(),
                        rotationSize = vm.rotationSize(),
                        onBeginTurn = { vm.dispatch(SessionEvent.SeatRejoined(s.missing)) },
                        onCards = { vm.openCards() },
                        onLeave = { vm.dispatch(SessionEvent.End(EndReason.CLOSED_BY_HOST)) },
                    )
                }
        }
    }
}

/**
 * A visible dead end instead of an invisible one.
 *
 * If routing ever lands somewhere with nothing to draw, this says so on screen
 * and offers a way out. A black screen tells nobody anything; this at least
 * names the state it got stuck in.
 */
@Composable
private fun Diagnostic(message: String, onReset: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0A14))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Something is wrong", color = Color(0xFFEE6E5C), fontSize = 20.sp)
        Text(message, color = Color(0xFFF2E4C4), fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
        Text(
            "Tap here to start over",
            color = Color(0xFF5FC9D6),
            fontSize = 15.sp,
            modifier = Modifier
                .padding(top = 24.dp)
                .background(Color(0x335FC9D6))
                .clickable(onClick = onReset)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/**
 * The last fatal exception, on screen, selectable and scrollable.
 *
 * A blank screen is the worst possible failure mode: it carries no information
 * and costs a whole round-trip to diagnose. This costs one screenshot.
 */
@Composable
private fun CrashScreen(trace: String, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF17070A))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Crystal Key crashed", color = Color(0xFFEE6E5C), fontSize = 19.sp)
        Text(
            "This is the last fatal error. Screenshot it — it is exactly what is needed to fix this.",
            color = Color(0xFFB8B2C6),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        Text(
            "Dismiss and try again",
            color = Color(0xFF5FC9D6),
            fontSize = 15.sp,
            modifier = Modifier
                .background(Color(0x335FC9D6))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text(
            trace,
            color = Color(0xFFF2E4C4),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
