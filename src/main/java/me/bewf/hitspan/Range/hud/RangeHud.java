// src/main/java/me/bewf/hitspan/Range/hud/RangeHud.java
package me.bewf.hitspan.Range.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import me.bewf.hitspan.Range.util.RangeTracker;
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
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return;
        if (cfg.rangeHud == null || !cfg.rangeHud.isEnabled()) return;

        String label = cfg.rangeLabel != null ? cfg.rangeLabel : "Range: ";

        if (example) {
            lines.add(label + "§a" + DF.format(3.00));
            return;
        }

        long now = System.currentTimeMillis();

        if (cfg.rangeHideOnDecay) {
            if (RangeTracker.lastRangeTimeMs == 0L) return;
            if (now - RangeTracker.lastRangeTimeMs > cfg.rangeDecayTimeMs) return;
        }

        double val = RangeTracker.lastRange;
        if (RangeTracker.lastRangeTimeMs == 0L) val = 0.0;
        if (now - RangeTracker.lastRangeTimeMs > cfg.rangeDecayTimeMs) val = 0.0;
        if (val < 0.0) val = 0.0;

        String num = DF.format(val);

        if (cfg.dynamicRange && val > 0) {
            OneColor c;
            if (val >= cfg.rangeFarMin) c = cfg.rangeFarColor;
            else if (val >= cfg.rangeMediumMin) c = cfg.rangeMediumColor;
            else c = cfg.rangeCloseColor;

            num = nearestMcColorCode(c) + num;
        }

        lines.add(label + num);
    }

    private static String nearestMcColorCode(OneColor c) {
        if (c == null) return "§f";

        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();

        final char[] codes = new char[]{'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        final int[][] cols = new int[][]{
                {0,   0,   0},
                {0,   0,   170},
                {0,   170, 0},
                {0,   170, 170},
                {170, 0,   0},
                {170, 0,   170},
                {255, 170, 0},
                {170, 170, 170},
                {85,  85,  85},
                {85,  85,  255},
                {85,  255, 85},
                {85,  255, 255},
                {255, 85,  85},
                {255, 85,  255},
                {255, 255, 85},
                {255, 255, 255}
        };

        int best = 15;
        int bestDist = Integer.MAX_VALUE;

        for (int i = 0; i < cols.length; i++) {
            int dr = r - cols[i][0];
            int dg = g - cols[i][1];
            int db = b - cols[i][2];
            int dist = dr * dr + dg * dg + db * db;

            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }

        return "§" + codes[best];
    }
}
