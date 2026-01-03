package me.bewf.hitspan;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(
        modid = "hitspan",
        name = "HitSpan",
        version = "1.0",
        acceptedMinecraftVersions = "[1.8.9]",
        clientSideOnly = true,
        acceptableRemoteVersions = "*"
)
public class HitSpan {
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new RangeTracker());
        MinecraftForge.EVENT_BUS.register(new KnockbackTracker());
        MinecraftForge.EVENT_BUS.register(new HitSpanHud());
        System.out.println("HitSpan loaded");
    }
}
