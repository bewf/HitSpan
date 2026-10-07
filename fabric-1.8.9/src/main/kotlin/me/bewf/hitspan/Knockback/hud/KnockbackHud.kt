package me.bewf.hitspan.Knockback.hud

import me.bewf.hitspan.Knockback.util.KnockbackTracker
import me.bewf.hitspan.hud.HitSpanTextHud
import me.bewf.hitspan.hud.HitSpanTextHud.Shown
import org.polyfrost.oneconfig.api.config.v1.annotations.Number
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import java.util.Locale

class KnockbackHud : HitSpanTextHud("hitspan_knockback_hud.json", "Knockback HUD", "KB: ") {

    @Number(
        title = "Decay Time (ms)",
        description = "How long before Knockback resets to 0.00 (or the HUD hides).",
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
        title = "Players Only",
        description = "Only track knockback on players.",
        subcategory = "General"
    )
    var playersOnly: Boolean = true

    override val description: String? = "How far your last hit knocked the target back."

    override fun compute(example: Boolean): Shown? {
        if (example) return Shown("0.00")

        val now = System.currentTimeMillis()
        val last = KnockbackTracker.lastKBTimeMs
        val decayed = last == 0L || now - last > decayTimeMs

        if (decayed && hideOnDecay) return null

        var v = if (decayed) 0.0 else KnockbackTracker.lastKB
        if (v < 0.0) v = 0.0

        return Shown(String.format(Locale.US, "%.2f", v))
    }
}
