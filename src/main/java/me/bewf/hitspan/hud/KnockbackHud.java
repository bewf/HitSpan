package me.bewf.hitspan.hud;

import cc.polyfrost.oneconfig.hud.TextHud;
import me.bewf.hitspan.KnockbackTracker;
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
        if (example) {
            lines.add("KB: 0.40");
            return;
        }

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return;

        long now = System.currentTimeMillis();

        if (cfg.hideOnDecay) {
            if (KnockbackTracker.lastKBTimeMs == 0L) return;
            if (now - KnockbackTracker.lastKBTimeMs > cfg.decayTimeMs) return;
        }

        double val = KnockbackTracker.lastKB;
        if (KnockbackTracker.lastKBTimeMs == 0L) val = 0.0;
        if (now - KnockbackTracker.lastKBTimeMs > cfg.decayTimeMs) val = 0.0;
        if (val < 0.0) val = 0.0;

        lines.add("KB: " + DF.format(val));
    }
}
