package me.bewf.hitspan;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class KnockbackTracker {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static double lastKB = -1;
    public static long lastKBTimeMs = 0;

    private static int trackingEntityId = -1;
    private static double startX = 0;
    private static double startZ = 0;
    private static int ticksLeft = 0;
    private static double maxHorizDisp = 0;

    private static int lastBeginEntityId = -1;
    private static long lastBeginTimeMs = 0;

    public static void beginTracking(EntityLivingBase target) {
        long now = System.currentTimeMillis();
        int id = target.getEntityId();

        if (id == lastBeginEntityId && (now - lastBeginTimeMs) < 120) return;
        lastBeginEntityId = id;
        lastBeginTimeMs = now;

        trackingEntityId = id;
        startX = target.posX;
        startZ = target.posZ;
        ticksLeft = 8;
        maxHorizDisp = 0;

        lastKB = 0;
        lastKBTimeMs = now;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.world == null) return;

        if (ticksLeft > 0 && trackingEntityId != -1) {
            ticksLeft--;

            Entity e = mc.world.getEntityByID(trackingEntityId);
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
