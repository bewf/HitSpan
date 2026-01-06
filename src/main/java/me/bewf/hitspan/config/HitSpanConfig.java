package me.bewf.hitspan.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class HitSpanConfig {

    private static Configuration configFile;

    public static int decayMs = 2000;
    public static boolean playersOnly = true;

    public static boolean showRangeHud = true;
    public static boolean showKbHud = true;

    public static int rangeX = 5, rangeY = 5;
    public static int kbX = 5, kbY = 22;

    public static void init(File file) {
        configFile = new Configuration(file);
        sync(); // loads, reads values, saves if needed
    }

    public static void sync() {
        if (configFile == null) return;

        try {
            configFile.load();

            decayMs = configFile.getInt(
                    "decayMs",
                    "general",
                    decayMs,
                    0,
                    600000,
                    "How long until HUD values reset to 0 (milliseconds)"
            );

            playersOnly = configFile.getBoolean(
                    "playersOnly",
                    "general",
                    playersOnly,
                    "Only track hits against players"
            );

            showRangeHud = configFile.getBoolean(
                    "showRangeHud",
                    "hud",
                    showRangeHud,
                    "Show the Range HUD"
            );

            showKbHud = configFile.getBoolean(
                    "showKbHud",
                    "hud",
                    showKbHud,
                    "Show the KB HUD"
            );

            rangeX = configFile.getInt("rangeX", "hud", rangeX, 0, 10000, "Range HUD X position");
            rangeY = configFile.getInt("rangeY", "hud", rangeY, 0, 10000, "Range HUD Y position");

            kbX = configFile.getInt("kbX", "hud", kbX, 0, 10000, "KB HUD X position");
            kbY = configFile.getInt("kbY", "hud", kbY, 0, 10000, "KB HUD Y position");

        } finally {
            if (configFile.hasChanged()) configFile.save();
        }
    }

    public static Configuration getConfig() {
        return configFile;
    }

    // called when GUI changes, just re-read from the config object + save
    public static void saveAndSyncFromGui() {
        if (configFile == null) return;
        // don't call load() here, GUI already edited configFile in memory
        // just re-read values out of it:
        decayMs = configFile.get("general", "decayMs", decayMs).getInt();
        playersOnly = configFile.get("general", "playersOnly", playersOnly).getBoolean();

        showRangeHud = configFile.get("hud", "showRangeHud", showRangeHud).getBoolean();
        showKbHud = configFile.get("hud", "showKbHud", showKbHud).getBoolean();

        rangeX = configFile.get("hud", "rangeX", rangeX).getInt();
        rangeY = configFile.get("hud", "rangeY", rangeY).getInt();
        kbX = configFile.get("hud", "kbX", kbX).getInt();
        kbY = configFile.get("hud", "kbY", kbY).getInt();

        if (configFile.hasChanged()) configFile.save();
    }
}
