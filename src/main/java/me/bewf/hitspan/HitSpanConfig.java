package me.bewf.hitspan;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class HitSpanConfig {
    private static Configuration config;

    // defaults
    public static int decayMs = 2000;
    public static boolean playersOnly = true;

    public static void init(File configFile) {
        config = new Configuration(configFile);
        load();
    }

    public static void load() {
        try {
            config.load();

            decayMs = config.getInt(
                    "decayMs",
                    "general",
                    decayMs,
                    0,
                    600000,
                    "How long until HUD values reset to 0 (milliseconds)"
            );

            playersOnly = config.getBoolean(
                    "playersOnly",
                    "general",
                    playersOnly,
                    "Only track hits against players"
            );
        } catch (Exception ignored) {
        } finally {
            if (config.hasChanged()) config.save();
        }
    }
}
