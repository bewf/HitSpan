// src/main/java/me/bewf/hitspan/combo/hud/ComboHud.java
package me.bewf.hitspan.combo.hud;

import cc.polyfrost.oneconfig.hud.TextHud;
import me.bewf.hitspan.combo.util.ComboTracker;
import me.bewf.hitspan.config.HitSpanConfig;

import java.util.List;

public class ComboHud extends TextHud {

    public ComboHud() {
        super(true);
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return;
        if (!cfg.comboHudEnabled || cfg.comboHud == null || !cfg.comboHud.isEnabled()) return;

        String label = cfg.comboLabel != null ? cfg.comboLabel : "Combo: ";

        if (example) {
            lines.add(label + "5");
            return;
        }

        int val = ComboTracker.combo;

        // Hide HUD if combo is zero and option is enabled
        if (cfg.comboHideOnZero && val == 0) return;

        // After 10 seconds without hits, reset to 0
        long now = System.currentTimeMillis();
        if (ComboTracker.lastHitTimeMs > 0 && now - ComboTracker.lastHitTimeMs > 10000L) {
            val = 0;
        }

        lines.add(label + val);
    }
}
