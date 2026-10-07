package me.bewf.hitspan.Range.util;

import me.bewf.hitspan.Knockback.util.KnockbackTracker;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.HitSpanDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RangeTracker {

    private final Minecraft mc = Minecraft.getMinecraft();

    public static double lastRange = -1;
    public static long lastRangeTimeMs = 0;

    public static RangeTracker INSTANCE;
    public RangeTracker() { INSTANCE = this; }

    private static final int MAX_QUEUE = 60;
    private static final long CONFIRM_WINDOW_MS = 1200L;
    private static final long PACKET_WAIT_MS = 120L;
    private static final long PACKET_GRACE_MS = 200L;
    private static final long FALLBACK_MATCH_WINDOW_MS = 250L;
    private static final int RESIST_ELIGIBLE_MAX = 5;
    private static final int HURT_ELIGIBLE_MAX = 2;
    private static final long PACKET_MATCH_WINDOW_MS = 250L;

    private final ArrayDeque<PendingHit> pending = new ArrayDeque<>(MAX_QUEUE);
    private final HashMap<Integer, Long> lastPacketConfirmMs = new HashMap<>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.world == null) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.rangeHudEnabled) {
            pending.clear();
            lastPacketConfirmMs.clear();
            return;
        }
        if (pending.isEmpty()) return;

        long now = System.currentTimeMillis();
        purgeStale(now);
        purgeOldPacketMarks(now);
        if (pending.isEmpty()) return;

        Set<Integer> entityIds = collectEntityIds();
        for (int entityId : entityIds) {
            Entity e = mc.world.getEntityByID(entityId);
            if (!(e instanceof EntityLivingBase)) continue;

            EntityLivingBase t = (EntityLivingBase) e;
            PendingHit newest = pickNewest(entityId, now);
            if (newest == null) continue;

            float hp = t.getHealth();

            if (Float.isNaN(newest.baselineHealth)) newest.baselineHealth = hp;
            else if (hp + 0.001f < newest.baselineHealth) {
                double confirmedRange = recomputeFromSnapshot(newest);
                if (cfg.debugEnabled && cfg.debugConfirms) {
                    HitSpanDebug.chat("CONFIRM health id=" + entityId +
                            " r=" + fmt(confirmedRange) +
                            " hp " + newest.baselineHealth + "->" + hp);
                }
                confirmValue(confirmedRange);
                purgeEntity(entityId);
                continue;
            }

            Long lastPkt = lastPacketConfirmMs.get(entityId);
            if (lastPkt != null && now - lastPkt < PACKET_GRACE_MS) continue;
            if (now - newest.attackTimeMs < PACKET_WAIT_MS) continue;

            PendingHit fb = pickBestFallbackCandidate(entityId, now, t);
            if (fb != null) {
                double confirmedRange = recomputeFromSnapshot(fb);
                if (cfg.debugEnabled && cfg.debugConfirms) {
                    HitSpanDebug.chat("CONFIRM hurt/resist id=" + entityId +
                            " r=" + fmt(confirmedRange));
                }
                confirmValue(confirmedRange);
                purgeEntity(entityId);
            }
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event == null || event.getTarget() == null) return;
        if (event.getEntityPlayer() == null || !event.getEntityPlayer().world.isRemote) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.rangeHudEnabled) return;
        if (!(event.getTarget() instanceof EntityLivingBase)) return;
        if (cfg.rangePlayersOnly && !(event.getTarget() instanceof EntityPlayer)) return;

        EntityLivingBase target = (EntityLivingBase) event.getTarget();
        if (cfg.knockbackHudEnabled && (!cfg.knockbackPlayersOnly || target instanceof EntityPlayer)) {
            KnockbackTracker.beginTracking(target);
        }

        double maxReach = mc.player.capabilities.isCreativeMode ? 4.5D : 3.0D;
        double computed = computeEntityRayDistance(mc.player, target, 1.0F, maxReach);
        if (computed < 0) return;
        if (computed > maxReach) computed = maxReach;

        if (!cfg.confirmRangeOnHitConfirm) {
            confirmValue(computed);
            return;
        }

        if (pending.size() >= MAX_QUEUE) pending.pollFirst();
        float border = target.getCollisionBorderSize();
        AxisAlignedBB bbSnap = target.getEntityBoundingBox().expand(border, border, border);

        PendingHit p = new PendingHit(target.getEntityId(), computed, mc.player.rotationYaw, mc.player.rotationPitch,
                maxReach, bbSnap, target.hurtTime, target.hurtResistantTime, System.currentTimeMillis());
        pending.addLast(p);
    }

    // Make this public so INSTANCE can call it
    public void confirmFromHurtPacket(int entityId) {
        if (mc.world == null || pending.isEmpty()) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.rangeHudEnabled) return;

        long now = System.currentTimeMillis();
        PendingHit best = pickBestForPacket(entityId, now);
        if (best == null) return;

        double confirmedRange = recomputeFromSnapshot(best);
        confirmValue(confirmedRange);
        lastPacketConfirmMs.put(entityId, now);
        purgeEntity(entityId);
    }

    private PendingHit pickNewest(int entityId, long now) {
        PendingHit best = null;
        for (PendingHit p : pending) {
            if (p.entityId != entityId) continue;
            long age = now - p.attackTimeMs;
            if (age > CONFIRM_WINDOW_MS) continue;
            if (best == null || p.attackTimeMs > best.attackTimeMs) best = p;
        }
        return best;
    }

    private PendingHit pickBestForPacket(int entityId, long now) {
        PendingHit bestEligible = null;
        PendingHit bestAny = null;
        long bestEligibleAge = Long.MAX_VALUE;

        for (PendingHit p : pending) {
            if (p.entityId != entityId) continue;
            long age = now - p.attackTimeMs;
            if (age > CONFIRM_WINDOW_MS) continue;

            if (bestAny == null || p.attackTimeMs > bestAny.attackTimeMs) bestAny = p;

            boolean eligibleCooldown = p.prevResistTime <= RESIST_ELIGIBLE_MAX || p.prevHurtTime <= HURT_ELIGIBLE_MAX;
            if (!eligibleCooldown) continue;
            if (age > PACKET_MATCH_WINDOW_MS) continue;

            if (bestEligible == null || age < bestEligibleAge) {
                bestEligible = p;
                bestEligibleAge = age;
            }
        }

        return bestEligible != null ? bestEligible : bestAny;
    }

    private PendingHit pickBestFallbackCandidate(int entityId, long now, EntityLivingBase t) {
        PendingHit best = null;
        long bestDelta = Long.MAX_VALUE;

        for (PendingHit p : pending) {
            if (p.entityId != entityId) continue;
            long age = now - p.attackTimeMs;
            if (age < PACKET_WAIT_MS || age > FALLBACK_MATCH_WINDOW_MS || age > CONFIRM_WINDOW_MS) continue;
            boolean hurtReset = t.hurtTime > 0 && t.hurtTime > p.prevHurtTime && p.prevHurtTime <= HURT_ELIGIBLE_MAX;
            boolean resistReset = t.hurtResistantTime > 0 && t.hurtResistantTime > p.prevResistTime && p.prevResistTime <= RESIST_ELIGIBLE_MAX;
            if (!(hurtReset || resistReset)) continue;
            long delta = Math.abs(now - p.attackTimeMs);
            if (best == null || delta < bestDelta) {
                best = p;
                bestDelta = delta;
            }
        }
        return best;
    }

    private double recomputeFromSnapshot(PendingHit p) {
        if (mc.player == null) return p.range;
        double r = computeRayDistanceAgainstBox(mc.player, p.bbSnap, p.yaw, p.pitch, 1.0F, p.maxReach);
        if (r < 0) return p.range;
        if (r > p.maxReach) r = p.maxReach;
        return r;
    }

    private static double computeRayDistanceAgainstBox(EntityLivingBase player, AxisAlignedBB bb, float yaw, float pitch, float partialTicks, double maxDist) {
        Vec3d eyes = player.getPositionEyes(partialTicks);
        float yawRad = (float) Math.toRadians(-yaw) - (float) Math.PI;
        float pitchRad = (float) Math.toRadians(-pitch);
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);

        Vec3d look = new Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
        Vec3d end = eyes.add(look.x * maxDist, look.y * maxDist, look.z * maxDist);

        RayTraceResult hit = bb.calculateIntercept(eyes, end);
        if (hit == null || hit.hitVec == null) return -1;
        return hit.hitVec.distanceTo(eyes);
    }

    private void confirmValue(double range) {
        lastRange = range;
        lastRangeTimeMs = System.currentTimeMillis();
    }

    private void purgeStale(long now) {
        pending.removeIf(p -> now - p.attackTimeMs > CONFIRM_WINDOW_MS);
    }

    private void purgeOldPacketMarks(long now) {
        lastPacketConfirmMs.entrySet().removeIf(e -> e.getValue() == null || now - e.getValue() > 5000L);
    }

    private Set<Integer> collectEntityIds() {
        Set<Integer> ids = new HashSet<>();
        for (PendingHit p : pending) ids.add(p.entityId);
        return ids;
    }

    private void purgeEntity(int entityId) {
        pending.removeIf(p -> p.entityId == entityId);
    }

    private static double computeEntityRayDistance(EntityLivingBase player, Entity target, float partialTicks, double maxDist) {
        Vec3d eyes = player.getPositionEyes(partialTicks);
        Vec3d look = player.getLook(partialTicks);
        Vec3d end = eyes.add(look.x * maxDist, look.y * maxDist, look.z * maxDist);

        float border = target.getCollisionBorderSize();
        AxisAlignedBB bb = target.getEntityBoundingBox().expand(border, border, border);

        RayTraceResult hit = bb.calculateIntercept(eyes, end);
        if (hit == null || hit.hitVec == null) return -1;
        return hit.hitVec.distanceTo(eyes);
    }

    private static String fmt(double d) {
        long x = (long) (d * 1000.0);
        return String.valueOf(x / 1000.0);
    }

    private static class PendingHit {
        final int entityId;
        final double range;
        final float yaw;
        final float pitch;
        final double maxReach;
        final AxisAlignedBB bbSnap;
        final int prevHurtTime;
        final int prevResistTime;
        final long attackTimeMs;
        float baselineHealth = Float.NaN;

        PendingHit(int entityId, double range, float yaw, float pitch, double maxReach, AxisAlignedBB bbSnap,
                   int prevHurtTime, int prevResistTime, long attackTimeMs) {
            this.entityId = entityId;
            this.range = range;
            this.yaw = yaw;
            this.pitch = pitch;
            this.maxReach = maxReach;
            this.bbSnap = bbSnap;
            this.prevHurtTime = prevHurtTime;
            this.prevResistTime = prevResistTime;
            this.attackTimeMs = attackTimeMs;
        }
    }
}
