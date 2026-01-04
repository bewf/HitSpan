package me.bewf.hitspan;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class KnockbackTracker {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // Public values HUD reads
    public static double lastKB = -1;
    public static long lastKBTimeMs = 0;

    // Tracking state
    private static int trackingEntityId = -1;
    private static double startX = 0;
    private static double startZ = 0;
    private static int ticksLeft = 0;
    private static double maxHorizDisp = 0;

    // Called by RangeTracker when a hit is confirmed
    public static void beginTracking(EntityLivingBase target) {
        trackingEntityId = target.getEntityId();
        startX = target.posX;
        startZ = target.posZ;
        ticksLeft = 8;          // measure over next ~8 ticks
        maxHorizDisp = 0;

        // reset lastKB when a new hit starts tracking (optional)
        lastKB = 0;
        lastKBTimeMs = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null) return;

        if (ticksLeft > 0 && trackingEntityId != -1) {
            ticksLeft--;

            Entity e = mc.theWorld.getEntityByID(trackingEntityId);
            if (e != null) {
                double dx = e.posX - startX;
                double dz = e.posZ - startZ;
                double horiz = Math.sqrt(dx * dx + dz * dz);

                if (horiz > maxHorizDisp) {
                    maxHorizDisp = horiz;
                    lastKB = maxHorizDisp;
                    lastKBTimeMs = System.currentTimeMillis();
                }
            }

            if (ticksLeft == 0) {
                trackingEntityId = -1;
            }
        }
    }
}
