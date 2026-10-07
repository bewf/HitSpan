package me.bewf.hitspan.util;

import me.bewf.hitspan.HitSpan;
import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;

public final class UpdateCheckListener {

    private static boolean started = false;
    private static boolean initialized = false;

    private UpdateCheckListener() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        EventManager.register(TickEvent.End.class, event -> onClientTick());
    }

    private static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        if (started) return;
        started = true;

        if (HitSpanConfig.INSTANCE != null && !HitSpanConfig.INSTANCE.updateCheckerEnabled) {
            System.out.println("[HitSpan] Update checker disabled in config");
            return;
        }

        UpdateChecker.checkOnce(
                "dDmpgD3L",
                "hitspan",
                "HitSpan",
                HitSpan.VERSION,
                HitSpan.MC_VERSION,
                HitSpan.LOADER
        );
    }
}
