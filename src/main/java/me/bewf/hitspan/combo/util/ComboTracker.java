package me.bewf.hitspan.combo.util;

import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.HitSpanDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ComboTracker {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static int combo = 0;
    public static long lastHitTimeMs = 0;
    public static int lastTargetId = -1;

    public static ComboTracker INSTANCE;
    public ComboTracker() { INSTANCE = this; }

    // Reset combo when the local player is damaged by another player
    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (event == null || event.entity == null) return;
        // Only care about the local player being hurt
        if (mc.thePlayer == null) return;
        if (event.entity != mc.thePlayer) return;

        // The damage source may have an entity attacker; check if it's a player
        if (event.source != null && event.source.getEntity() instanceof EntityPlayer) {
            // Reset combo fully
            combo = 0;
            lastHitTimeMs = 0;
            lastTargetId = -1;

            HitSpanDebug.chat("COMBO RESET: hit by player");
        }
    }

    private static final int MAX_QUEUE = 60;
    private static final long CONFIRM_WINDOW_MS = 1200L;

    // Packet / fallback tuning
    private static final long PACKET_WAIT_MS = 120L;
    private static final long PACKET_GRACE_MS = 200L;
    private static final long FALLBACK_MATCH_WINDOW_MS = 250L;

    // For invuln spam: prefer queued clicks that happened when target wasn't deep in cooldown
    private static final int RESIST_ELIGIBLE_MAX = 5;
    private static final int HURT_ELIGIBLE_MAX = 2;

    private static final long PACKET_MATCH_WINDOW_MS = 250L;

    private final ArrayDeque<PendingHit> pending = new ArrayDeque<>(MAX_QUEUE);
    private final HashMap<Integer, Long> lastPacketConfirmMs = new HashMap<Integer, Long>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) {
            pending.clear();
            lastPacketConfirmMs.clear();
        }

        if (pending.isEmpty()) {
            tick(); // Still run the basic combo tick logic
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
            Entity e = mc.theWorld.getEntityByID(entityId);
            if (!(e instanceof EntityLivingBase)) continue;

            EntityLivingBase t = (EntityLivingBase) e;

            PendingHit newest = pickNewest(entityId, now);
            if (newest == null) continue;

            float hp = t.getHealth();

            // Packet grace
            Long lastPkt = lastPacketConfirmMs.get(entityId);
            if (lastPkt != null && now - lastPkt < PACKET_GRACE_MS) {
                continue;
            }

            // Wait for packet
            if (now - newest.attackTimeMs < PACKET_WAIT_MS) {
                continue;
            }

            // 2) Hurt/Resist fallback (check BEFORE health confirm)
            PendingHit fb = pickBestFallbackCandidate(entityId, now, t);
            if (fb != null) {
                confirmHit(fb.target);
                HitSpanDebug.chat("CONFIRM hurt/resist id=" + entityId +
                        " ht=" + t.hurtTime + "/" + fb.prevHurtTime +
                        " rt=" + t.hurtResistantTime + "/" + fb.prevResistTime);
                purgeEntity(entityId);
                continue;
            }

            // 1) Health confirm (run after hurt/resist fallback)
            if (Float.isNaN(newest.baselineHealth)) {
                newest.baselineHealth = hp;
            } else {
                if (hp + 0.001f < newest.baselineHealth) {
                    confirmHit(newest.target);
                    HitSpanDebug.chat("CONFIRM health id=" + entityId +
                            " hp " + newest.baselineHealth + "->" + hp);
                    purgeEntity(entityId);
                    continue;
                }
            }
        }

        tick(); // Run basic combo tick logic
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null) return;
        if (event == null || event.target == null) return;

        if (event.entityPlayer == null) return;
        if (!event.entityPlayer.worldObj.isRemote) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return;

        if (!(event.target instanceof EntityLivingBase)) return;
        if (cfg.comboPlayersOnly && !(event.target instanceof EntityPlayer)) return;

        EntityLivingBase target = (EntityLivingBase) event.target;

        // Queue the hit for confirmation (don't confirm immediately)
        if (pending.size() >= MAX_QUEUE) pending.pollFirst();

        // Snapshot target bounds at click time
        float border = target.getCollisionBorderSize();
        AxisAlignedBB bb = target.getEntityBoundingBox();
        AxisAlignedBB bbSnap = new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ)
                .expand(border, border, border);

        PendingHit p = new PendingHit(
                target.getEntityId(),
                target,
                target.hurtTime,
                target.hurtResistantTime,
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

        Entity e = mc.theWorld.getEntityByID(entityId);
        if (!(e instanceof EntityLivingBase)) {
            purgeEntity(entityId);
            return;
        }

        confirmHit(best.target);
        lastPacketConfirmMs.put(entityId, now);
        purgeEntity(entityId);
    }

    public void onPlayerHurt() {
        // Reset combo completely when player gets hit
        combo = 0;
        lastHitTimeMs = 0;
        lastTargetId = -1;
        
        // Clear pending hits since the player was interrupted
        pending.clear();
        lastPacketConfirmMs.clear();
    }

    public static void hit(Entity target) {
        if (target == null) return;

        long now = System.currentTimeMillis();
        int targetId = target.getEntityId();

        // Reset combo if new target
        if (lastTargetId != targetId) {
            combo = 0;
            lastTargetId = targetId;
        }

        // Increment combo
        combo++;
        lastHitTimeMs = now;
    }

    private void confirmHit(Entity target) {
        hit(target);
    }

    public static void tick() {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null) return;
        
        // Keep only the configured inactivity reset behavior (world-change resets removed)
        if (cfg.comboResetTimeSeconds <= 0) return; // Don't reset if disabled
        
        long now = System.currentTimeMillis();
        long resetTimeMs = cfg.comboResetTimeSeconds * 1000L;

        // Reset combo after configured time of inactivity
        if (lastHitTimeMs > 0 && now - lastHitTimeMs > resetTimeMs) {
            combo = 0;
            lastHitTimeMs = 0;
            lastTargetId = -1;
        }
    }

    // -------------------------
    // Selection helpers
    // -------------------------

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

    private PendingHit pickBestFallbackCandidate(int entityId, long now, EntityLivingBase t) {
        PendingHit best = null;
        long bestDelta = Long.MAX_VALUE;

        for (PendingHit p : pending) {
            if (p.entityId != entityId) continue;

            long age = now - p.attackTimeMs;
            if (age > CONFIRM_WINDOW_MS) continue;
            if (age < PACKET_WAIT_MS) continue;
            if (age > FALLBACK_MATCH_WINDOW_MS) continue;

            boolean hurtReset =
                    (t.hurtTime > 0) &&
                            (t.hurtTime > p.prevHurtTime) &&
                            (p.prevHurtTime <= HURT_ELIGIBLE_MAX);

            boolean resistReset =
                    (t.hurtResistantTime > 0) &&
                            (t.hurtResistantTime > p.prevResistTime) &&
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

    // -------------------------
    // Helpers
    // -------------------------

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
        final long attackTimeMs;

        float baselineHealth = Float.NaN;

        PendingHit(int entityId,
                   Entity target,
                   int prevHurtTime,
                   int prevResistTime,
                   long attackTimeMs) {
            this.entityId = entityId;
            this.target = target;
            this.prevHurtTime = prevHurtTime;
            this.prevResistTime = prevResistTime;
            this.attackTimeMs = attackTimeMs;
        }
    }
}
