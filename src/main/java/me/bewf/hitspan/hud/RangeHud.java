package me.bewf.hitspan.hud;

import cc.polyfrost.oneconfig.hud.SingleTextHud;
import me.bewf.hitspan.RangeTracker;
import me.bewf.hitspan.config.HitSpanConfig;

import java.text.DecimalFormat;

public class RangeHud extends SingleTextHud {

    private static final transient DecimalFormat DF = new DecimalFormat("0.00");

    public RangeHud() {
        super("Range", true); // this becomes the left label in the HUD
    }

    @Override
    protected String getText(boolean example) {
        if (example) return "3.00";

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return "";

        long now = System.currentTimeMillis();

        if (cfg.hideOnDecay) {
            if (RangeTracker.lastRangeTimeMs == 0L) return "";
            if (now - RangeTracker.lastRangeTimeMs > cfg.decayTimeMs) return "";
        }

        double val = RangeTracker.lastRange;
        if (RangeTracker.lastRangeTimeMs == 0L) val = 0.0;
        if (now - RangeTracker.lastRangeTimeMs > cfg.decayTimeMs) val = 0.0;
        if (val < 0.0) val = 0.0;

        // If you want dynamic color IN the text itself, use § codes here.
        if (cfg.dynamicRange && val > 0) {
            if (val >= cfg.rangeGreenMin) return "§a" + DF.format(val);
            if (val >= cfg.rangeYellowMin) return "§e" + DF.format(val);
            return "§c" + DF.format(val);
        }

        return DF.format(val);
    }
}
