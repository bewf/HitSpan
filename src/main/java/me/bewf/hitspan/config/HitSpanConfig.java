// src/main/java/me/bewf/hitspan/config/HitSpanConfig.java
package me.bewf.hitspan.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Checkbox;
import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import me.bewf.hitspan.Knockback.hud.KnockbackHud;
import me.bewf.hitspan.Range.hud.RangeHud;
import me.bewf.hitspan.cps.hud.CpsHud;
import me.bewf.hitspan.combo.hud.ComboHud;


public class HitSpanConfig extends Config {

    public static HitSpanConfig INSTANCE;

    // =============================
    // Range
    // =============================

    @HUD(
            name = "Range HUD",
            category = "Range",
            subcategory = "HUD"
    )
    public final RangeHud rangeHud = new RangeHud();

    public boolean rangeHudEnabled = true;

    @Number(
            name = "Decay Time (ms)",
            description = "How long before Range resets or the HUD hides",
            min = 0, max = 10000,
            category = "Range",
            subcategory = "General"
    )
    public int rangeDecayTimeMs = 2000;

    @Checkbox(
            name = "Hide on Decay",
            description = "Hide the Range HUD when decayed (otherwise it shows 0.00)",
            category = "Range",
            subcategory = "General"
    )
    public boolean rangeHideOnDecay = false;

    @Text(
            name = "Label",
            description = "Text shown before the range value (example: \"Range: \")",
            category = "Range",
            subcategory = "General"
    )
    public String rangeLabel = "Range: ";

    @Checkbox(
            name = "Confirmed Hit Only",
            description = "Only update Range when the server confirms a hurt (packet or client hurt state).",
            category = "Range",
            subcategory = "General"
    )
    public boolean confirmRangeOnHitConfirm = true;

    @Checkbox(
            name = "Players Only",
            description = "Only track hits on players",
            category = "Range",
            subcategory = "General"
    )
    public boolean rangePlayersOnly = true;

    @Checkbox(
            name = "Enabled",
            description = "Selected colors are mapped to the closest Minecraft chat color.",
            category = "Range",
            subcategory = "Dynamic Range"
    )
    public boolean dynamicRange = true;

    @Color(
            name = "Far Color",
            description = "Mapped to closest Minecraft chat color.",
            category = "Range",
            subcategory = "Dynamic Range"
    )
    public OneColor rangeFarColor = new OneColor(85, 255, 85);

    @Number(
            name = "Far Min",
            description = "Far color if range >= this",
            min = 0, max = 10,
            category = "Range",
            subcategory = "Dynamic Range"
    )
    public float rangeFarMin = 2.7f;

    @Color(
            name = "Medium Color",
            description = "Mapped to closest Minecraft chat color.",
            category = "Range",
            subcategory = "Dynamic Range"
    )
    public OneColor rangeMediumColor = new OneColor(255, 255, 85);

    @Number(
            name = "Medium Min",
            description = "Medium color if range >= this",
            min = 0, max = 10,
            category = "Range",
            subcategory = "Dynamic Range"
    )
    public float rangeMediumMin = 1.5f;

    @Color(
            name = "Close Color",
            description = "Mapped to closest Minecraft chat color.",
            category = "Range",
            subcategory = "Dynamic Range"
    )
    public OneColor rangeCloseColor = new OneColor(255, 85, 85);

    // =============================
    // Knockback
    // =============================

    @HUD(
            name = "Knockback HUD",
            category = "Knockback",
            subcategory = "HUD"
    )
    public final KnockbackHud knockbackHud = new KnockbackHud();

    public boolean knockbackHudEnabled = true;

    @Number(
            name = "Decay Time (ms)",
            description = "How long before Knockback resets or the HUD hides",
            min = 0, max = 10000,
            category = "Knockback",
            subcategory = "General"
    )
    public int knockbackDecayTimeMs = 2000;

    @Checkbox(
            name = "Hide on Decay",
            description = "Hide the Knockback HUD when decayed (otherwise it shows 0.00)",
            category = "Knockback",
            subcategory = "General"
    )
    public boolean knockbackHideOnDecay = false;

    @Text(
            name = "Label",
            description = "Text shown before the knockback value (example: \"KB: \")",
            category = "Knockback",
            subcategory = "General"
    )
    public String knockbackLabel = "KB: ";

