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

    private static final ArrayDeque<Integer> leftSamples = new ArrayDeque<>();
    private static final ArrayDeque<Integer> rightSamples = new ArrayDeque<>();

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
        if (mc == null || mc.world == null) return;

        pruneRaw(leftClicks);
        pruneRaw(rightClicks);

        long now = System.currentTimeMillis();
        if (now - lastSampleTimeMs < SAMPLE_EVERY_MS) return;
        lastSampleTimeMs = now;

        int lRaw = leftClicks.size();
        int rRaw = rightClicks.size();

        if (lRaw == 0) leftSamples.clear();
        if (rRaw == 0) rightSamples.clear();

        leftSamples.addLast(lRaw);
        rightSamples.addLast(rRaw);

        pruneSamples(leftSamples);
        pruneSamples(rightSamples);
    }

    private static void recordEdges() {
        if (mc == null || mc.world == null) return;

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
        return applySmoothing(rawLeft(), leftSamples);
    }

    public static float getRightCpsFloat() {
        return applySmoothing(rawRight(), rightSamples);
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

    private static float applySmoothing(int raw, ArrayDeque<Integer> samples) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return raw;

        int seconds = cfg.cpsAverageSeconds;
        if (seconds <= 1) return raw;

        int warmupSamples = 1000 / (int) SAMPLE_EVERY_MS;
        int maxSamples = (seconds * 1000) / (int) SAMPLE_EVERY_MS;

        if (samples.size() < warmupSamples) return raw;
        if (samples.size() < maxSamples) return raw;

        int sum = 0;
        for (int v : samples) sum += v;
        return (float) sum / samples.size();
    }

    private static void pruneRaw(ArrayDeque<Long> deque) {
        long now = System.currentTimeMillis();
        while (!deque.isEmpty() && now - deque.peekFirst() > RAW_WINDOW_MS) {
            deque.pollFirst();
        }
    }

    private static void pruneSamples(ArrayDeque<Integer> samples) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return;

        int seconds = Math.max(1, Math.min(5, cfg.cpsAverageSeconds));
        int maxSamples = (seconds * 1000) / (int) SAMPLE_EVERY_MS;

        while (samples.size() > maxSamples) {
            samples.pollFirst();
        }
    }
}
