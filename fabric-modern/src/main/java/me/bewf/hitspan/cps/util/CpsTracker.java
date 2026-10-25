package me.bewf.hitspan.cps.util;

import me.bewf.hitspan.cps.hud.CpsHud;
import me.bewf.hitspan.hud.HitSpanHuds;
import me.bewf.hitspan.util.KeyMatch;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;

import java.util.ArrayDeque;

public class CpsTracker {

    private static final long MAX_WINDOW_MS = 5000L;

    private static final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private static final ArrayDeque<Long> rightClicks = new ArrayDeque<>();

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        EventManager.register(MouseInputEvent.class, event -> onMouse(event));
        EventManager.register(KeyInputEvent.class, event -> onKey(event));
        EventManager.register(TickEvent.End.class, event -> onClientTick());
    }

    private static boolean inWorld() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null;
    }

    private static void onMouse(MouseInputEvent event) {
        if (event.state != MouseInputEvent.PRESSED) return;
        if (!inWorld()) return;

        long now = System.currentTimeMillis();
        if (KeyMatch.attackMouse(event.button)) leftClicks.addLast(now);
        if (KeyMatch.useMouse(event.button)) rightClicks.addLast(now);
    }

    private static void onKey(KeyInputEvent event) {
        if (event.state != KeyInputEvent.PRESSED || event.key == 0) return;
        if (!inWorld()) return;

        long now = System.currentTimeMillis();
        if (KeyMatch.attackKey(event.key)) leftClicks.addLast(now);
        if (KeyMatch.useKey(event.key)) rightClicks.addLast(now);
    }

    private static void onClientTick() {
        if (!inWorld()) {
            leftClicks.clear();
            rightClicks.clear();
            return;
        }
        long now = System.currentTimeMillis();
        prune(leftClicks, now, MAX_WINDOW_MS);
        prune(rightClicks, now, MAX_WINDOW_MS);
    }

    public static int getLeftCpsInt() {
        return Math.round(getLeftCpsFloat());
    }

    public static int getRightCpsInt() {
        return Math.round(getRightCpsFloat());
    }

    public static float getLeftCpsFloat() {
        return cps(leftClicks);
    }

    public static float getRightCpsFloat() {
        return cps(rightClicks);
    }

    private static int windowSeconds() {
        CpsHud hud = HitSpanHuds.cps();
        int seconds = hud == null ? 1 : hud.getAverageSeconds();
        return Math.max(1, Math.min(5, seconds));
    }

    private static float cps(ArrayDeque<Long> clicks) {
        int seconds = windowSeconds();
        long windowMs = seconds * 1000L;
        long now = System.currentTimeMillis();

        prune(clicks, now, windowMs);
        if (clicks.isEmpty()) return 0f;

        int count = clicks.size();
        if (seconds <= 1) return count;

        long span = Math.min(windowMs, Math.max(1000L, now - clicks.peekFirst()));
        return count * 1000f / span;
    }

    private static void prune(ArrayDeque<Long> clicks, long now, long windowMs) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > windowMs) {
            clicks.pollFirst();
        }
    }
}
