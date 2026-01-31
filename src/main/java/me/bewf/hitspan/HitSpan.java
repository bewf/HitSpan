package me.bewf.hitspan;

import me.bewf.hitspan.Knockback.util.KnockbackTracker;
import me.bewf.hitspan.Range.util.RangeTracker;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.cps.util.CpsTracker;
import me.bewf.hitspan.util.UpdateCheckListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = HitSpan.MODID,
        name = "HitSpan",
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "[1.8.9]",
        clientSideOnly = true,
        acceptableRemoteVersions = "*"
)
public class HitSpan {

    public static final String MODID = "hitspan";
    public static final String VERSION = "@MOD_VERSION@";


    public static final String MC_VERSION = "1.8.9";
    public static final String LOADER = "forge";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        try {
            HitSpanConfig.init();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    HitSpanConfig.saveConfig();
                } catch (Throwable t) {
                    System.err.println("HitSpanConfig failed to save on shutdown: " + t);
                }
            }, "HitSpan-ConfigSave"));
        } catch (Throwable t) {
            System.err.println("HitSpanConfig failed to initialize; continuing without config: " + t);
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new RangeTracker());
        MinecraftForge.EVENT_BUS.register(new KnockbackTracker());
        MinecraftForge.EVENT_BUS.register(new CpsTracker());

        MinecraftForge.EVENT_BUS.register(new UpdateCheckListener());

        System.out.println("HitSpan loaded - bewf on top");
    }
}
