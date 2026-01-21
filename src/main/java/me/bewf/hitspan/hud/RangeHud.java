package me.bewf.hitspan.hud;

import cc.polyfrost.oneconfig.hud.TextHud;
import me.bewf.hitspan.RangeTracker;
import me.bewf.hitspan.config.HitSpanConfig;

import java.text.DecimalFormat;
import java.util.List;

public class RangeHud extends TextHud {

    private static final transient DecimalFormat DF = new DecimalFormat("0.00");

    public RangeHud() {
        super(true);
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("Range: 3.00");
            return;
        }

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return;

        long now = System.currentTimeMillis();

        if (cfg.hideOnDecay) {
            if (RangeTracker.lastRangeTimeMs == 0L) return;
            if (now - RangeTracker.lastRangeTimeMs > cfg.decayTimeMs) return;
        }

        double val = RangeTracker.lastRange;
        if (RangeTracker.lastRangeTimeMs == 0L) val = 0.0;
        if (now - RangeTracker.lastRangeTimeMs > cfg.decayTimeMs) val = 0.0;
        if (val < 0.0) val = 0.0;

        String num;
        if (cfg.dynamicRange && val > 0) {
            if (val >= cfg.rangeGreenMin) num = "§a" + DF.format(val);
            else if (val >= cfg.rangeYellowMin) num = "§e" + DF.format(val);
            else num = "§c" + DF.format(val);
        } else {
            num = DF.format(val);
        }

        lines.add("Range: " + num);
    }
}
