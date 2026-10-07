package me.bewf.hitspan.combo.hud

import me.bewf.hitspan.combo.util.ComboTracker
import me.bewf.hitspan.hud.HitSpanTextHud
import me.bewf.hitspan.hud.HitSpanTextHud.Shown
import org.polyfrost.oneconfig.api.config.v1.annotations.Number
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch

class ComboHud : HitSpanTextHud("hitspan_combo_hud.json", "Combo HUD", "Combo: ") {

    @Number(
        title = "Hide Under",
        description = "Hide the HUD while the combo is under this value. Set to 0 to always show.",
        min = 0f, max = 100f,
        subcategory = "General"
    )
    var hideUnder: Int = 3

    @Number(
        title = "Reset Time (seconds)",
        description = "Seconds without a hit before the combo resets to 0. The combo also resets when you get hit.",
        min = 1f, max = 60f,
        subcategory = "General"
    )
    var resetTimeSeconds: Int = 5

    @Switch(
        title = "Players Only",
        description = "Only count combos on players.",
        subcategory = "General"
    )
    var playersOnly: Boolean = true

    override val description: String? = "Your current hit combo."

    override fun compute(example: Boolean): Shown? {
        if (example) return Shown("0")

        val combo = ComboTracker.combo
        if (hideUnder > 0 && combo < hideUnder) return null

        return Shown(combo.toString())
    }
}
