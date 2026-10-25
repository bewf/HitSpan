package me.bewf.hitspan;

import me.bewf.hitspan.Knockback.hud.KnockbackHud;
import me.bewf.hitspan.Knockback.util.KnockbackTracker;
import me.bewf.hitspan.Range.hud.RangeHud;
import me.bewf.hitspan.Range.util.RangeTracker;
import me.bewf.hitspan.combo.hud.ComboHud;
import me.bewf.hitspan.combo.util.ComboTracker;
import me.bewf.hitspan.commands.HitSpanCommand;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.cps.hud.CpsHud;
import me.bewf.hitspan.cps.util.CpsTracker;
import me.bewf.hitspan.util.HitSpanPackets;
import me.bewf.hitspan.util.UpdateCheckListener;
import net.fabricmc.api.ClientModInitializer;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;

public class HitSpanClient implements ClientModInitializer {

    private static final String ICON = "assets/hitspan/icon3.png";

    @Override
    public void onInitializeClient() {
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

        HudManager.register(new RangeHud(), HitSpan.MODID, ICON);
        HudManager.register(new KnockbackHud(), HitSpan.MODID, ICON);
        HudManager.register(new ComboHud(), HitSpan.MODID, ICON);
        HudManager.register(new CpsHud(), HitSpan.MODID, ICON);

        RangeTracker.INSTANCE = new RangeTracker();
        KnockbackTracker.init();
        ComboTracker.INSTANCE = new ComboTracker();
        CpsTracker.init();
        HitSpanPackets.init();
        UpdateCheckListener.init();

        HitSpanCommand.register();

        System.out.println("HitSpan loaded - bewf on top");
    }
}
