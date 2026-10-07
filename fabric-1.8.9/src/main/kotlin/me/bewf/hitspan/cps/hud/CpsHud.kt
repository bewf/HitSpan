package me.bewf.hitspan.cps.hud

import me.bewf.hitspan.cps.util.CpsTracker
import me.bewf.hitspan.hud.HitSpanTextHud
import me.bewf.hitspan.hud.HitSpanTextHud.Shown
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.Number
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import java.util.Locale

class CpsHud : HitSpanTextHud("hitspan_cps_hud.json", "CPS HUD", "CPS: ") {

    @Switch(
        title = "Hide on Zero",
        description = "Hide the HUD completely when you are not clicking.",
        subcategory = "General"
    )
    var hideOnZero: Boolean = false

    @Dropdown(
        title = "Mode",
        description = "Which clicks to show.",
        options = ["Left", "Right", "Both"],
        subcategory = "General"
    )
    var mode: Int = 2

    @Number(
        title = "Average Window (seconds)",
        description = "Averages your CPS over this many seconds. 1 = clicks in the last second.",
        min = 1f, max = 5f,
        subcategory = "General"
    )
    var averageSeconds: Int = 1

    @Switch(
        title = "Show Decimals",
        description = "Show CPS with one decimal place. Only useful when Average Window is above 1.",
        subcategory = "General"
    )
    var showDecimals: Boolean = false

    override val description: String? = "Clicks per second."

    override fun compute(example: Boolean): Shown? {
        if (example) return Shown(if (mode == 2) "0 | 0" else "0")

        val l = CpsTracker.getLeftCpsFloat()
        val r = CpsTracker.getRightCpsFloat()

        if (hideOnZero) {
            if (mode == 0 && l == 0f) return null
            if (mode == 1 && r == 0f) return null
            if (mode == 2 && l == 0f && r == 0f) return null
        }

        val text = when (mode) {
            0 -> format(l)
            1 -> format(r)
            else -> format(l) + " | " + format(r)
        }
        return Shown(text)
    }

    private fun format(v: Float): String =
        if (!showDecimals) Math.round(v).toString() else String.format(Locale.US, "%.1f", v)
}
