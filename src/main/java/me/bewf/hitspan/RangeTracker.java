package me.bewf.hitspan;

import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class RangeTracker {

    private final Minecraft mc = Minecraft.getMinecraft();

    public static double lastRange = -1;
    public static long lastRangeTimeMs = 0;

    public static RangeTracker INSTANCE;
    public RangeTracker() { INSTANCE = this; }

    private int pendingEntityId = -1;
    private int pendingPrevHurtTime = 0;
    private int pendingPrevHurtResistantTime = 0;
    private int pendingTicksLeft = 0;

    private double pendingRange = -1;

    private double pendingStartY = 0;
    private int pendingAgeTicks = 0;

    private int lastConfirmedEntityId = -1;
    private long lastConfirmedTimeMs = 0;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.target == null) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;

        if (cfg != null && cfg.playersOnly && !(event.target instanceof EntityPlayer)) return;
        if (!(event.target instanceof EntityLivingBase)) return;

        EntityLivingBase target = (EntityLivingBase) event.target;

        boolean confirmedOnly = cfg != null && cfg.confirmRangeOnHitConfirm;

        if (confirmedOnly && cfg != null && cfg.confirmCooldownEnabled) {
            long now = System.currentTimeMillis();
            if (target.getEntityId() == lastConfirmedEntityId) {
                long cd = Math.max(0, cfg.confirmCooldownMs);
                if (cd > 0 && (now - lastConfirmedTimeMs) < cd) {
                    return;
                }
            }
        }

        double maxReach = mc.thePlayer.capabilities.isCreativeMode ? 4.5D : 3.0D;
        double computed = computeEntityRayDistance(mc.thePlayer, target, 1.0F, maxReach);
        if (computed < 0) return;
        if (computed > maxReach) computed = maxReach;

        if (!confirmedOnly) {
            lastRange = computed;
            lastRangeTimeMs = System.currentTimeMillis();
            return;
        }

        // pending confirm window (short)
        pendingRange = computed;
        pendingEntityId = target.getEntityId();
        pendingPrevHurtTime = target.hurtTime;
        pendingPrevHurtResistantTime = target.hurtResistantTime;

        pendingStartY = target.posY;
        pendingAgeTicks = 0;
        pendingTicksLeft = 3;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null) return;

        if (pendingTicksLeft > 0 && pendingEntityId != -1) {
            pendingTicksLeft--;
            pendingAgeTicks++;

            Entity e = mc.theWorld.getEntityByID(pendingEntityId);
            if (e instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) e;

                HitSpanConfig cfg = HitSpanConfig.INSTANCE;
                boolean confirmedOnly = cfg != null && cfg.confirmRangeOnHitConfirm;

                boolean hurtTimeIncreased = target.hurtTime > pendingPrevHurtTime;
                boolean resistantIncreased = target.hurtResistantTime > pendingPrevHurtResistantTime;

                boolean verticalConfirmed = false;
                if (cfg != null && cfg.verticalConfirmEnabled) {
                    double dy = Math.abs(target.posY - pendingStartY);
                    verticalConfirmed = dy >= Math.max(0.0f, cfg.verticalConfirmThreshold);
                }

                if (hurtTimeIncreased || resistantIncreased || verticalConfirmed) {
                    KnockbackTracker.beginTracking(target);

                    if (confirmedOnly && pendingRange >= 0) {
                        lastRange = pendingRange;
                        lastRangeTimeMs = System.currentTimeMillis();
                    }

                    lastConfirmedEntityId = pendingEntityId;
                    lastConfirmedTimeMs = System.currentTimeMillis();

                    clearPending();
                    return;
                }
            }

            if (pendingTicksLeft == 0) {
                clearPending();
            }
        }
    }

    // hurt animation packet fallback
    public void confirmFromHurtPacket(int entityId) {
        if (pendingEntityId == -1) return;
        if (entityId != pendingEntityId) return;
        if (pendingTicksLeft <= 0) return;
        if (mc.theWorld == null) return;

        Entity e = mc.theWorld.getEntityByID(pendingEntityId);
        if (e instanceof EntityLivingBase) {
            KnockbackTracker.beginTracking((EntityLivingBase) e);

            HitSpanConfig cfg = HitSpanConfig.INSTANCE;
            if (cfg != null && cfg.confirmRangeOnHitConfirm && pendingRange >= 0) {
                lastRange = pendingRange;
                lastRangeTimeMs = System.currentTimeMillis();
            }

            lastConfirmedEntityId = pendingEntityId;
            lastConfirmedTimeMs = System.currentTimeMillis();
        }

        clearPending();
    }

    private void clearPending() {
        pendingEntityId = -1;
        pendingTicksLeft = 0;
        pendingPrevHurtTime = 0;
        pendingPrevHurtResistantTime = 0;

        pendingRange = -1;
        pendingStartY = 0;
        pendingAgeTicks = 0;
    }

    private static double computeEntityRayDistance(EntityLivingBase player, Entity target, float partialTicks, double maxDist) {
        Vec3 eyes = player.getPositionEyes(partialTicks);
        Vec3 look = player.getLook(partialTicks);
        Vec3 end = eyes.addVector(look.xCoord * maxDist, look.yCoord * maxDist, look.zCoord * maxDist);

        float border = target.getCollisionBorderSize();
        AxisAlignedBB bb = target.getEntityBoundingBox().expand(border, border, border);

        MovingObjectPosition hit = bb.calculateIntercept(eyes, end);
        if (hit == null || hit.hitVec == null) return -1;

        return hit.hitVec.distanceTo(eyes);
    }
}
