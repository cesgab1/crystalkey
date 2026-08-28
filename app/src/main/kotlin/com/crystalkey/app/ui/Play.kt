package com.crystalkey.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crystalkey.core.Atom
import com.crystalkey.core.AtomId
import com.crystalkey.core.Party
import com.crystalkey.core.Seat
import com.crystalkey.core.SeatId
import com.crystalkey.core.TurnPlan
import com.crystalkey.app.theme.CrystalPalette
import com.crystalkey.app.theme.CrystalShapes
import com.crystalkey.app.theme.seatColor

/** Shared header: chapter, lantern segments, and the way into the cards. */
@Composable
private fun QuestHeader(party: Party, label: String, title: String, onCards: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            SectionLabel(label, CrystalPalette.NatureGreen)
            Text(title, color = CrystalPalette.Bone, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(
            Modifier
                .clip(CrystalShapes.chip)
                .background(Color(0xC7101820))
                .border(2.dp, CrystalPalette.LanternGold.copy(alpha = 0.6f), CrystalShapes.chip)
                .clickable(onClick = onCards)
                .padding(horizontal = 11.dp, vertical = 9.dp),
        ) {
            Text("MY CARD", color = CrystalPalette.LanternGold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        }
    }
    Spacer(Modifier.height(9.dp))
    LanternBar(party.lanternRemaining, party.rules.lanternSegments)
}

@Composable
private fun LanternBar(remaining: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (i < remaining) CrystalPalette.LanternGold else Color(0x66463C3A)),
            )
        }
    }
}

// ------------------------------------------------------------------ quest map

/**
 * The between-turns screen. It exists to answer one question — whose hands are
 * on the puzzle next — and to be the place you come back to.
 */
