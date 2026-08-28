package com.crystalkey.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.crystalkey.app.theme.CrystalShapes

/**
 * The painted-plate + scrim + vector-UI sandwich every screen is built on.
 *
 * This is the seam the whole art direction rests on: a full-bleed painted
 * background, a gradient scrim tuned per screen so text stays legible over it,
 * and every word and control drawn as vector on top. Without the plate the
 * screens are just dark rectangles — which is exactly what they were before
 * the art was bundled.
 */
@Composable
fun PaintedScreen(
    @DrawableRes plate: Int,
    scrim: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(plate),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(scrim)))
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 44.dp, bottom = 26.dp),
            content = content,
        )
    }
}

/** Scrims tuned per plate, straight from the design canvas. */
object Scrims {
    val trail = listOf(
        Color(0x800A120E), Color(0x140A120E), Color(0x8C0A120E), Color(0xF0080E0C),
    )
    val campfire = listOf(
        Color(0xE6160C06), Color(0x4D160C06), Color(0xF00E0604),
    )
    val chamber = listOf(
        Color(0xDB120A1C), Color(0x4D120A1C), Color(0xF70A0610),
    )
    val arena = listOf(
        Color(0xDB061216), Color(0x33061216), Color(0xEE050E12),
    )
}

/**
 * A circular portrait in a coloured ring. `objectPosition: center 12%` in the
 * design becomes a crop bias here — the busts are painted head-high, so the
 * circle is filled from the top of the image rather than its centre.
 */
@Composable
fun Portrait(
    @DrawableRes portrait: Int,
    ring: Color,
    size: Int = 44,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(size.dp)
            .clip(CrystalShapes.avatar)
            .background(Color(0xFF1B2630))
            .border(2.dp, ring, CrystalShapes.avatar),
    ) {
        Image(
            painter = painterResource(portrait),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
