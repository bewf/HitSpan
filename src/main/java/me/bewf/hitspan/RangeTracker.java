package me.bewf.hitspan;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class RangeTracker {

    private final Minecraft mc = Minecraft.getMinecraft();

    // Public values HUD reads
    public static double lastRange = -1;
    public static long lastRangeTimeMs = 0;

    // Pending hit confirmation
    private int pendingEntityId = -1;
    private int pendingPrevHurtTime = 0;
    private int pendingTicksLeft = 0;
    private double pendingRange = -1;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.target == null) return;

        // Only care about living targets (players/mobs)
        if (!(event.target instanceof EntityLivingBase)) return;

        EntityLivingBase target = (EntityLivingBase) event.target;

        double maxReach = mc.thePlayer.capabilities.isCreativeMode ? 4.5D : 3.0D;

        double computed = computeEntityRayDistance(mc.thePlayer, target, 1.0F, maxReach);

        // If we can't compute, don't update anything
        if (computed < 0) return;

        // Clamp to the same max reach used by the ray
        if (computed > maxReach) computed = maxReach;

        pendingEntityId = target.getEntityId();
        pendingPrevHurtTime = target.hurtTime;
        pendingTicksLeft = 3; // wait up to 3 client ticks to confirm actual damage
        pendingRange = computed;
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

                // Confirm real hit: hurtTime increased (damage animation triggered)
                if (target.hurtTime > pendingPrevHurtTime) {
                    lastRange = pendingRange;
                    lastRangeTimeMs = System.currentTimeMillis();

                    // Start KB tracking off this confirmed hit
                    KnockbackTracker.beginTracking(target);

                    // Clear pending
                    pendingEntityId = -1;
                    pendingTicksLeft = 0;
                    pendingRange = -1;
                }
            }

            // If timed out, clear pending (prevents updates on spam clicks)
            if (pendingTicksLeft == 0) {
                pendingEntityId = -1;
                pendingRange = -1;
            }
        }
    }

    // Ray from player eyes along look vector, intersect target AABB like MC does.
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
