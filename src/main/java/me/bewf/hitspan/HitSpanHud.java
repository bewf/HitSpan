package me.bewf.hitspan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.text.DecimalFormat;

public class HitSpanHud {

    private final Minecraft mc = Minecraft.getMinecraft();

    // Temporary fixed positions (we’ll make draggable with OneConfig later)
    private int rangeX = 5, rangeY = 5;
    private int kbX = 5, kbY = 22;

    private final DecimalFormat df = new DecimalFormat("0.00");

    // After this many ms without updates, show 0.00 instead of hiding
    private double getValueOrZero(double val, long lastTime, long now) {
        long decay = HitSpanConfig.decayMs; // <-- add this line
        if (lastTime == 0L) {
            return 0.0D;
        } else if (now - lastTime > decay) {   // <-- use it here
            return 0.0D;
        } else {
            return val < 0.0D ? 0.0D : val;
        }
    }


    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        long now = System.currentTimeMillis();

        double rangeVal = getValueOrZero(RangeTracker.lastRange, RangeTracker.lastRangeTimeMs, now);
        double kbVal = getValueOrZero(KnockbackTracker.lastKB, KnockbackTracker.lastKBTimeMs, now);

        drawBox(rangeX, rangeY, "Range: " + df.format(rangeVal));
        drawBox(kbX, kbY, "KB: " + df.format(kbVal));
    }


    private void drawBox(int x, int y, String text) {
        int w = 6 + mc.fontRendererObj.getStringWidth(text);
        int h = 6 + mc.fontRendererObj.FONT_HEIGHT;

        Gui.drawRect(x, y, x + w, y + h, 0x50000000);
        mc.fontRendererObj.drawString(text, x + 3, y + 3, 0xFFFFFF);
    }
}
