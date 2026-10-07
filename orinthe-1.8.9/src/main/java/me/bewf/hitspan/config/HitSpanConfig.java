package me.bewf.hitspan.config;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.Include;
import org.polyfrost.oneconfig.api.config.v1.annotations.Info;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

public class HitSpanConfig extends Config {

    public static HitSpanConfig INSTANCE;

    @Info(
            title = "For developers",
            description = "These options are for debugging and can spam your chat. You shouldn't need to touch them.",
            category = "Debug",
            subcategory = "General"
    )
    public String debugWarning = "";

    @Switch(
            title = "Enabled",
            description = "Shows HitSpan debug messages in chat.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugEnabled = false;

    @Switch(
            title = "Verbose",
            description = "Spammy. Includes per-tick state and queue details.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugVerbose = false;

    @Switch(
            title = "Show Packet Confirms",
            description = "Logs S19 hurt packet confirms.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugPackets = true;

    @Switch(
            title = "Show Attack Events",
            description = "Logs attack-entity enqueue details.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugAttacks = true;

    @Switch(
            title = "Show Confirm Results",
            description = "Logs which confirm path fired and why others were ignored.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugConfirms = true;

    @Switch(
            title = "Singleplayer Server Compare",
            description = "Singleplayer only. Compares confirmed client range vs integrated server range.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugServerCompare = false;

    @Switch(
            title = "Show Server Attacker Info",
            description = "Includes server-side attacker yaw, pitch, and position in server range debug output.",
            category = "Debug",
            subcategory = "General"
    )
    public boolean debugServerAttackerInfo = false;

    @Switch(
            title = "Update Checker",
            description = "Show update notification when joining the game.",
            category = "Debug",
            subcategory = "Updates"
    )
    public boolean updateCheckerEnabled = true;

    @Include
    public boolean hasShownConfigTip = false;

    private HitSpanConfig() {
        super("hitspan", "assets/hitspan/icon3.png", "HitSpan", Category.COMBAT);
    }

    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new HitSpanConfig();
            INSTANCE.preload();
        }
    }

    public static void saveConfig() {
        if (INSTANCE != null) INSTANCE.save();
    }
}
