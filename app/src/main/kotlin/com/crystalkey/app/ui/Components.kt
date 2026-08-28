package com.crystalkey.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crystalkey.app.theme.CrystalPalette
import com.crystalkey.app.theme.CrystalShapes

/**
 * The 3D bevel button from the design sheet: a gradient face, a lighter top
 * sheen, a coloured border and a hard 6dp drop that reads as depth rather than
 * as a shadow. Uppercase, wide-tracked, white — a child should be able to spot
 * the primary action across a room.
 */
@Composable
fun CrystalButton(
    label: String,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    height: Int = 62,
    onClick: () -> Unit = {},
) {
    val drop = style.drop
    Box(modifier = modifier.height((height + 6).dp)) {
        // the hard drop, drawn as a solid block behind the face
        Box(
            Modifier
                .fillMaxWidth()
                .height(height.dp)
                .offset(y = 6.dp)
                .clip(CrystalShapes.button)
                .background(drop),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(CrystalShapes.button)
                .background(Brush.verticalGradient(style.face))
                .border(2.dp, style.border, CrystalShapes.button)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            // top sheen
            Box(
                Modifier
                    .fillMaxWidth()
                    .height((height * 0.46f).dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        ),
                    ),
            )
            Text(
                text = label.uppercase(),
                color = style.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.05.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
    }
}

enum class ButtonStyle(
    val face: List<Color>,
    val border: Color,
    val drop: Color,
    val text: Color,
) {
    Primary(CrystalPalette.teal, Color(0xFF59C4D2), Color(0xFF0E4650), Color.White),
    Secondary(CrystalPalette.orange, Color(0xFFF9B45C), Color(0xFF7E3410), Color.White),
    Task(CrystalPalette.purple, Color(0xFFA78EE4), Color(0xFF372868), Color.White),
    Ghost(
        listOf(Color(0x800E1A1E), Color(0x800E1A1E)),
        Color(0x6BA8EAF0),
        Color.Transparent,
        Color(0xFFA8EAF0),
    ),
    Danger(
        listOf(Color(0xFFF0553A), Color(0xFF9E2314)),
        Color(0xFFFF8A72),
        Color(0xFF6E1509),
        Color.White,
    ),
}

/** Glass — live state layered over painted art. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0x4DB8B2C6),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(CrystalShapes.panel)
            .background(Color(0xB8101820))
            .border(2.dp, borderColor, CrystalShapes.panel)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** A parchment card: cream fill, tan 3dp edge, hard drop. */
@Composable
fun ParchmentCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(CrystalShapes.parchment)
                .background(Brush.linearGradient(listOf(Color(0xFFFBF2DE), Color(0xFFEBD9AE))))
                .border(3.dp, CrystalPalette.ParchmentEdge, CrystalShapes.parchment)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

/** The uppercase, wide-tracked section label used everywhere in the design. */
@Composable
fun SectionLabel(text: String, color: Color = Color(0xFFA8EAF0), modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.7.sp,
        modifier = modifier,
    )
}

/** A meter with a hard track and a glowing fill — Lantern, Wrath and boss HP. */
@Composable
fun Meter(
    fraction: Float,
    modifier: Modifier = Modifier,
    fill: List<Color> = listOf(Color(0xFFFF8A6E), Color(0xFFC42A18)),
    track: Color = Color(0xFF2A1418),
    border: Color = Color(0xFF6E2A20),
    height: Int = 22,
    label: String? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(track)
            .border(2.dp, border, RoundedCornerShape((height / 2).dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height.dp)
                .background(Brush.horizontalGradient(fill)),
        )
        if (label != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    label.uppercase(),
                    color = Color(0xFFFFE4DC),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp,
                )
            }
        }
    }
}