@Composable
fun QuestMapScreen(
    party: Party,
    plan: TurnPlan?,
    turnNumber: Int,
    rotationSize: Int,
    onBeginTurn: () -> Unit,
    onCards: () -> Unit,
    onLeave: () -> Unit,
) {
    val actorIndex = party.seats.indexOfFirst { it.id == plan?.actor }.coerceAtLeast(0)
    val actor = party.seats.getOrNull(actorIndex)
    val accent = seatColor(actorIndex)

    PaintedScreen(Art.plateTrail, Scrims.trail) {
        QuestHeader(party, "Lanternwood · the clearing", "The Whispering Woods", onCards)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            if (actor != null) {
                GlassPanel(Modifier.fillMaxWidth(), accent.copy(alpha = 0.55f)) {
                    SectionLabel("Turn $turnNumber of $rotationSize · hands on the puzzle", accent)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Portrait(Art.hero(actor.heroId).portrait, accent, 52)
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(actor.displayName, color = CrystalPalette.Bone, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                Art.hero(actor.heroId).title,
                                color = accent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                            )
                        }
                    }
                }
            }

            if (plan != null) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    SectionLabel("Who this turn needs", CrystalPalette.StoneLight)
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        party.seats.forEachIndexed { i, s ->
                            val needed = s.id in plan.speakers || s.id == plan.actor
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Portrait(
                                    Art.hero(s.heroId).portrait,
                                    if (needed) seatColor(i) else Color(0x4DB8B2C6),
                                    38,
                                )
                                Text(
                                    if (s.id == plan.actor) "acts" else if (needed) "speaks" else "idle",
                                    color = if (needed) seatColor(i) else CrystalPalette.Muted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                    }
                    if (plan.quiet.isNotEmpty()) {
                        Text(
                            "The idle seats are not forgotten — Wrath fills if anyone stays quiet too long.",
                            color = CrystalPalette.Rose,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            ParchmentCard(Modifier.fillMaxWidth()) {
                Text(
                    "The path forks at the old stones. Nobody's map shows the whole route — " +
                        "take a turn and find out what you are missing.",
                    color = CrystalPalette.ParchmentInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = 10.dp)) {
            CrystalButton("Take the turn", Modifier.fillMaxWidth(), ButtonStyle.Primary, 62, onBeginTurn)
            CrystalButton("Leave the quest", Modifier.fillMaxWidth(), ButtonStyle.Ghost, 46, onLeave)
        }
    }
}

// ------------------------------------------------------------------ the turn

/**
 * The playable beat, reduced to what one device can honestly show.
 *
 * The actor holds part of what the action needs and is missing the rest. Each
 * missing piece is locked behind the seat that holds it, and the only way to
 * open it is to ask. On one phone that is a tap; in a room it is a sentence.
 * Locking in early is allowed and costs a lantern segment — the game does not
 * stop you making the mistake, it just charges for it.
 */
@Composable
fun PuzzleTurnScreen(
    party: Party,
    plan: TurnPlan,
    held: List<Atom>,
    missing: List<Atom>,
    spoken: Set<AtomId>,
    holderOf: (AtomId) -> SeatId?,
    onSpeak: (AtomId) -> Unit,
    onLockIn: () -> Unit,
    onCards: () -> Unit,
) {
    val actorIndex = party.seats.indexOfFirst { it.id == plan.actor }.coerceAtLeast(0)
    val actor = party.seats.getOrNull(actorIndex)
    val accent = seatColor(actorIndex)
    val ready = missing.all { it.id in spoken }

    PaintedScreen(Art.plateChamber, Scrims.chamber) {
        QuestHeader(party, "The Sync Pattern", actor?.displayName?.let { "$it's turn" } ?: "Your turn", onCards)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Spacer(Modifier.height(6.dp))

            GlassPanel(Modifier.fillMaxWidth(), accent.copy(alpha = 0.55f)) {
                SectionLabel("On your screen", accent)
                held.forEach { atom ->
                    AtomRow(atom, accent, open = true)
                }
                if (held.isEmpty()) {
                    Text("Nothing — this one is all on the others.", color = CrystalPalette.Muted, fontSize = 12.sp)
                }
            }

            GlassPanel(
                Modifier.fillMaxWidth(),
                if (ready) CrystalPalette.NatureGreen.copy(alpha = 0.6f) else CrystalPalette.WrathRed.copy(alpha = 0.45f),
            ) {
                SectionLabel(
                    if (ready) "All in — you can lock it" else "You are missing ${missing.count { it.id !in spoken }}",
                    if (ready) CrystalPalette.NatureGreen else CrystalPalette.WrathRed,
                )
                missing.forEach { atom ->
                    val holder = holderOf(atom.id)
                    val hIndex = party.seats.indexOfFirst { it.id == holder }.coerceAtLeast(0)
                    val hSeat = party.seats.getOrNull(hIndex)
                    val said = atom.id in spoken
                    AskRow(
                        atom = atom,
                        seat = hSeat,
                        accent = seatColor(hIndex),
                        said = said,
                        onAsk = { onSpeak(atom.id) },
                    )
                }
            }

            ParchmentCard(Modifier.fillMaxWidth()) {
                Text(
                    "At a real table nobody taps anything here — you say “what comes fourth?” " +
                        "and someone answers. One device just makes the asking visible.",
                    color = CrystalPalette.ParchmentInk,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = 10.dp)) {
            CrystalButton(
                if (ready) "Lock it in" else "Lock it in anyway",
                Modifier.fillMaxWidth(),
                if (ready) ButtonStyle.Primary else ButtonStyle.Danger,
                62,
                onLockIn,
            )
            if (!ready) {
                Text(
                    "Guessing costs a lantern segment.",
                    color = CrystalPalette.WrathRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AtomRow(atom: Atom, color: Color, open: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(
            if (open) "✓" else "✕",
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(end = 8.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                atom.interrogative.label.uppercase(),
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
            Text(
                atom.label,
                color = CrystalPalette.Bone,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun AskRow(atom: Atom, seat: Seat?, accent: Color, said: Boolean, onAsk: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(CrystalShapes.chip)
            .background(if (said) accent.copy(alpha = 0.12f) else Color(0x4D1A1424))
            .border(
                1.5.dp,
                if (said) accent.copy(alpha = 0.6f) else Color(0x3DB8B2C6),
                CrystalShapes.chip,
            )
            .clickable(enabled = !said, onClick = onAsk)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (seat != null) Portrait(Art.hero(seat.heroId).portrait, accent, 34)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                atom.interrogative.label.uppercase(),
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
            Text(
                if (said) atom.label else "held by ${seat?.displayName ?: "someone else"}",
                color = if (said) CrystalPalette.Bone else CrystalPalette.Muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
            )
        }
        if (!said) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = 0.22f))
                    .border(1.5.dp, accent, RoundedCornerShape(9.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("ASK", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            }
        }
    }
}

// ------------------------------------------------------------------ endings

@Composable
fun ObjectiveCompleteScreen(party: Party, onContinue: () -> Unit, onCards: () -> Unit) {
    PaintedScreen(Art.plateCampfire, Scrims.campfire) {
        QuestHeader(party, "Lanternwood · chapter 1", "Objective complete", onCards)
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "The lantern is lit",
                color = CrystalPalette.LanternGold,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(10.dp))
            ParchmentCard(Modifier.fillMaxWidth()) {
                Text(
                    "Nobody could have finished that alone. Every piece had to cross the room " +
                        "out loud before the door would open.",
                    color = CrystalPalette.ParchmentInk,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                )
            }
        }
        CrystalButton("Back to camp", Modifier.fillMaxWidth(), ButtonStyle.Primary, 62, onContinue)
    }
}

@Composable
fun DebriefScreen(party: Party, reasonText: String, onDone: () -> Unit) {
    PaintedScreen(Art.plateCampfire, Scrims.campfire) {
        SectionLabel("Session over", CrystalPalette.SunsetOrange)
        Text("Back to camp", color = CrystalPalette.Bone, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            ParchmentCard(Modifier.fillMaxWidth()) {
                Text(reasonText, color = CrystalPalette.ParchmentInk, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(12.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                SectionLabel("The party", CrystalPalette.StoneLight)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    party.seats.forEachIndexed { i, s ->
                        Portrait(Art.hero(s.heroId).portrait, seatColor(i), 40)
                    }
                }
            }
        }
        CrystalButton("Finish", Modifier.fillMaxWidth(), ButtonStyle.Primary, 62, onDone)
    }
}
