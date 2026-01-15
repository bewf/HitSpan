package me.bewf.hitspan.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Checkbox;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.annotations.Number;
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
            category = "General",
            subcategory = "Decay"
    )
    public int decayTimeMs = 2000;

    @Checkbox(
            name = "Hide on Decay",
            description = "Hide the HUD when decayed (otherwise it shows 0.00)",
            category = "General",
            subcategory = "Decay"
    )
    public boolean hideOnDecay = false;

    @Checkbox(
            name = "Players Only",
            description = "Only track hits on players",
            category = "General",
            subcategory = "Filters"
    )
    public boolean playersOnly = true;

    // -------------------------
    // Range
    // -------------------------

    @Checkbox(
            name = "Confirmed Hit Only",
            description = "Only update Range when a hit is confirmed",
            category = "Range",
            subcategory = "Confirmation"
    )
    public boolean confirmRangeOnHitConfirm = true;

    @Checkbox(
            name = "Confirm Cooldown",
            description = "Blocks repeated confirms on the same target for a short time after a confirmed hit",
            category = "Range",
            subcategory = "Confirmation"
    )
    public boolean confirmCooldownEnabled = true;

    @Number(
            name = "Confirm Cooldown (ms)",
            description = "Cooldown after a confirmed hit",
            min = 0, max = 2000,
            category = "Range",
            subcategory = "Confirmation"
    )
    public int confirmCooldownMs = 500;

    @Checkbox(
            name = "Vertical Confirm",
            description = "Tiny vertical movement check (helps sometimes, can be turned off)",
            category = "Range",
            subcategory = "Confirmation"
    )
    public boolean verticalConfirmEnabled = true;

    @Number(
            name = "Vertical Threshold",
            description = "Minimum vertical movement to count as confirmed",
            min = 0, max = 1,
            category = "Range",
            subcategory = "Confirmation"
    )
    public float verticalConfirmThreshold = 0.05f;

    // -------------------------
    // Range -> Dynamic Range
    // -------------------------

    @Checkbox(
            name = "Enabled",
            description = "Green/yellow/red based on distance",
            category = "Range",
            subcategory = "Dynamic Range"
    )
    public boolean dynamicRange = true;

    @Number(
            name = "Green Minimum",
            description = "Green if range >= this",
            min = 0, max = 10,
            category = "Range",
            subcategory = "Dynamic Range"
    )
    public float rangeGreenMin = 2.7f;

    @Number(
            name = "Yellow Minimum",
            description = "Yellow if range >= this",
            min = 0, max = 10,
            category = "Range",
            subcategory = "Dynamic Range"
    )
    public float rangeYellowMin = 1.5f;

    // -------------------------
    // HUDs
    // -------------------------

    @HUD(name = "Range HUD", category = "HUD")
    public final RangeHud rangeHud = new RangeHud();

    @HUD(name = "Knockback HUD", category = "HUD")
    public final KnockbackHud knockbackHud = new KnockbackHud();

    private HitSpanConfig() {
        super(
                new Mod(
                        "HitSpan",
                        ModType.UTIL_QOL,
                        "/assets/hitspan/icon3.png"
                ),
                "hitspan.json"
        );

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
