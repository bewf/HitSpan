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
import org.lwjgl.input.Mouse;

public class RangeTracker {

    private final Minecraft mc = Minecraft.getMinecraft();

    public static double lastRange = -1;
    public static long lastRangeTimeMs = 0;

    public static RangeTracker INSTANCE;
    public RangeTracker() { INSTANCE = this; }

    private boolean leftWasDown = false;
    private int clickSeq = 0;
    private int lastProcessedClickSeq = -1;

    private int pendingEntityId = -1;
    private double pendingRange = -1;
    private int pendingTicksLeft = 0;

    private int pendingPrevHurtTime = 0;
    private int pendingPrevHurtResistantTime = 0;

    private long lastConfirmTimeMs = 0;

    private static final int PENDING_WINDOW_TICKS = 10;
    private static final long CONFIRM_DEDUPE_MS = 120;

    private long lastImmediateWorldTime = -1;
    private int lastImmediateEntityId = -1;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null) return;

        boolean leftDown = Mouse.isButtonDown(0);
        if (leftDown && !leftWasDown) {
            clickSeq++;
        }
        leftWasDown = leftDown;

        if (pendingEntityId != -1 && pendingTicksLeft > 0) {
            pendingTicksLeft--;

            Entity e = mc.theWorld.getEntityByID(pendingEntityId);
            if (e instanceof EntityLivingBase) {
                EntityLivingBase t = (EntityLivingBase) e;

                int curHurtTime = t.hurtTime;
                int curResist = t.hurtResistantTime;

                boolean hurtTimeBumped = curHurtTime > pendingPrevHurtTime + 1;
                boolean resistBumped = curResist > pendingPrevHurtResistantTime + 2;

                if (hurtTimeBumped || resistBumped) {
                    confirmPending(t);
                    clearPending();
                    return;
                }
            }

            if (pendingTicksLeft == 0) {
                clearPending();
            }
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event == null || event.target == null) return;

        if (event.entityPlayer == null || event.entityPlayer.worldObj == null) return;
        if (!event.entityPlayer.worldObj.isRemote) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;

        if (cfg != null && cfg.playersOnly && !(event.target instanceof EntityPlayer)) return;
        if (!(event.target instanceof EntityLivingBase)) return;

        EntityLivingBase target = (EntityLivingBase) event.target;

        double maxReach = mc.thePlayer.capabilities.isCreativeMode ? 4.5D : 3.0D;
        double computed = computeEntityRayDistance(mc.thePlayer, target, 1.0F, maxReach);
        if (computed < 0) return;
        if (computed > maxReach) computed = maxReach;

        boolean confirmedOnly = cfg != null && cfg.confirmRangeOnHitConfirm;

        if (!confirmedOnly) {
            long wt = mc.theWorld.getTotalWorldTime();
            int id = target.getEntityId();

            if (wt == lastImmediateWorldTime && id == lastImmediateEntityId) return;
            lastImmediateWorldTime = wt;
            lastImmediateEntityId = id;

            lastRange = computed;
            lastRangeTimeMs = System.currentTimeMillis();
            KnockbackTracker.beginTracking(target);
            return;
        }

        if (clickSeq == lastProcessedClickSeq) return;
        lastProcessedClickSeq = clickSeq;

        pendingEntityId = target.getEntityId();
        pendingRange = computed;
        pendingPrevHurtTime = target.hurtTime;
        pendingPrevHurtResistantTime = target.hurtResistantTime;
        pendingTicksLeft = PENDING_WINDOW_TICKS;
    }

    public void confirmFromHurtPacket(int entityId) {
        if (mc.theWorld == null) return;
        if (pendingEntityId == -1) return;
        if (entityId != pendingEntityId) return;
        if (pendingTicksLeft <= 0) return;

        Entity e = mc.theWorld.getEntityByID(pendingEntityId);
        if (e instanceof EntityLivingBase) {
            confirmPending((EntityLivingBase) e);
        }

        clearPending();
    }

    private void confirmPending(EntityLivingBase target) {
        long now = System.currentTimeMillis();
        if (now - lastConfirmTimeMs < CONFIRM_DEDUPE_MS) return;
        lastConfirmTimeMs = now;

        if (pendingRange >= 0) {
            lastRange = pendingRange;
            lastRangeTimeMs = now;
        }

        KnockbackTracker.beginTracking(target);
    }

    private void clearPending() {
        pendingEntityId = -1;
        pendingRange = -1;
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
