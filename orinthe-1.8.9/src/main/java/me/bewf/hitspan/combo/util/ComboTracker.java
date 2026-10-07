package me.bewf.hitspan.combo.util;

import me.bewf.hitspan.combo.hud.ComboHud;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.hud.HitSpanHuds;
import me.bewf.hitspan.debug.HitSpanDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.effect.StatusEffect;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ComboTracker {

    private static final Minecraft mc = Minecraft.getInstance();

    public static int combo = 0;
    public static long lastHitTimeMs = 0;
    public static int lastTargetId = -1;

    public static ComboTracker INSTANCE;

    public ComboTracker() {
        INSTANCE = this;
        EventManager.register(TickEvent.End.class, event -> onClientTick());
    }

    public void onPlayerHurtByPlayer() {
        combo = 0;
        lastHitTimeMs = 0;
        lastTargetId = -1;

        HitSpanDebug.chat("COMBO RESET: hit by player");
    }

    private static final int MAX_QUEUE = 60;
    private static final long CONFIRM_WINDOW_MS = 1200L;
    private static final float FALL_DAMAGE_THRESHOLD = 3.0f;

    private static final long PACKET_WAIT_MS = 120L;
    private static final long PACKET_GRACE_MS = 200L;
    private static final long FALLBACK_MATCH_WINDOW_MS = 250L;

    private static final int RESIST_ELIGIBLE_MAX = 5;
    private static final int HURT_ELIGIBLE_MAX = 2;

    private static final long PACKET_MATCH_WINDOW_MS = 250L;

    private final ArrayDeque<PendingHit> pending = new ArrayDeque<>(MAX_QUEUE);
    private final HashMap<Integer, Long> lastPacketConfirmMs = new HashMap<Integer, Long>();

    private void onClientTick() {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) {
            pending.clear();
            lastPacketConfirmMs.clear();
        }

        if (mc.world == null) return;

        if (pending.isEmpty()) {
            tick();
            return;
        }

        long now = System.currentTimeMillis();

        purgeStale(now);
        purgeOldPacketMarks(now);

        if (pending.isEmpty()) {
            tick();
            return;
        }

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
                continue;
            }

            if (now - newest.attackTimeMs < PACKET_WAIT_MS) {
                continue;
            }

            PendingHit fb = pickBestFallbackCandidate(entityId, now, t);
            if (fb != null) {
                confirmHit(fb.target);
                HitSpanDebug.chat("CONFIRM hurt/resist id=" + entityId +
                        " ht=" + t.damagedTimer + "/" + fb.prevHurtTime +
                        " rt=" + t.invulnerableTimer + "/" + fb.prevResistTime);
                purgeEntity(entityId);
                continue;
            }

            if (Float.isNaN(newest.baselineHealth)) {
                newest.baselineHealth = hp;
            } else {
                if (hp + 0.001f < newest.baselineHealth) {
                    if (newest.prevBurning || newest.prevPoisoned || newest.prevWither || newest.prevFallDistance > FALL_DAMAGE_THRESHOLD) {
                        continue;
                    }

                    confirmHit(newest.target);
                    HitSpanDebug.chat("CONFIRM health id=" + entityId +
                            " hp " + newest.baselineHealth + "->" + hp);
                    purgeEntity(entityId);
                    continue;
                }
            }
        }

        tick();
    }

    public void onAttack(PlayerEntity attacker, Entity target) {
        if (mc.player == null) return;
        if (attacker == null || target == null) return;
        if (attacker.world == null || !attacker.world.isClient) return;

        ComboHud hud = HitSpanHuds.combo();
        boolean playersOnly = hud == null || hud.getPlayersOnly();

        if (!(target instanceof LivingEntity)) return;
        if (playersOnly && !(target instanceof PlayerEntity)) return;

        LivingEntity targetLiving = (LivingEntity) target;

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
                target,
                targetLiving.damagedTimer,
                targetLiving.invulnerableTimer,
                prevBurning,
                prevPoisoned,
                prevWither,
                prevFallDistance,
                System.currentTimeMillis()
        );
        pending.addLast(p);
    }

    public void confirmFromHurtPacket(int entityId) {
        if (pending.isEmpty()) return;

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

        confirmHit(best.target);
        lastPacketConfirmMs.put(entityId, now);
        purgeEntity(entityId);
    }

    public void onPlayerHurt() {
        combo = 0;
        lastHitTimeMs = 0;
        lastTargetId = -1;

        pending.clear();
        lastPacketConfirmMs.clear();
    }

    public static void hit(Entity target) {
        if (target == null) return;

        long now = System.currentTimeMillis();
        int targetId = target.getNetworkId();

        if (lastTargetId != targetId) {
            combo = 0;
            lastTargetId = targetId;
        }

        combo++;
        lastHitTimeMs = now;
    }

    private void confirmHit(Entity target) {
        hit(target);
    }

    public static void tick() {
        ComboHud hud = HitSpanHuds.combo();
        int resetSeconds = hud == null ? 5 : hud.getResetTimeSeconds();
        if (resetSeconds <= 0) resetSeconds = 5;

        long now = System.currentTimeMillis();
        long resetTimeMs = resetSeconds * 1000L;

        if (lastHitTimeMs > 0 && now - lastHitTimeMs > resetTimeMs) {
            combo = 0;
            lastHitTimeMs = 0;
            lastTargetId = -1;
        }
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
            if (t == null) {
                it.remove();
                continue;
            }
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

    private static class PendingHit {
        final int entityId;
        final Entity target;
        final int prevHurtTime;
        final int prevResistTime;
        final boolean prevBurning;
        final boolean prevPoisoned;
        final boolean prevWither;
        final float prevFallDistance;
        final long attackTimeMs;

        float baselineHealth = Float.NaN;

        PendingHit(int entityId,
                   Entity target,
                   int prevHurtTime,
                   int prevResistTime,
                   boolean prevBurning,
                   boolean prevPoisoned,
                   boolean prevWither,
                   float prevFallDistance,
                   long attackTimeMs) {
            this.entityId = entityId;
            this.target = target;
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
