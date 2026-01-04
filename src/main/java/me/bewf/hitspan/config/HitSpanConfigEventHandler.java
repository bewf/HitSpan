package me.bewf.hitspan.config;

import me.bewf.hitspan.HitSpan;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HitSpanConfigEventHandler {

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(HitSpan.MODID)) {
            HitSpanConfig.saveAndSyncFromGui(); // apply + save
        }
    }
}
