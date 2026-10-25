package me.bewf.hitspan.hud

import me.bewf.hitspan.Knockback.hud.KnockbackHud
import me.bewf.hitspan.Range.hud.RangeHud
import me.bewf.hitspan.combo.hud.ComboHud
import me.bewf.hitspan.cps.hud.CpsHud
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager

object HitSpanHuds {

    @JvmStatic fun range(): RangeHud? = find(RangeHud::class.java)
    @JvmStatic fun knockback(): KnockbackHud? = find(KnockbackHud::class.java)
    @JvmStatic fun combo(): ComboHud? = find(ComboHud::class.java)
    @JvmStatic fun cps(): CpsHud? = find(CpsHud::class.java)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Hud> find(cls: Class<T>): T? =
        HudManager.getHudsOfType(cls).firstOrNull() ?: (HudManager.getProvider(cls) as? T)
}
