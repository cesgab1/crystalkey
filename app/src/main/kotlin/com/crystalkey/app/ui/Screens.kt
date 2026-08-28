package com.crystalkey.app.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crystalkey.app.SessionViewModel
import com.crystalkey.app.theme.CrystalPalette
import com.crystalkey.app.theme.CrystalShapes
import com.crystalkey.app.theme.seatColor
import com.crystalkey.core.AgeBand
import com.crystalkey.core.Deal
import com.crystalkey.core.PartyRules
import com.crystalkey.core.PuzzleSpec
import com.crystalkey.core.Seat
import com.crystalkey.core.SeatId
import com.crystalkey.core.SessionState

// ------------------------------------------------------------------ 1 · title

@Composable
fun TitleScreen(onHost: () -> Unit, onJoin: () -> Unit) {
    PaintedScreen(Art.plateTrail, Scrims.trail) {
        Spacer(Modifier.height(18.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            ParchmentCard(Modifier.fillMaxWidth(0.94f)) {
                Text(
                    "Quest for the",
                    color = Color(0xFF7A4A18),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Crystal Key",
                    color = Color(0xFF16606E),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "2–6 players · one room · a phone each",
                color = CrystalPalette.Bone,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Nobody can see the whole puzzle.",
                color = CrystalPalette.LanternGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(Art.cast),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            CrystalButton("Start a new quest", Modifier.fillMaxWidth(), ButtonStyle.Primary, 64, onHost)
            CrystalButton("Join a family room", Modifier.fillMaxWidth(), ButtonStyle.Secondary, 58, onJoin)
            Text(
                "LOCAL WI-FI · NO ACCOUNT · NO ADS",
                color = Color(0xFF8FA09E),
                fontSize = 10.sp,
                letterSpacing = 1.3.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ------------------------------------------------------------------ 2 · lobby

@Composable
fun LobbyScreen(
    state: SessionState,
    deal: Deal?,
    onAddSeat: (String, AgeBand) -> Unit,
    onReady: (SeatId) -> Unit,
    onStart: () -> Unit,
) {
    val lobby = state as? SessionState.Lobby
    val seats = lobby?.seats.orEmpty()
    val rules = if (seats.size >= PartyRules.MIN_SEATS) PartyRules.forSeats(seats.size) else null
    val code = lobby?.roomCode ?: (state as? SessionState.Hosting)?.roomCode ?: "…"

    PaintedScreen(Art.plateCampfire, Scrims.campfire) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Step 2 of 3", CrystalPalette.SunsetOrange)
                Text("Family Room", color = CrystalPalette.Bone, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            }
            RoomCodeChip(code)
        }

        Spacer(Modifier.height(14.dp))

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            seats.forEachIndexed { index, seat ->
                SeatRow(
                    seat = seat,
                    accent = seatColor(index),
                    carrying = deal?.hands?.firstOrNull { it.seat == seat.id }?.atoms?.size,
                    ready = lobby?.ready?.contains(seat.id) == true,
                    onClick = { onReady(seat.id) },
                )
            }
            if (seats.isEmpty()) {
                Text(
                    "Nobody has joined yet. Tap “Add a seat” to sit someone down.",
                    color = CrystalPalette.Muted,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            }
            if (rules != null) {
                Spacer(Modifier.height(5.dp))
                GlassPanel(Modifier.fillMaxWidth()) {
                    SectionLabel("This party", CrystalPalette.LanternGold)
                    Text(
                        "${PuzzleSpec.ATOMS_PER_PUZZLE} atoms across ${seats.size} seats · " +
                            "${rules.lanternSegments} lantern segments · " +
                            "${rules.castThreshold} of ${seats.size} must act in the beat",
                        color = CrystalPalette.StoneLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (rules.quietGuardEnabled) {
                        Text(
                            "Quiet guard on — Wrath fills if anyone goes 25s without speaking.",
                            color = CrystalPalette.Rose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = 10.dp)) {
            if (seats.size < PartyRules.MAX_SEATS) {
                CrystalButton("Add a seat", Modifier.fillMaxWidth(), ButtonStyle.Ghost, 48) {
                    onAddSeat("Player ${seats.size + 1}", Art.heroForSeat(seats.size).band)
                }
            }
            if (seats.isNotEmpty() && lobby?.everyoneReady != true) {
                CrystalButton("Everyone ready", Modifier.fillMaxWidth(), ButtonStyle.Ghost, 48) {
                    seats.forEach { onReady(it.id) }
                }
            }
            CrystalButton(
                if (lobby?.canStart == true) "Begin the quest" else "Waiting for the room",
                Modifier.fillMaxWidth(),
                if (lobby?.canStart == true) ButtonStyle.Primary else ButtonStyle.Ghost,
                62,
                onStart,
            )
        }
    }
}

@Composable
private fun RoomCodeChip(code: String) {
    Box(
        Modifier
            .clip(CrystalShapes.system)
            .background(Brush.verticalGradient(CrystalPalette.teal))
            .border(2.dp, Color(0xFF59C4D2), CrystalShapes.system)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(code, color = Color(0xFFDFF6F9), fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun SeatRow(
    seat: Seat,
    accent: Color,
    carrying: Int?,
    ready: Boolean,
    onClick: () -> Unit,
) {
    val hero = Art.hero(seat.heroId)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CrystalShapes.chip)
            .background(Color(0xC7101820))
            .border(2.dp, accent.copy(alpha = 0.55f), CrystalShapes.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Portrait(hero.portrait, accent, 44)
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) {
            Text(seat.displayName, color = CrystalPalette.Bone, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                when (carrying) {
                    null -> hero.title
                    1 -> "${hero.title} · 1 atom"
                    else -> "${hero.title} · $carrying atoms"
                },
                color = accent,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
            )
        }
        Badge(if (ready) "READY" else "TAP", ready)
    }
}

@Composable
private fun Badge(text: String, on: Boolean) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (on) CrystalPalette.NatureGreen else Color(0x4D78728C))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            color = if (on) Color(0xFF0E2A16) else CrystalPalette.StoneLight,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp,
        )
    }
}

// ------------------------------------------------------------------ 3 · chapter

@Composable
fun ChapterScreen(chapter: Int, onEnter: () -> Unit) {
    PaintedScreen(Art.plateTrail, Scrims.trail) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Lanternwood · Chapter $chapter", CrystalPalette.NatureGreen)
            Text(
                "The Lantern Goes Out",
                color = CrystalPalette.Bone,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            Image(
                painter = painterResource(Art.crystalKey),
                contentDescription = null,
                modifier = Modifier.height(74.dp).padding(vertical = 12.dp),
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ParchmentCard(Modifier.fillMaxWidth()) {
                Text(
                    "The great lantern at the heart of Lanternwood has burned for four hundred " +
                        "years. Tonight it went dark, and the forest began to forget its own paths.",
                    color = CrystalPalette.ParchmentInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp,
                )
                Text(
                    "The old Guardian still stands at the clearing. It will not let strangers " +
                        "pass. It will let through one family that can finish each other's sentences.",
                    color = CrystalPalette.ParchmentInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp,
                )
            }
            GlassPanel(Modifier.fillMaxWidth(), CrystalPalette.LanternGold.copy(alpha = 0.5f)) {
                SectionLabel("Objective", CrystalPalette.LanternGold)
                Text(
                    "Relight the lantern before the forest forgets the way home.",
                    color = CrystalPalette.Bone,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                )
            }
        }

        CrystalButton("See what you're holding", Modifier.fillMaxWidth(), ButtonStyle.Primary, 62, onEnter)
    }
}

// ------------------------------------------------------------------ 4 · the cards

/**
 * The payoff screen, and the one that makes the concept land on a single device.
 *
 * In a real session each phone shows only its own card. Here every seat is on
 * one device, so there is a switcher along the top: tap between players and
 * watch what each of them is and is not allowed to see. It is the fastest way
 * to understand why the room has to talk.
 */
@Composable
fun SeatCardsScreen(
    seats: List<Seat>,
    deal: Deal?,
    viewing: SeatId,
    onView: (SeatId) -> Unit,
    onBack: () -> Unit,
) {
    val index = seats.indexOfFirst { it.id == viewing }.coerceAtLeast(0)
    val seat = seats.getOrNull(index) ?: return
    val accent = seatColor(index)
    val hero = Art.hero(seat.heroId)
    val hand = deal?.hands?.firstOrNull { it.seat == seat.id }?.atoms.orEmpty()
    val puzzle = SessionViewModel.SYNC_PATTERN
    val held = puzzle.atoms.filter { it.id in hand }
    val hidden = puzzle.atoms.filterNot { it.id in hand }

    PaintedScreen(Art.plateChamber, Scrims.chamber) {
        SectionLabel("Viewing as — tap to switch phone", CrystalPalette.StoneLight)
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            seats.forEachIndexed { i, s ->
                val c = seatColor(i)
                val on = s.id == viewing
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CrystalShapes.chip)
                        .background(if (on) c.copy(alpha = 0.22f) else Color(0x66101820))
                        .border(if (on) 2.dp else 1.dp, if (on) c else Color(0x3DB8B2C6), CrystalShapes.chip)
                        .clickable { onView(s.id) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Portrait(Art.hero(s.heroId).portrait, if (on) c else Color(0x66B8B2C6), 30)
                        Text(
                            s.displayName.take(9),
                            color = if (on) c else CrystalPalette.Muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Portrait(hero.portrait, accent, 58)
            Spacer(Modifier.size(12.dp))
            Column {
                Text(seat.displayName, color = CrystalPalette.Bone, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text(hero.title, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            GlassPanel(Modifier.fillMaxWidth(), accent.copy(alpha = 0.5f)) {
                SectionLabel("You will see", accent)
                held.forEach { atom ->
                    AtomLine(atom.interrogative.label, atom.label, accent, known = true)
                }
            }
            GlassPanel(Modifier.fillMaxWidth(), CrystalPalette.WrathRed.copy(alpha = 0.4f)) {
                SectionLabel("You will never see", CrystalPalette.WrathRed)
                hidden.forEach { atom ->
                    AtomLine(atom.interrogative.label, atom.label, CrystalPalette.WrathRed, known = false)
                }
            }
            ParchmentCard(Modifier.fillMaxWidth()) {
                Text(
                    "This is not a hint system. The other pieces do not exist on this phone. " +
                        "The only way to get them is to ask, out loud, in the room.",
                    color = CrystalPalette.ParchmentInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp,
                )
            }
        }

        CrystalButton("Close", Modifier.fillMaxWidth(), ButtonStyle.Ghost, 52, onBack)
    }
}

@Composable
private fun AtomLine(interrogative: String, label: String, color: Color, known: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(
            if (known) "✓" else "✕",
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(end = 8.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                interrogative.uppercase(),
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
            Text(
                label,
                color = if (known) CrystalPalette.Bone else CrystalPalette.Muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
            )
        }
    }
}
