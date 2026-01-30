// src/main/java/me/bewf/hitspan/debug/HitSpanDebug.java
package me.bewf.hitspan.debug;

import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public class HitSpanDebug {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static long lastSecond = 0L;
    private static int linesThisSecond = 0;

    private static long lastVerboseMs = 0L;

    private HitSpanDebug() {}

    public static void chat(String msg) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.debugEnabled) return;
        send("[HitSpan] " + msg);
    }

    public static void chatVerbose(String msg) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.debugEnabled || !cfg.debugVerbose) return;

        long now = System.currentTimeMillis();
        if (now - lastVerboseMs < 60L) return;
        lastVerboseMs = now;

        send("[HitSpan] " + msg);
    }

    private static void send(String msg) {
        if (mc == null || mc.thePlayer == null) return;

        long now = System.currentTimeMillis();
        long sec = now / 1000L;

        if (sec != lastSecond) {
            lastSecond = sec;
            linesThisSecond = 0;
        }

        if (linesThisSecond >= 8) return;
        linesThisSecond++;

        mc.thePlayer.addChatMessage(new ChatComponentText(msg));
    }
}
