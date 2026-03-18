// src/main/java/me/bewf/hitspan/cps/util/CpsTracker.java
package me.bewf.hitspan.cps.util;

import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;

public class CpsTracker {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final long RAW_WINDOW_MS = 1000L;
    private static final long SAMPLE_EVERY_MS = 100L;

    private static final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private static final ArrayDeque<Long> rightClicks = new ArrayDeque<>();

    // Previously we kept fixed-size sample buffers here. Those produced
    // abrupt jumps when the buffer filled or was cleared. Replace with
    // a lightweight exponential moving average (EMA) to smooth values
    // continuously.
    private static float emaLeft = 0f;
    private static float emaRight = 0f;
    private static boolean emaInitLeft = false;
    private static boolean emaInitRight = false;

    private static boolean leftPressed = false;
    private static boolean rightPressed = false;

    private static long lastSampleTimeMs = 0L;

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        recordEdges();
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        recordEdges();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc == null || mc.theWorld == null) return;

        pruneRaw(leftClicks);
        pruneRaw(rightClicks);

        long now = System.currentTimeMillis();
        if (now - lastSampleTimeMs < SAMPLE_EVERY_MS) return;
        lastSampleTimeMs = now;

        int lRaw = leftClicks.size();
        int rRaw = rightClicks.size();

        // Update EMA smoothing. The configured cpsAverageSeconds controls
        // how "wide" the smoothing window is. We derive an alpha from the
        // desired window so that the EMA responds gradually instead of
        // jumping when a buffer fills or clears. Use asymmetric multipliers
        // so rises and falls react faster than a symmetric EMA would.
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        int seconds = (cfg == null) ? 1 : cfg.cpsAverageSeconds;

        if (seconds <= 1) {
            // No smoothing: keep EMA synced to raw so getters will return raw.
            emaLeft = lRaw;
            emaRight = rRaw;
            emaInitLeft = emaInitRight = true;
        } else {
            // base alpha = dt / window
            float baseAlpha = (float) SAMPLE_EVERY_MS / (seconds * 1000f);
            // multipliers: make rise respond quicker and make fall respond even quicker
            final float RISE_MULT = 3.5f;
            final float FALL_MULT = 8.0f;
            // clamp baseAlpha to avoid degenerate tiny values
            baseAlpha = Math.max(0.005f, Math.min(1f, baseAlpha));

            // Left
            if (!emaInitLeft) {
                // Only seed EMA when we actually see activity (avoid bias from idle zeros)
                if (lRaw > 0) {
                    emaLeft = lRaw;
                    emaInitLeft = true;
                }
            } else {
                float alpha = baseAlpha * (lRaw > emaLeft ? RISE_MULT : FALL_MULT);
                alpha = Math.max(0.01f, Math.min(1f, alpha));
                emaLeft += alpha * (lRaw - emaLeft);
                // Snap small lingering values to zero for a cleaner display when idle
                // Use a slightly higher threshold so tiny 1-2 CPS blips don't linger.
                if (lRaw == 0 && emaLeft < 1.5f) emaLeft = 0f;
            }

            // Right
            if (!emaInitRight) {
                if (rRaw > 0) {
                    emaRight = rRaw;
                    emaInitRight = true;
                }
            } else {
                float alpha = baseAlpha * (rRaw > emaRight ? RISE_MULT : FALL_MULT);
                alpha = Math.max(0.01f, Math.min(1f, alpha));
                emaRight += alpha * (rRaw - emaRight);
                if (rRaw == 0 && emaRight < 1.5f) emaRight = 0f;
            }
        }
    }

    private static void recordEdges() {
        if (mc == null || mc.theWorld == null) return;

        long now = System.currentTimeMillis();

        boolean lDown = mc.gameSettings.keyBindAttack.isKeyDown();
        if (lDown != leftPressed) {
            leftPressed = lDown;
            if (lDown) leftClicks.addLast(now);
        }

        boolean rDown = mc.gameSettings.keyBindUseItem.isKeyDown();
        if (rDown != rightPressed) {
            rightPressed = rDown;
            if (rDown) rightClicks.addLast(now);
        }
    }

    /* ===== Public getters ===== */

    public static int getLeftCpsInt() {
        return Math.round(getLeftCpsFloat());
    }

    public static int getRightCpsInt() {
        return Math.round(getRightCpsFloat());
    }

    public static float getLeftCpsFloat() {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || cfg.cpsAverageSeconds <= 1) return rawLeft();
        // If EMA hasn't been initialized yet return raw as a fallback.
        return emaInitLeft ? emaLeft : rawLeft();
    }

    public static float getRightCpsFloat() {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || cfg.cpsAverageSeconds <= 1) return rawRight();
        return emaInitRight ? emaRight : rawRight();
    }

    /* ===== Internals ===== */

    private static int rawLeft() {
        pruneRaw(leftClicks);
        return leftClicks.size();
    }

    private static int rawRight() {
        pruneRaw(rightClicks);
        return rightClicks.size();
    }

    // Smoothing is handled by the EMA updated in onClientTick.

    private static void pruneRaw(ArrayDeque<Long> deque) {
        long now = System.currentTimeMillis();
        while (!deque.isEmpty() && now - deque.peekFirst() > RAW_WINDOW_MS) {
            deque.pollFirst();
        }
    }
}