    @Checkbox(
            name = "Players Only",
            description = "Only track knockback on players",
            category = "Knockback",
            subcategory = "General"
    )
    public boolean knockbackPlayersOnly = true;

// =============================
// Combo
// =============================

    @HUD(
            name = "Combo HUD",
            category = "Combo",
            subcategory = "HUD"
    )
    public final ComboHud comboHud = new ComboHud();

    public boolean comboHudEnabled = true;

    @Checkbox(
            name = "Hide on Zero",
            description = "Hide the Combo HUD when combo is zero",
            category = "Combo",
            subcategory = "General"
    )
    public boolean comboHideOnZero = true;

    @Text(
            name = "Label",
            description = "Text shown before the combo value (example: \"Combo: \")",
            category = "Combo",
            subcategory = "General"
    )
    public String comboLabel = "Combo: ";

    @Checkbox(
            name = "Players Only",
            description = "Only count combos on players",
            category = "Combo",
            subcategory = "General"
    )
    public boolean comboPlayersOnly = true;

    @Number(
            name = "Reset Time (seconds)",
            description = "Time before combo resets to 0. Set to 0 to disable auto-reset.",
            min = 0, max = 60,
            category = "Combo",
            subcategory = "General"
    )
    public int comboResetTimeSeconds = 5;

// =============================
// CPS
// =============================


    @HUD(
            name = "CPS HUD",
            category = "CPS",
            subcategory = "HUD"
    )
    public final CpsHud cpsHud = new CpsHud();

    public boolean cpsHudEnabled = true;

    @Checkbox(
            name = "Hide on Zero",
            description = "Hide the CPS HUD when CPS reaches zero.",
            category = "CPS",
            subcategory = "General"
    )
    public boolean cpsHideOnZero = false;

    @Text(
            name = "Label",
            description = "Text shown before the CPS value (example: \"CPS: \")",
            category = "CPS",
            subcategory = "General"
    )
    public String cpsLabel = "CPS: ";

    @Dropdown(
            name = "Mode",
            description = "Which clicks to show",
            options = {"Left", "Right", "Both"},
            category = "CPS",
            subcategory = "General"
    )
    public int cpsMode = 2;

    @Number(
            name = "Average Window (seconds)",
            description = "Smooths CPS after 1 second of activity. 1 = no smoothing.",
            min = 1, max = 5,
            category = "CPS",
            subcategory = "General"
    )
    public int cpsAverageSeconds = 1;

    @Checkbox(
            name = "Show Decimals",
            description = "Show CPS with one decimal place. Does nothing when Average Window is 1.",
            category = "CPS",
            subcategory = "General"
    )
    public boolean cpsShowDecimals = false;

    // =============================
    // Debug
    // =============================

    @Checkbox(
            name = "Enabled",
            description = "Shows HitSpan debug messages in chat.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugEnabled = false;

    @Checkbox(
            name = "Verbose",
            description = "Spammy. Includes per-tick state and queue details.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugVerbose = false;

    @Checkbox(
            name = "Show Packet Confirms",
            description = "Logs S19 hurt packet confirms.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugPackets = true;

    @Checkbox(
            name = "Show Attack Events",
            description = "Logs AttackEntityEvent enqueue details.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugAttacks = true;

    @Checkbox(
            name = "Show Confirm Results",
            description = "Logs which confirm path fired and why others were ignored.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugConfirms = true;

    @Checkbox(
            name = "Singleplayer Server Compare",
            description = "Singleplayer only. Compares confirmed client range vs integrated server range.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugServerCompare = false;

    @Checkbox(
            name = "Show Server Attacker Info",
            description = "Includes server-side attacker yaw, pitch, and position in server range debug output.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugServerAttackerInfo = false;

    @Checkbox(
            name = "Update Checker",
            description = "Show update notification when joining the game.",
            category = "Debug",
            subcategory = "Updates"
    )
    public boolean updateCheckerEnabled = true;

    private HitSpanConfig() {
        super(
                new Mod("HitSpan", ModType.UTIL_QOL, "/assets/hitspan/icon3.png"),
                "hitspan.json"
        );
        initialize();
    }

    public static void init() {
        if (INSTANCE == null) INSTANCE = new HitSpanConfig();
    }

    public static void saveConfig() {
        if (INSTANCE != null) INSTANCE.save();
    }
}
