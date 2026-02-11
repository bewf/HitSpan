package me.bewf.hitspan.util;

import me.bewf.hitspan.HitSpan;
import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class UpdateCheckListener {

    private boolean started = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        if (started) return;
        started = true;

        // Check if update checker is enabled in config
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
