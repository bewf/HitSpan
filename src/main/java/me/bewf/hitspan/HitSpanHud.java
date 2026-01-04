package me.bewf.hitspan;

import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.text.DecimalFormat;

public class HitSpanHud {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final DecimalFormat df = new DecimalFormat("0.00");

    private double getValueOrZero(double val, long lastTime, long now) {
        long decay = HitSpanConfig.decayMs;

        if (lastTime == 0L) return 0.0D;
        if (now - lastTime > decay) return 0.0D;
        return val < 0.0D ? 0.0D : val;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        long now = System.currentTimeMillis();

        // pull positions from config (so changing cfg actually moves it)
        int rangeX = HitSpanConfig.rangeX;
        int rangeY = HitSpanConfig.rangeY;
        int kbX = HitSpanConfig.kbX;
        int kbY = HitSpanConfig.kbY;

        // only draw what’s enabled
        if (HitSpanConfig.showRangeHud) {
            double rangeVal = getValueOrZero(RangeTracker.lastRange, RangeTracker.lastRangeTimeMs, now);
            drawBox(rangeX, rangeY, "Range: " + df.format(rangeVal));
        }

        if (HitSpanConfig.showKbHud) {
            double kbVal = getValueOrZero(KnockbackTracker.lastKB, KnockbackTracker.lastKBTimeMs, now);
            drawBox(kbX, kbY, "KB: " + df.format(kbVal));
        }
    }

    private void drawBox(int x, int y, String text) {
        int w = 6 + mc.fontRendererObj.getStringWidth(text);
        int h = 6 + mc.fontRendererObj.FONT_HEIGHT;

        Gui.drawRect(x, y, x + w, y + h, 0x50000000);
        mc.fontRendererObj.drawString(text, x + 3, y + 3, 0xFFFFFF);
    }
}
