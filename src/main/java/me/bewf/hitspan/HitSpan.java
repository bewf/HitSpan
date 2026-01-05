package me.bewf.hitspan;

import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = HitSpan.MODID,
        name = "HitSpan",
        version = "1.2",
        acceptedMinecraftVersions = "[1.8.9]",
        clientSideOnly = true,
        acceptableRemoteVersions = "*"
)
public class HitSpan {

    public static final String MODID = "hitspan";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        HitSpanConfig.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Register event listeners
        MinecraftForge.EVENT_BUS.register(new RangeTracker());
        MinecraftForge.EVENT_BUS.register(new KnockbackTracker());

        System.out.println("HitSpan loaded - bewf on top");
    }
}
