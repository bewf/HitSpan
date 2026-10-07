package me.bewf.hitspan.Range.util;

import me.bewf.hitspan.Knockback.util.KnockbackTracker;
import me.bewf.hitspan.Range.hud.RangeHud;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.hud.HitSpanHuds;
import me.bewf.hitspan.debug.HitSpanDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.effect.StatusEffect;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.HitResult;
import net.minecraft.util.math.Vec3d;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RangeTracker {

    private final Minecraft mc = Minecraft.getInstance();

    private static final float FALL_DAMAGE_THRESHOLD = 3.0f;

    public static double lastRange = -1;
    public static long lastRangeTimeMs = 0;

    public static RangeTracker INSTANCE;

    private static final int MAX_QUEUE = 60;
    private static final long CONFIRM_WINDOW_MS = 1200L;

    private static final long PACKET_WAIT_MS = 120L;
    private static final long PACKET_GRACE_MS = 200L;
    private static final long FALLBACK_MATCH_WINDOW_MS = 250L;

    private static final int RESIST_ELIGIBLE_MAX = 5;
    private static final int HURT_ELIGIBLE_MAX = 2;

    private static final long PACKET_MATCH_WINDOW_MS = 250L;

    private final ArrayDeque<PendingHit> pending = new ArrayDeque<>(MAX_QUEUE);
    private final HashMap<Integer, Long> lastPacketConfirmMs = new HashMap<Integer, Long>();

    public RangeTracker() {
        EventManager.register(TickEvent.End.class, event -> onClientTick());
    }

    private void onClientTick() {
        if (mc.world == null) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) {
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
            Entity e = mc.world.getEntity(entityId);
            if (!(e instanceof LivingEntity)) continue;

            LivingEntity t = (LivingEntity) e;

            PendingHit newest = pickNewest(entityId, now);
            if (newest == null) continue;

            float hp = t.getHealth();

            Long lastPkt = lastPacketConfirmMs.get(entityId);
            if (lastPkt != null && now - lastPkt < PACKET_GRACE_MS) {
                if (cfg.debugEnabled && cfg.debugVerbose) {
                    HitSpanDebug.chatVerbose("skip fallback (packet grace) id=" + entityId +
                            " age=" + (now - lastPkt));
                }
                continue;
            }

            if (now - newest.attackTimeMs < PACKET_WAIT_MS) {
                if (cfg.debugEnabled && cfg.debugVerbose) {
                    HitSpanDebug.chatVerbose("wait packet id=" + entityId +
                            " bestAge=" + (now - newest.attackTimeMs));
                }
                continue;
            }

            PendingHit fb = pickBestFallbackCandidate(entityId, now, t);
            if (fb != null) {
                double confirmedRange = recomputeFromSnapshot(fb);

                if (cfg.debugEnabled && cfg.debugConfirms) {
                    HitSpanDebug.chat("CONFIRM hurt/resist id=" + entityId +
                            " r=" + fmt(confirmedRange) +
                            " ht=" + t.damagedTimer + "/" + fb.prevHurtTime +
                            " rt=" + t.invulnerableTimer + "/" + fb.prevResistTime);
                }

                confirmValue(confirmedRange);
                HitSpanDebug.rangeServerCompare(entityId, confirmedRange, "hurt/resist", fb.attackTimeMs);
                purgeEntity(entityId);
                continue;
            }

            if (Float.isNaN(newest.baselineHealth)) {
                newest.baselineHealth = hp;
            } else {
                if (hp + 0.001f < newest.baselineHealth) {
                    if (newest.prevBurning || newest.prevPoisoned || newest.prevWither || newest.prevFallDistance > FALL_DAMAGE_THRESHOLD) {
                        if (cfg.debugEnabled && cfg.debugConfirms) {
                            HitSpanDebug.chat("SKIP health confirm (env damage snapshot) id=" + entityId);
                        }
                        continue;
                    }

                    double confirmedRange = recomputeFromSnapshot(newest);

                    if (cfg.debugEnabled && cfg.debugConfirms) {
                        HitSpanDebug.chat("CONFIRM health id=" + entityId +
                                " r=" + fmt(confirmedRange) +
                                " hp " + newest.baselineHealth + "->" + hp);
                    }

                    confirmValue(confirmedRange);
                    HitSpanDebug.rangeServerCompare(entityId, confirmedRange, "health", newest.attackTimeMs);
                    purgeEntity(entityId);
                    continue;
                }
            }
        }
    }

    public void onAttack(PlayerEntity attacker, Entity target) {
        if (mc.player == null || mc.world == null) return;
        if (attacker == null || target == null) return;
        if (attacker.world == null || !attacker.world.isClient) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return;

        if (!(target instanceof LivingEntity)) return;
        LivingEntity targetLiving = (LivingEntity) target;

        KnockbackTracker.beginTracking(targetLiving);

        RangeHud rangeHud = HitSpanHuds.range();
        boolean playersOnly = rangeHud == null || rangeHud.getPlayersOnly();
        boolean confirmedOnly = rangeHud == null || rangeHud.getConfirmedHitOnly();

        if (playersOnly && !(target instanceof PlayerEntity)) return;

        double maxReach = mc.player.abilities.creativeMode ? 4.5D : 3.0D;

        float yaw = mc.player.yaw;
        float pitch = mc.player.pitch;

        double computed = computeEntityRayDistance(mc.player, target, 1.0F, maxReach);
        if (computed < 0) return;
        if (computed > maxReach) computed = maxReach;

        if (!confirmedOnly) {
            confirmValue(computed);

            if (cfg.debugEnabled && cfg.debugAttacks) {
                HitSpanDebug.chat("RANGE immediate id=" + target.getNetworkId() + " r=" + fmt(computed));
            }
        } else {
            if (pending.size() >= MAX_QUEUE) pending.pollFirst();

            float border = targetLiving.getPickRadius();
            Box bb = targetLiving.getShape();
            Box bbSnap = Box.of(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ)
                    .expanded(border, border, border);

            boolean prevBurning = targetLiving.isOnFire();
            boolean prevPoisoned = targetLiving.hasStatusEffect(StatusEffect.POISON);
            boolean prevWither = targetLiving.hasStatusEffect(StatusEffect.WITHER);
            float prevFallDistance = targetLiving.fallDistance;

            PendingHit p = new PendingHit(
                    target.getNetworkId(),
                    computed,
                    yaw,
                    pitch,
                    maxReach,
                    bbSnap,
                    targetLiving.damagedTimer,
                    targetLiving.invulnerableTimer,
                    prevBurning,
                    prevPoisoned,
                    prevWither,
                    prevFallDistance,
                    System.currentTimeMillis()
            );
            pending.addLast(p);

            if (cfg.debugEnabled && cfg.debugAttacks) {
                HitSpanDebug.chat("ENQUEUE id=" + p.entityId +
                        " r=" + fmt(p.range) +
                        " ht=" + p.prevHurtTime +
                        " rt=" + p.prevResistTime +
                        " burn=" + p.prevBurning +
                        " poison=" + p.prevPoisoned +
                        " wither=" + p.prevWither +
                        " fall=" + p.prevFallDistance +
                        " q=" + pending.size());
            }
        }
    }

    public void confirmFromHurtPacket(int entityId) {
        if (mc.world == null) return;
        if (pending.isEmpty()) return;

        RangeHud rangeHud = HitSpanHuds.range();
        if (rangeHud != null && !rangeHud.getConfirmedHitOnly()) return;

        long now = System.currentTimeMillis();

        purgeStale(now);
        purgeOldPacketMarks(now);

        PendingHit best = pickBestForPacket(entityId, now);
        if (best == null) return;

        Entity e = mc.world.getEntity(entityId);
        if (!(e instanceof LivingEntity)) {
            purgeEntity(entityId);
            return;
        }

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

            boolean eligibleCooldown = (p.prevResistTime <= RESIST_ELIGIBLE_MAX) || (p.prevHurtTime <= HURT_ELIGIBLE_MAX);
            if (!eligibleCooldown) continue;

            if (age > PACKET_MATCH_WINDOW_MS) continue;

            if (bestEligible == null || age < bestEligibleAge) {
                bestEligible = p;
                bestEligibleAge = age;
            }
        }

        return bestEligible != null ? bestEligible : bestAny;
    }

    private PendingHit pickBestFallbackCandidate(int entityId, long now, LivingEntity t) {
        PendingHit best = null;
        long bestDelta = Long.MAX_VALUE;

        for (PendingHit p : pending) {
            if (p.entityId != entityId) continue;

            long age = now - p.attackTimeMs;
            if (age > CONFIRM_WINDOW_MS) continue;
            if (age < PACKET_WAIT_MS) continue;
            if (age > FALLBACK_MATCH_WINDOW_MS) continue;

            boolean hurtReset =
                    (t.damagedTimer > 0) &&
                            (t.damagedTimer > p.prevHurtTime) &&
                            (p.prevHurtTime <= HURT_ELIGIBLE_MAX);

            boolean resistReset =
                    (t.invulnerableTimer > 0) &&
                            (t.invulnerableTimer > p.prevResistTime) &&
                            (p.prevResistTime <= RESIST_ELIGIBLE_MAX);

            if (!(hurtReset || resistReset)) continue;

            long delta = Math.abs(now - p.attackTimeMs);
            if (best == null || delta < bestDelta) {
                best = p;
                bestDelta = delta;
            }
        }

        return best;
    }

    private void purgeStale(long now) {
        Iterator<PendingHit> it = pending.iterator();
        while (it.hasNext()) {
            PendingHit p = it.next();
            if (now - p.attackTimeMs > CONFIRM_WINDOW_MS) it.remove();
        }
    }

    private void purgeOldPacketMarks(long now) {
        Iterator<Integer> it = lastPacketConfirmMs.keySet().iterator();
        while (it.hasNext()) {
            Integer id = it.next();
            Long t = lastPacketConfirmMs.get(id);
            if (t == null) { it.remove(); continue; }
            if (now - t > 5000L) it.remove();
        }
    }

    private Set<Integer> collectEntityIds() {
        Set<Integer> ids = new HashSet<Integer>();
        for (PendingHit p : pending) ids.add(p.entityId);
        return ids;
    }

    private void purgeEntity(int entityId) {
        Iterator<PendingHit> it = pending.iterator();
        while (it.hasNext()) {
            if (it.next().entityId == entityId) it.remove();
        }
    }

    private void confirmValue(double r) {
        lastRange = r;
        lastRangeTimeMs = System.currentTimeMillis();
    }

    private double recomputeFromSnapshot(PendingHit p) {
        if (mc.player == null) return p.range;

        double r = computeRayDistanceAgainstBox(mc.player, p.bbSnap, p.yaw, p.pitch, 1.0F, p.maxReach);
        if (r < 0) return p.range;
        if (r > p.maxReach) r = p.maxReach;
        if (r < 0) r = 0;
        return r;
    }

    private static double computeRayDistanceAgainstBox(LivingEntity player,
                                                       Box bb,
                                                       float yaw,
                                                       float pitch,
                                                       float partialTicks,
                                                       double maxDist) {
        Vec3d eyes = player.getEyePosition(partialTicks);

        float yawRad = (float) Math.toRadians(-yaw) - (float) Math.PI;
        float pitchRad = (float) Math.toRadians(-pitch);

        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);

        Vec3d look = new Vec3d(
                (double) (sinYaw * cosPitch),
                (double) (sinPitch),
                (double) (cosYaw * cosPitch)
        );

        Vec3d end = eyes.add(look.x * maxDist, look.y * maxDist, look.z * maxDist);

        HitResult hit = bb.clip(eyes, end);
        if (hit == null || hit.facePos == null) return -1;

        return hit.facePos.distanceTo(eyes);
    }

    private static double computeEntityRayDistance(LivingEntity player, Entity target, float partialTicks, double maxDist) {
        Vec3d eyes = player.getEyePosition(partialTicks);
        Vec3d look = player.getRotationVector(partialTicks);
        Vec3d end = eyes.add(look.x * maxDist, look.y * maxDist, look.z * maxDist);

        float border = target.getPickRadius();
        Box bb = target.getShape().expanded(border, border, border);

        HitResult hit = bb.clip(eyes, end);
        if (hit == null || hit.facePos == null) return -1;

        return hit.facePos.distanceTo(eyes);
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

        final Box bbSnap;

        final int prevHurtTime;
        final int prevResistTime;
        final boolean prevBurning;
        final boolean prevPoisoned;
        final boolean prevWither;
        final float prevFallDistance;
        final long attackTimeMs;

        float baselineHealth = Float.NaN;

        PendingHit(int entityId,
                   double range,
                   float yaw,
                   float pitch,
                   double maxReach,
                   Box bbSnap,
                   int prevHurtTime,
                   int prevResistTime,
                   boolean prevBurning,
                   boolean prevPoisoned,
                   boolean prevWither,
                   float prevFallDistance,
                   long attackTimeMs) {
            this.entityId = entityId;
            this.range = range;
            this.yaw = yaw;
            this.pitch = pitch;
            this.maxReach = maxReach;
            this.bbSnap = bbSnap;
            this.prevHurtTime = prevHurtTime;
            this.prevResistTime = prevResistTime;
            this.prevBurning = prevBurning;
            this.prevPoisoned = prevPoisoned;
            this.prevWither = prevWither;
            this.prevFallDistance = prevFallDistance;
            this.attackTimeMs = attackTimeMs;
        }
    }
}
