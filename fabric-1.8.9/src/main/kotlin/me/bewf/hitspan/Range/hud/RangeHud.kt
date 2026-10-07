package me.bewf.hitspan.Range.hud

import me.bewf.hitspan.Range.util.RangeTracker
import me.bewf.hitspan.hud.HitSpanTextHud
import me.bewf.hitspan.hud.HitSpanTextHud.Shown
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.config.v1.annotations.DependsOn
import org.polyfrost.oneconfig.api.config.v1.annotations.Number
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import java.util.Locale

class RangeHud : HitSpanTextHud("hitspan_range_hud.json", "Range HUD", "Range: ") {

    @Number(
        title = "Decay Time (ms)",
        description = "How long before Range resets to 0.00 (or the HUD hides).",
        min = 0f, max = 10000f,
        subcategory = "General"
    )
    var decayTimeMs: Int = 2000

    @Switch(
        title = "Hide on Decay",
        description = "Hide the HUD completely once decayed, instead of showing 0.00.",
        subcategory = "General"
    )
    var hideOnDecay: Boolean = false

    @Switch(
        title = "Confirmed Hit Only",
        description = "Only update Range when the server confirms a hurt (packet or client hurt state).",
        subcategory = "General"
    )
    var confirmedHitOnly: Boolean = true

    @Switch(
        title = "Players Only",
        description = "Only track hits on players.",
        subcategory = "General"
    )
    var playersOnly: Boolean = true

    @Switch(
        title = "Dynamic Range",
        description = "Colors the number by how far the hit was.",
        subcategory = "Dynamic Range"
    )
    var dynamicRange: Boolean = true

    @Color(title = "Far Color", description = "Used when range is at least Far Min.", subcategory = "Dynamic Range")
    @DependsOn("dynamicRange")
    var farColor: PolyColor = PolyColor(0xFF55FF55.toInt())

    @Number(
        title = "Far Min",
        description = "Far color if range >= this.",
        min = 0f, max = 10f,
        subcategory = "Dynamic Range"
    )
    @DependsOn("dynamicRange")
    var farMin: Float = 2.7f

    @Color(title = "Medium Color", description = "Used when range is at least Medium Min.", subcategory = "Dynamic Range")
    @DependsOn("dynamicRange")
    var mediumColor: PolyColor = PolyColor(0xFFFFFF55.toInt())

    @Number(
        title = "Medium Min",
        description = "Medium color if range >= this.",
        min = 0f, max = 10f,
        subcategory = "Dynamic Range"
    )
    @DependsOn("dynamicRange")
    var mediumMin: Float = 1.5f

    @Color(title = "Close Color", description = "Used below Medium Min.", subcategory = "Dynamic Range")
    @DependsOn("dynamicRange")
    var closeColor: PolyColor = PolyColor(0xFFFF5555.toInt())

    override val description: String? = "Distance of your last confirmed hit."

    override fun compute(example: Boolean): Shown? {
        if (example) return Shown("0.00")

        val now = System.currentTimeMillis()
        val last = RangeTracker.lastRangeTimeMs
        val decayed = last == 0L || now - last > decayTimeMs

        if (decayed && hideOnDecay) return null

        var v = if (decayed) 0.0 else RangeTracker.lastRange
        if (v < 0.0) v = 0.0

        val text = String.format(Locale.US, "%.2f", v)

        if (dynamicRange && v > 0.0) {
            val c = when {
                v >= farMin -> farColor
                v >= mediumMin -> mediumColor
                else -> closeColor
            }
            return Shown(text, c)
        }
        return Shown(text)
    }
}
