package me.bewf.hitspan;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

@Mod(
        modid = "hitspan",
        name = "HitSpan",
        version = "1.1",
        acceptedMinecraftVersions = "[1.8.9]",
        clientSideOnly = true,
        acceptableRemoteVersions = "*"
)
public class HitSpan {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // init config
        HitSpanConfig.init(new File(event.getModConfigurationDirectory(), "hitspan.cfg"));
    }

    @Mod.EventHandler
    public void init(net.minecraftforge.fml.common.event.FMLInitializationEvent event) {
        // register event handlers
        MinecraftForge.EVENT_BUS.register(new RangeTracker());
        MinecraftForge.EVENT_BUS.register(new KnockbackTracker());
        MinecraftForge.EVENT_BUS.register(new HitSpanHud());

        System.out.println("HitSpan loaded - bewf on top");
    }
}
