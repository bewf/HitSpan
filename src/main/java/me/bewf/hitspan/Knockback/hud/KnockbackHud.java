// src/main/java/me/bewf/hitspan/Knockback/hud/KnockbackHud.java
package me.bewf.hitspan.Knockback.hud;

import cc.polyfrost.oneconfig.hud.TextHud;
import me.bewf.hitspan.Knockback.util.KnockbackTracker;
import me.bewf.hitspan.config.HitSpanConfig;

import java.text.DecimalFormat;
import java.util.List;

public class KnockbackHud extends TextHud {

    private static final transient DecimalFormat DF = new DecimalFormat("0.00");

    public KnockbackHud() {
        super(true);
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return;
        if (cfg.knockbackHud == null || !cfg.knockbackHud.isEnabled()) return;

        String label = cfg.knockbackLabel != null ? cfg.knockbackLabel : "KB: ";

        if (example) {
            lines.add(label + "0.40");
            return;
        }

        long now = System.currentTimeMillis();

        if (cfg.knockbackHideOnDecay) {
            if (KnockbackTracker.lastKBTimeMs == 0L) return;
            if (now - KnockbackTracker.lastKBTimeMs > cfg.knockbackDecayTimeMs) return;
        }

        double val = KnockbackTracker.lastKB;
        if (KnockbackTracker.lastKBTimeMs == 0L) val = 0.0;
        if (now - KnockbackTracker.lastKBTimeMs > cfg.knockbackDecayTimeMs) val = 0.0;
        if (val < 0.0) val = 0.0;

        lines.add(label + DF.format(val));
    }
}
