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

    // Used by the mixin to confirm hits
    public static RangeTracker INSTANCE;
    public RangeTracker() { INSTANCE = this; }

    private int pendingEntityId = -1;
    private int pendingPrevHurtTime = 0;
    private int pendingPrevHurtResistantTime = 0;
    private int pendingTicksLeft = 0;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.target == null) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg != null && cfg.playersOnly && !(event.target instanceof EntityPlayer)) return;
        if (!(event.target instanceof EntityLivingBase)) return;

        EntityLivingBase target = (EntityLivingBase) event.target;

        double maxReach = mc.thePlayer.capabilities.isCreativeMode ? 4.5D : 3.0D;
        double computed = computeEntityRayDistance(mc.thePlayer, target, 1.0F, maxReach);
        if (computed < 0) return;
        if (computed > maxReach) computed = maxReach;

        // ✅ ALWAYS update range immediately (even if server later rejects)
        lastRange = computed;
        lastRangeTimeMs = System.currentTimeMillis();

        // Pending confirmation (ONLY used to start KB tracking)
        pendingEntityId = target.getEntityId();
        pendingPrevHurtTime = target.hurtTime;
        pendingPrevHurtResistantTime = target.hurtResistantTime;
        pendingTicksLeft = 10;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null) return;

        if (pendingTicksLeft > 0 && pendingEntityId != -1) {
            pendingTicksLeft--;

            Entity e = mc.theWorld.getEntityByID(pendingEntityId);
            if (e instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) e;

                boolean hurtTimeIncreased = target.hurtTime > pendingPrevHurtTime;
                boolean resistantIncreased = target.hurtResistantTime > pendingPrevHurtResistantTime;

                if (hurtTimeIncreased || resistantIncreased) {
                    // ✅ Confirmed hit: start KB tracking
                    KnockbackTracker.beginTracking(target);
                    clearPending();
                    return;
                }
            }

            if (pendingTicksLeft == 0) {
                clearPending();
            }
        }
    }

    // Called by mixin when we receive the server "hurt animation" packet (opcode 2)
    public void confirmFromHurtPacket(int entityId) {
        if (pendingEntityId == -1) return;
        if (entityId != pendingEntityId) return;
        if (pendingTicksLeft <= 0) return;
        if (mc.theWorld == null) return;

        Entity e = mc.theWorld.getEntityByID(pendingEntityId);
        if (e instanceof EntityLivingBase) {
            // ✅ Confirmed hit: start KB tracking
            KnockbackTracker.beginTracking((EntityLivingBase) e);
        }
        clearPending();
    }

    private void clearPending() {
        pendingEntityId = -1;
        pendingTicksLeft = 0;
        pendingPrevHurtTime = 0;
        pendingPrevHurtResistantTime = 0;
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
