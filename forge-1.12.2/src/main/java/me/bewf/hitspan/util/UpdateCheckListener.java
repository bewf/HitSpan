package me.bewf.hitspan.util;

import me.bewf.hitspan.HitSpan;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class UpdateCheckListener {

    private boolean started = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null) return;

        if (started) return;
        started = true;

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
