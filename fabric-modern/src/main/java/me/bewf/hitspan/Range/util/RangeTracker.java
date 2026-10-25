package me.bewf.hitspan.Range.util;

import me.bewf.hitspan.Knockback.util.KnockbackTracker;
import me.bewf.hitspan.Range.hud.RangeHud;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.HitSpanDebug;
import me.bewf.hitspan.hud.HitSpanHuds;
import me.bewf.hitspan.util.McCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class RangeTracker {

    private static final float FALL_DAMAGE_THRESHOLD = 3.0f;
    private static final double MAX_RAY = 6.0D;

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
    private final HashMap<Integer, Long> lastPacketConfirmMs = new HashMap<>();

    public RangeTracker() {
        EventManager.register(TickEvent.End.class, event -> onClientTick());
    }

    private void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        if (mc.level == null) {
            pending.clear();
            lastPacketConfirmMs.clear();
            return;
        }

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
            Entity e = mc.level.getEntity(entityId);
            if (!(e instanceof LivingEntity t)) continue;

            PendingHit newest = pickNewest(entityId, now);
            if (newest == null) continue;

            float hp = McCompat.health(t);

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
                double confirmedRange = fb.range;

                if (cfg.debugEnabled && cfg.debugConfirms) {
                    HitSpanDebug.chat("CONFIRM hurt/resist id=" + entityId +
                            " r=" + fmt(confirmedRange) +
                            " ht=" + McCompat.hurtTime(t) + "/" + fb.prevHurtTime +
                            " rt=" + McCompat.invulnerableTime(t) + "/" + fb.prevResistTime);
                }

                confirmValue(confirmedRange);
                HitSpanDebug.rangeServerCompare(entityId, confirmedRange, "hurt/resist", fb.attackTimeMs);
                purgeEntity(entityId);
                continue;
            }

            if (Float.isNaN(newest.baselineHealth)) {
                newest.baselineHealth = hp;
            } else if (hp + 0.001f < newest.baselineHealth) {
                if (newest.prevBurning || newest.prevPoisoned || newest.prevWither || newest.prevFallDistance > FALL_DAMAGE_THRESHOLD) {
                    if (cfg.debugEnabled && cfg.debugConfirms) {
                        HitSpanDebug.chat("SKIP health confirm (env damage snapshot) id=" + entityId);
                    }
                    continue;
                }

                double confirmedRange = newest.range;

                if (cfg.debugEnabled && cfg.debugConfirms) {
                    HitSpanDebug.chat("CONFIRM health id=" + entityId +
                            " r=" + fmt(confirmedRange) +
                            " hp " + newest.baselineHealth + "->" + hp);
                }

                confirmValue(confirmedRange);
                HitSpanDebug.rangeServerCompare(entityId, confirmedRange, "health", newest.attackTimeMs);
                purgeEntity(entityId);
            }
        }
    }

    public void onAttack(Player attacker, Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        if (attacker == null || target == null) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return;

        if (!(target instanceof LivingEntity targetLiving)) return;

        KnockbackTracker.beginTracking(targetLiving);

        RangeHud rangeHud = HitSpanHuds.range();
        boolean playersOnly = rangeHud == null || rangeHud.getPlayersOnly();
        boolean confirmedOnly = rangeHud == null || rangeHud.getConfirmedHitOnly();

        if (playersOnly && !(target instanceof Player)) return;

        double computed = computeEntityRayDistance(mc.player, target, 1.0F, MAX_RAY);
        if (computed < 0) return;
        if (computed > MAX_RAY) computed = MAX_RAY;

        if (!confirmedOnly) {
            confirmValue(computed);

            if (cfg.debugEnabled && cfg.debugAttacks) {
                HitSpanDebug.chat("RANGE immediate id=" + target.getId() + " r=" + fmt(computed));
            }
        } else {
            if (pending.size() >= MAX_QUEUE) pending.pollFirst();

            PendingHit p = new PendingHit(
                    target.getId(),
                    computed,
                    McCompat.hurtTime(targetLiving),
                    McCompat.invulnerableTime(targetLiving),
                    McCompat.burning(targetLiving),
                    McCompat.poisoned(targetLiving),
                    McCompat.withered(targetLiving),
                    McCompat.fallDistance(targetLiving),
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
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        if (pending.isEmpty()) return;

        RangeHud rangeHud = HitSpanHuds.range();
        if (rangeHud != null && !rangeHud.getConfirmedHitOnly()) return;

        long now = System.currentTimeMillis();

        purgeStale(now);
        purgeOldPacketMarks(now);

        PendingHit best = pickBestForPacket(entityId, now);
        if (best == null) return;

        Entity e = mc.level.getEntity(entityId);
        if (!(e instanceof LivingEntity)) {
            purgeEntity(entityId);
            return;
        }

        confirmValue(best.range);
        lastPacketConfirmMs.put(entityId, now);
        purgeEntity(entityId);
    }

    public void rejectFromForeignDamage(int entityId) {
        lastPacketConfirmMs.put(entityId, System.currentTimeMillis());
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

        int hurt = McCompat.hurtTime(t);
        int resist = McCompat.invulnerableTime(t);

        for (PendingHit p : pending) {
            if (p.entityId != entityId) continue;

            long age = now - p.attackTimeMs;
            if (age > CONFIRM_WINDOW_MS) continue;
            if (age < PACKET_WAIT_MS) continue;
            if (age > FALLBACK_MATCH_WINDOW_MS) continue;

            boolean hurtReset =
                    (hurt > 0) &&
                            (hurt > p.prevHurtTime) &&
                            (p.prevHurtTime <= HURT_ELIGIBLE_MAX);

            boolean resistReset =
                    (resist > 0) &&
                            (resist > p.prevResistTime) &&
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
        Iterator<Long> it = lastPacketConfirmMs.values().iterator();
        while (it.hasNext()) {
            Long t = it.next();
            if (t == null || now - t > 5000L) it.remove();
        }
    }

    private Set<Integer> collectEntityIds() {
        Set<Integer> ids = new HashSet<>();
        for (PendingHit p : pending) ids.add(p.entityId);
        return ids;
    }

    private void purgeEntity(int entityId) {
        pending.removeIf(p -> p.entityId == entityId);
    }

    private void confirmValue(double r) {
        lastRange = r;
        lastRangeTimeMs = System.currentTimeMillis();
    }

    private static double computeEntityRayDistance(Entity viewer, Entity target, float partialTicks, double maxDist) {
        Vec3 eyes = viewer.getEyePosition(partialTicks);
        Vec3 look = viewer.getViewVector(partialTicks);

        float border = target.getPickRadius();
        AABB bb = target.getBoundingBox().inflate(border, border, border);

        return McCompat.rayDistance(eyes, look, bb, maxDist);
    }

    private static String fmt(double d) {
        long x = (long) (d * 1000.0);
        return String.valueOf(x / 1000.0);
    }

    private static class PendingHit {
        final int entityId;
        final double range;

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
                   int prevHurtTime,
                   int prevResistTime,
                   boolean prevBurning,
                   boolean prevPoisoned,
                   boolean prevWither,
                   float prevFallDistance,
                   long attackTimeMs) {
            this.entityId = entityId;
            this.range = range;
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
