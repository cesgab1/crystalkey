package com.crystalkey.app.ui

import androidx.annotation.DrawableRes
import com.crystalkey.app.R
import com.crystalkey.core.AgeBand

/**
 * The bundled art, and the mapping from a seat's `heroId` to a drawable.
 *
 * Heroes are addressed by string id rather than resource id so that `core/`
 * never has to know Android exists — a seat carries `heroId = "bard"` and this
 * is the only place that turns it into a picture.
 */
object Art {

    @DrawableRes val plateTrail = R.drawable.plate_trail
    @DrawableRes val plateCampfire = R.drawable.plate_campfire
    @DrawableRes val plateChamber = R.drawable.plate_chamber
    @DrawableRes val plateArena = R.drawable.plate_arena
    @DrawableRes val cast = R.drawable.cast
    @DrawableRes val crystalKey = R.drawable.prop_crystal_key

    /** Every hero that ships with a portrait, in the order seats are filled. */
    val heroes: List<Hero> = listOf(
        Hero("mom", "The Cartographer", AgeBand.ADULT, R.drawable.bust_mom),
        Hero("dad", "The Guide", AgeBand.ADULT, R.drawable.bust_dad),
        Hero("leo", "The Navigator", AgeBand.CHILD, R.drawable.bust_leo),
        Hero("mia", "The Explorer", AgeBand.CHILD, R.drawable.bust_mia),
        Hero("loremaster", "The Loremaster", AgeBand.ADULT, R.drawable.bust_loremaster),
        Hero("bard", "The Bard", AgeBand.CHILD, R.drawable.bust_bard),
        Hero("ranger", "The Ranger", AgeBand.ADULT, R.drawable.bust_ranger),
        Hero("smith", "The Smith", AgeBand.ADULT, R.drawable.bust_smith),
        Hero("warden", "The Warden", AgeBand.TEEN, R.drawable.bust_warden),
        Hero("apprentice", "The Apprentice", AgeBand.TEEN, R.drawable.bust_apprentice),
        Hero("scout", "The Scout", AgeBand.TEEN, R.drawable.bust_scout),
        Hero("acolyte", "The Acolyte", AgeBand.CHILD, R.drawable.bust_acolyte),
        Hero("tinkerer", "The Tinkerer", AgeBand.CHILD, R.drawable.bust_tinkerer),
    )

    private val byId = heroes.associateBy { it.id }

    fun hero(id: String): Hero = byId[id] ?: heroes.first()

    @DrawableRes
    fun portrait(id: String): Int = hero(id).portrait

    /** The hero handed to the nth seat to join, wrapping if a party is huge. */
    fun heroForSeat(index: Int): Hero = heroes[index % heroes.size]
}

data class Hero(
    val id: String,
    val title: String,
    val band: AgeBand,
    @DrawableRes val portrait: Int,
)
