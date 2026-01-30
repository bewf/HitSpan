// src/main/java/me/bewf/hitspan/cps/hud/CpsHud.java
package me.bewf.hitspan.cps.hud;

import cc.polyfrost.oneconfig.hud.TextHud;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.cps.util.CpsTracker;

import java.util.List;
import java.util.Locale;

public class CpsHud extends TextHud {

    public CpsHud() {
        super(true);
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.cpsHudEnabled) return;

        String label = cfg.cpsLabel != null ? cfg.cpsLabel : "CPS: ";

        if (example) {
            lines.add(label + "6.4 | 7.1");
            return;
        }

        boolean dec = cfg.cpsShowDecimals;

        float l = CpsTracker.getLeftCpsFloat();
        float r = CpsTracker.getRightCpsFloat();

        if (cfg.cpsHideOnZero) {
            if (cfg.cpsMode == 0 && l == 0f) return;
            if (cfg.cpsMode == 1 && r == 0f) return;
            if (cfg.cpsMode == 2 && l == 0f && r == 0f) return;
        }

        String out;
        if (cfg.cpsMode == 0) {
            out = format(l, dec);
        } else if (cfg.cpsMode == 1) {
            out = format(r, dec);
        } else {
            out = format(l, dec) + " | " + format(r, dec);
        }

        lines.add(label + out);
    }

    private static String format(float v, boolean dec) {
        if (!dec) return Integer.toString(Math.round(v));
        return String.format(Locale.US, "%.1f", v);
    }
}
