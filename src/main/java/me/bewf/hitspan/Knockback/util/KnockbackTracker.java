package me.bewf.hitspan.Knockback.util;

import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class KnockbackTracker {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static double lastKB = -1;
    public static long lastKBTimeMs = 0;

    private static int trackingEntityId = -1;
    private static double startX = 0;
    private static double startZ = 0;
    private static int ticksLeft = 0;

    private static double maxConeDisp = 0;

    private static int lastBeginEntityId = -1;
    private static long lastBeginTimeMs = 0;

    private static double fwdX = 0;
    private static double fwdZ = 0;

    private static final double MIN_KB_EPS = 0.02D;
    private static final double CONE_COS_MIN = 0.60D;

    public static void beginTracking(EntityLivingBase target) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.knockbackHudEnabled) return;
        if (cfg.knockbackPlayersOnly && !(target instanceof EntityPlayer)) return;
        if (mc.thePlayer == null) return;

        long now = System.currentTimeMillis();
        int id = target.getEntityId();

        if (id == lastBeginEntityId && (now - lastBeginTimeMs) < 120) return;
        lastBeginEntityId = id;
        lastBeginTimeMs = now;

        trackingEntityId = id;
        startX = target.posX;
        startZ = target.posZ;
        ticksLeft = 8;

        maxConeDisp = 0;
        lastKB = 0;

        float yaw = mc.thePlayer.rotationYaw;
        double rad = Math.toRadians(yaw);

        fwdX = -Math.sin(rad);
        fwdZ = Math.cos(rad);

        double len = Math.sqrt(fwdX * fwdX + fwdZ * fwdZ);
        if (len > 1e-9) {
            fwdX /= len;
            fwdZ /= len;
        } else {
            fwdX = 0;
            fwdZ = 0;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null) return;

        if (ticksLeft > 0 && trackingEntityId != -1) {
            ticksLeft--;

            EntityLivingBase e = (EntityLivingBase) mc.theWorld.getEntityByID(trackingEntityId);
            if (e != null) {
                double dx = e.posX - startX;
                double dz = e.posZ - startZ;

                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 1e-9) {
                    double ux = dx / dist;
                    double uz = dz / dist;

                    double cos = (ux * fwdX) + (uz * fwdZ);

                    if (cos >= CONE_COS_MIN) {
                        double forward = dist * cos;

                        if (forward > maxConeDisp + MIN_KB_EPS) {
                            maxConeDisp = forward;
                            lastKB = maxConeDisp;
                            lastKBTimeMs = System.currentTimeMillis();
                        }
                    }
                }
            }

            if (ticksLeft == 0) {
                trackingEntityId = -1;
            }
        }
    }
}
