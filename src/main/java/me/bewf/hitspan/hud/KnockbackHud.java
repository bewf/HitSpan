package me.bewf.hitspan.hud;

import cc.polyfrost.oneconfig.hud.SingleTextHud;
import me.bewf.hitspan.KnockbackTracker;
import me.bewf.hitspan.config.HitSpanConfig;

import java.text.DecimalFormat;

public class KnockbackHud extends SingleTextHud {

    private static final transient DecimalFormat DF = new DecimalFormat("0.00");

    public KnockbackHud() {
        super("KB", true); // label shown on the left
    }

    @Override
    protected String getText(boolean example) {
        if (example) return "0.40";

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return "";

        long now = System.currentTimeMillis();

        if (cfg.hideOnDecay) {
            if (KnockbackTracker.lastKBTimeMs == 0L) return "";
            if (now - KnockbackTracker.lastKBTimeMs > cfg.decayTimeMs) return "";
        }

        double val = KnockbackTracker.lastKB;
        if (KnockbackTracker.lastKBTimeMs == 0L) val = 0.0;
        if (now - KnockbackTracker.lastKBTimeMs > cfg.decayTimeMs) val = 0.0;
        if (val < 0.0) val = 0.0;

        return DF.format(val);
    }
}
