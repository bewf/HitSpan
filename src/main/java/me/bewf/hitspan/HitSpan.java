package me.bewf.hitspan;

import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

@Mod(
        modid = HitSpan.MODID,
        name = "HitSpan",
        version = "1.1",
        acceptedMinecraftVersions = "[1.8.9]",
        clientSideOnly = true,
        acceptableRemoteVersions = "*",
        guiFactory = "me.bewf.hitspan.config.HitSpanGuiFactory"
)
public class HitSpan {

    public static final String MODID = "hitspan";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        HitSpanConfig.init(new File(event.getModConfigurationDirectory(), "hitspan.cfg"));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new me.bewf.hitspan.config.HitSpanConfigEventHandler());
        MinecraftForge.EVENT_BUS.register(new RangeTracker());
        MinecraftForge.EVENT_BUS.register(new KnockbackTracker());
        MinecraftForge.EVENT_BUS.register(new HitSpanHud());
        System.out.println("HitSpan loaded - bewf on top");
    }
}
