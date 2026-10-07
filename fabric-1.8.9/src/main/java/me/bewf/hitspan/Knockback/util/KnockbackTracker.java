package me.bewf.hitspan.Knockback.util;

import me.bewf.hitspan.Knockback.hud.KnockbackHud;
import me.bewf.hitspan.hud.HitSpanHuds;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;

public class KnockbackTracker {

    private static final Minecraft mc = Minecraft.getInstance();

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

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        EventManager.register(TickEvent.End.class, event -> onClientTick());
    }

    public static void beginTracking(LivingEntity target) {
        KnockbackHud hud = HitSpanHuds.knockback();
        boolean playersOnly = hud == null || hud.getPlayersOnly();
        if (playersOnly && !(target instanceof PlayerEntity)) return;
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        int id = target.getNetworkId();

        if (id == lastBeginEntityId && (now - lastBeginTimeMs) < 120) return;
        lastBeginEntityId = id;
        lastBeginTimeMs = now;

        trackingEntityId = id;
        startX = target.x;
        startZ = target.z;
        ticksLeft = 8;

        maxConeDisp = 0;
        lastKB = 0;

        float yaw = mc.player.yaw;
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

    private static void onClientTick() {
        if (mc.world == null) return;

        if (ticksLeft > 0 && trackingEntityId != -1) {
            ticksLeft--;

            LivingEntity e = (LivingEntity) mc.world.getEntity(trackingEntityId);
            if (e != null) {
                double dx = e.x - startX;
                double dz = e.z - startZ;

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
