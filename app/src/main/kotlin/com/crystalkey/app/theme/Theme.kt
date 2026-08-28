package com.crystalkey.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The palette from the design system sheet, one-for-one.
 *
 * Each family colour belongs to an interrogative, not to a person — the seat
 * that holds WHERE is green whoever is sitting there. Wrath red is reserved:
 * it never appears except under boss pressure and failure, so that when it does
 * appear it means something.
 */
object CrystalPalette {
    val NatureGreen = Color(0xFF4FBF62)
    val AncientTeal = Color(0xFF5FC9D6)
    val SunsetOrange = Color(0xFFF08A33)
    val StonePurple = Color(0xFF8B72D4)
    val LanternGold = Color(0xFFFFD778)
    val Rose = Color(0xFFE88AA8)
    val WrathRed = Color(0xFFEE6E5C)

    val Parchment = Color(0xFFFBF2DE)
    val ParchmentEdge = Color(0xFFC9AC72)
    val ParchmentInk = Color(0xFF5E3F18)

    val NightGround = Color(0xFF101820)
    val Void = Color(0xFF0C0A14)
    val StoneGrey = Color(0xFF6B6580)
    val StoneLight = Color(0xFFB8B2C6)
    val Bone = Color(0xFFF2E4C4)
    val Muted = Color(0xFF9A94A8)

    val teal = listOf(Color(0xFF2E9AA8), Color(0xFF16606E))
    val orange = listOf(Color(0xFFF08A33), Color(0xFFB14A17))
    val purple = listOf(Color(0xFF7A5BC4), Color(0xFF4E3A90))
}

/** Seats are coloured by the interrogative they hold this round, never by name. */
fun seatColor(index: Int): Color = when (index % 6) {
    0 -> CrystalPalette.NatureGreen
    1 -> CrystalPalette.AncientTeal
    2 -> CrystalPalette.SunsetOrange
    3 -> CrystalPalette.StonePurple
    4 -> CrystalPalette.LanternGold
    else -> CrystalPalette.Rose
}

private val CrystalColors = darkColorScheme(
    primary = CrystalPalette.AncientTeal,
    onPrimary = Color.White,
    secondary = CrystalPalette.SunsetOrange,
    onSecondary = Color.White,
    tertiary = CrystalPalette.StonePurple,
    background = CrystalPalette.Void,
    onBackground = CrystalPalette.Bone,
    surface = CrystalPalette.NightGround,
    onSurface = CrystalPalette.Bone,
    error = CrystalPalette.WrathRed,
)

/**
 * Type roles map straight onto the sheet: display for the Cinzel-style titles,
 * body for anything a nine-year-old reads aloud, and a monospace role for every
 * number that ticks. Bundling the actual fonts is a follow-up — the weights and
 * sizes are what the layouts depend on.
 */
val CrystalTypography = Typography(
    displayLarge = TextStyle(fontSize = 35.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp),
    displayMedium = TextStyle(fontSize = 27.sp, fontWeight = FontWeight.Black),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.05.sp),
    labelSmall = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp),
)

object CrystalShapes {
    val button = RoundedCornerShape(19.dp)
    val panel = RoundedCornerShape(20.dp)
    val parchment = RoundedCornerShape(22.dp)
    val chip = RoundedCornerShape(14.dp)
    val avatar = RoundedCornerShape(50)
    val system = RoundedCornerShape(15.dp)
}

@Composable
fun CrystalKeyTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The world is a night forest either way; there is no light variant.
    MaterialTheme(colorScheme = CrystalColors, typography = CrystalTypography, content = content)
}
