package me.bewf.hitspan.config;

import me.bewf.hitspan.HitSpan;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.List;

public class HitSpanGuiConfig extends GuiConfig {

    public HitSpanGuiConfig(GuiScreen parentScreen) {
        super(parentScreen, getConfigElements(), HitSpan.MODID, false, false, "HitSpan Config");
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<IConfigElement>();
        Configuration cfg = HitSpanConfig.getConfig();
        if (cfg == null) return list;

        list.add(new ConfigElement(cfg.getCategory("general")));
        list.add(new ConfigElement(cfg.getCategory("hud")));
        return list;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        HitSpanConfig.saveAndSyncFromGui();
    }
}
