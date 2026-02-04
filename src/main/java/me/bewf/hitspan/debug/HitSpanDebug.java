// src/main/java/me/bewf/hitspan/debug/HitSpanDebug.java
package me.bewf.hitspan.debug;

import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

public class HitSpanDebug {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static long lastSecond = 0L;
    private static int linesThisSecond = 0;

    private static long lastVerboseMs = 0L;
    private static boolean warnedNotSingleplayer = false;

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

    public static void rangeServerCompare(int entityId, double clientRange, String source, long clientAttackTimeMs) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.debugEnabled || !cfg.debugServerCompare) return;

        if (mc == null || mc.world == null || mc.player == null) return;

        if (!mc.isSingleplayer()) {
            if (!warnedNotSingleplayer) {
                warnedNotSingleplayer = true;
                chat("Singleplayer server compare disabled (not in singleplayer).");
            }
            return;
        }
        warnedNotSingleplayer = false;

        ServerRangeProbe.Match m =
                ServerRangeProbe.getClosestMatch(entityId, clientAttackTimeMs, 600L);

        if (m == null) {
            chat("Range check (" + source + "): no matched server record id=" +
                    entityId + " client=" + fmt(clientRange));
            return;
        }

        double diff = Math.abs(clientRange - m.range);
        long dt = m.timeMs - clientAttackTimeMs;

        String msg = "Range check (" + source + "): client=" + fmt(clientRange) +
                " server=" + fmt(m.range) +
                " diff=" + fmt(diff) +
                " dtMs=" + dt +
                " id=" + entityId;

        if (cfg.debugServerAttackerInfo) {
            msg += " syaw=" + fmt1(m.yaw) +
                    " spitch=" + fmt1(m.pitch) +
                    " spos=(" + fmt(m.ax) + "," + fmt(m.ay) + "," + fmt(m.az) + ")";
        }

        chat(msg);
    }

    private static String fmt(double d) {
        long x = (long) (d * 1000.0);
        return String.valueOf(x / 1000.0);
    }

    private static String fmt1(float f) {
        long x = (long) (f * 10.0f);
        return String.valueOf(x / 10.0f);
    }

    private static void send(String msg) {
        if (mc == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        long sec = now / 1000L;

        if (sec != lastSecond) {
            lastSecond = sec;
            linesThisSecond = 0;
        }

        if (linesThisSecond >= 8) return;
        linesThisSecond++;

        mc.player.sendMessage(new TextComponentString(msg));
    }
}
