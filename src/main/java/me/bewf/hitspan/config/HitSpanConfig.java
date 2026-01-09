package me.bewf.hitspan.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Checkbox;
import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import me.bewf.hitspan.hud.KnockbackHud;
import me.bewf.hitspan.hud.RangeHud;

public class HitSpanConfig extends Config {

    public static HitSpanConfig INSTANCE;

    // -------------------------
    // General
    // -------------------------

    @Number(
            name = "Decay Time (ms)",
            description = "How long before values reset or the HUD hides",
            min = 0, max = 10000,
            category = "General"
    )
    public int decayTimeMs = 2000;

    @Checkbox(
            name = "Hide on Decay",
            description = "Hide the HUD when decayed (otherwise it shows 0.00)",
            category = "General"
    )
    public boolean hideOnDecay = false;

    @Checkbox(
            name = "Players Only",
            description = "Only track hits on players",
            category = "General"
    )
    public boolean playersOnly = true;

    // -------------------------
    // Range
    // -------------------------

    @Checkbox(
            name = "Dynamic Range",
            description = "Change the range text color based on distance",
            category = "Range"
    )
    public boolean dynamicRange = true;

    @Number(
            name = "Green Range Minimum",
            description = "Green if range >= this",
            min = 0, max = 10,
            category = "Range"
    )
    public float rangeGreenMin = 2.7f;

    @Number(
            name = "Yellow Range Minimum",
            description = "Yellow if range >= this",
            min = 0, max = 10,
            category = "Range"
    )
    public float rangeYellowMin = 1.5f;

    @Color(
            name = "Green Color",
            description = "Color for good range",
            category = "Range"
    )
    public OneColor rangeGreenColor = new OneColor(0xFF55FF55);

    @Color(
            name = "Yellow Color",
            description = "Color for medium range",
            category = "Range"
    )
    public OneColor rangeYellowColor = new OneColor(0xFFFFFF55);

    @Color(
            name = "Red Color",
            description = "Color for bad range",
            category = "Range"
    )
    public OneColor rangeRedColor = new OneColor(0xFFFF5555);

    // -------------------------
    // HUDs (this is what makes the HUD editor sections appear)
    // -------------------------

    @HUD(name = "Range HUD", category = "HUD")
    public final RangeHud rangeHud = new RangeHud();

    @HUD(name = "Knockback HUD", category = "HUD")
    public final KnockbackHud knockbackHud = new KnockbackHud();

    private HitSpanConfig() {
        super(new Mod("HitSpan", ModType.UTIL_QOL), "hitspan.json");
        try {
            initialize();
        } catch (NoClassDefFoundError e) {
            System.err.println("OneConfig internal classes not found; skipping config initialization: " + e.getMessage());
        }
    }

    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new HitSpanConfig();
        }
    }

    public static void saveConfig() {
        if (INSTANCE != null) {
            INSTANCE.save();
        }
    }
}
