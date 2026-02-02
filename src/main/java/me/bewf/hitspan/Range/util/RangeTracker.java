// src/main/java/me/bewf/hitspan/Range/util/RangeTracker.java
package me.bewf.hitspan.Range.util;

import me.bewf.hitspan.Knockback.util.KnockbackTracker;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.HitSpanDebug;
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
        if (mc.theWorld == null) return;

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
            Entity e = mc.theWorld.getEntityByID(entityId);
            if (!(e instanceof EntityLivingBase)) continue;

            EntityLivingBase t = (EntityLivingBase) e;

            PendingHit newest = pickNewest(entityId, now);
            if (newest == null) continue;

            float hp = t.getHealth();

            // 1) Health confirm
            if (Float.isNaN(newest.baselineHealth)) {
                newest.baselineHealth = hp;
            } else {
                if (hp + 0.001f < newest.baselineHealth) {
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

            // Packet grace
            Long lastPkt = lastPacketConfirmMs.get(entityId);
            if (lastPkt != null && now - lastPkt < PACKET_GRACE_MS) {
                if (cfg.debugEnabled && cfg.debugVerbose) {
                    HitSpanDebug.chatVerbose("skip fallback (packet grace) id=" + entityId +
                            " age=" + (now - lastPkt));
                }
                continue;
            }

            // Wait for packet
            if (now - newest.attackTimeMs < PACKET_WAIT_MS) {
                if (cfg.debugEnabled && cfg.debugVerbose) {
                    HitSpanDebug.chatVerbose("wait packet id=" + entityId +
                            " bestAge=" + (now - newest.attackTimeMs));
                }
                continue;
            }

            // 2) Hurt/Resist fallback
            PendingHit fb = pickBestFallbackCandidate(entityId, now, t);
            if (fb != null) {
                double confirmedRange = recomputeFromSnapshot(fb);

                if (cfg.debugEnabled && cfg.debugConfirms) {
                    HitSpanDebug.chat("CONFIRM hurt/resist id=" + entityId +
                            " r=" + fmt(confirmedRange) +
                            " ht=" + t.hurtTime + "/" + fb.prevHurtTime +
                            " rt=" + t.hurtResistantTime + "/" + fb.prevResistTime);
                }

                confirmValue(confirmedRange);
                HitSpanDebug.rangeServerCompare(entityId, confirmedRange, "hurt/resist", fb.attackTimeMs);
                purgeEntity(entityId);
                continue;
            }

            if (cfg.debugEnabled && cfg.debugVerbose) {
                HitSpanDebug.chatVerbose("check id=" + entityId +
                        " bestAge=" + (now - newest.attackTimeMs) +
                        " ht=" + t.hurtTime + "/" + newest.prevHurtTime +
                        " rt=" + t.hurtResistantTime + "/" + newest.prevResistTime +
                        " hp=" + hp + "/" + newest.baselineHealth);
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
        if (cfg == null) return;
        if (!cfg.rangeHudEnabled) return;

        if (!(event.target instanceof EntityLivingBase)) return;
        if (cfg.rangePlayersOnly && !(event.target instanceof EntityPlayer)) return;

        EntityLivingBase target = (EntityLivingBase) event.target;

        if (cfg.knockbackHudEnabled) {
            if (!cfg.knockbackPlayersOnly || target instanceof EntityPlayer) {
                KnockbackTracker.beginTracking(target);
            }
        }

        double maxReach = mc.thePlayer.capabilities.isCreativeMode ? 4.5D : 3.0D;

        // Snapshot view at click time
        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;

        // Reliable enqueue measurement (current client state)
        double computed = computeEntityRayDistance(mc.thePlayer, target, 1.0F, maxReach);
        if (computed < 0) return;
        if (computed > maxReach) computed = maxReach;

        if (!cfg.confirmRangeOnHitConfirm) {
            confirmValue(computed);

            if (cfg.debugEnabled && cfg.debugAttacks) {
                HitSpanDebug.chat("RANGE immediate id=" + target.getEntityId() + " r=" + fmt(computed));
            }
            return;
        }

        if (pending.size() >= MAX_QUEUE) pending.pollFirst();

        // Snapshot target bounds at click time (THIS is the important fix)
        float border = target.getCollisionBorderSize();
        AxisAlignedBB bb = target.getEntityBoundingBox();
        AxisAlignedBB bbSnap = new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ)
                .expand(border, border, border);

        PendingHit p = new PendingHit(
                target.getEntityId(),
                computed,
                yaw,
                pitch,
                maxReach,
                bbSnap,
                target.hurtTime,
                target.hurtResistantTime,
                System.currentTimeMillis()
        );
        pending.addLast(p);

        if (cfg.debugEnabled && cfg.debugAttacks) {
            HitSpanDebug.chat("ENQUEUE id=" + p.entityId +
                    " r=" + fmt(p.range) +
                    " ht=" + p.prevHurtTime +
                    " rt=" + p.prevResistTime +
                    " q=" + pending.size());
        }
    }

    public void confirmFromHurtPacket(int entityId) {
        if (mc.theWorld == null) return;
        if (pending.isEmpty()) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.rangeHudEnabled) return;
        if (!cfg.confirmRangeOnHitConfirm) return;

        long now = System.currentTimeMillis();

        purgeStale(now);
        purgeOldPacketMarks(now);

        PendingHit best = pickBestForPacket(entityId, now);

        if (cfg.debugEnabled && cfg.debugPackets) {
            HitSpanDebug.chat("PACKET id=" + entityId +
                    " q=" + pending.size() +
                    " best=" + (best == null ? "null" : ("r=" + fmt(best.range) + " age=" + (now - best.attackTimeMs))));
            dumpPacketCandidates(entityId, now, best);
        }

        if (best == null) return;

        Entity e = mc.theWorld.getEntityByID(entityId);
        if (!(e instanceof EntityLivingBase)) {
            purgeEntity(entityId);
            return;
        }

        double confirmedRange = recomputeFromSnapshot(best);

        if (cfg.debugEnabled && cfg.debugConfirms) {
            HitSpanDebug.chat("CONFIRM packet id=" + entityId + " r=" + fmt(confirmedRange));
        }

        confirmValue(confirmedRange);
        lastPacketConfirmMs.put(entityId, now);

        HitSpanDebug.rangeServerCompare(entityId, confirmedRange, "packet", best.attackTimeMs);

        purgeEntity(entityId);
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
    // Debug helpers
    // -------------------------

    private void dumpPacketCandidates(int entityId, long now, PendingHit chosen) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.debugEnabled || !cfg.debugPackets) return;

        PendingHit[] buf = new PendingHit[12];
        int n = 0;

        for (PendingHit p : pending) {
            if (p.entityId != entityId) continue;
            if (n < buf.length) buf[n++] = p;
            else {
                for (int i = 1; i < buf.length; i++) buf[i - 1] = buf[i];
                buf[buf.length - 1] = p;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("PKT candidates id=").append(entityId).append(": ");

        int shown = 0;
        for (int i = n - 1; i >= 0; i--) {
            PendingHit p = buf[i];
            if (p == null) continue;

            long age = now - p.attackTimeMs;
            boolean eligible = (p.prevResistTime <= RESIST_ELIGIBLE_MAX) || (p.prevHurtTime <= HURT_ELIGIBLE_MAX);
            boolean isChosen = (chosen != null && p == chosen);

            if (shown > 0) sb.append(" | ");
            if (isChosen) sb.append("*");

            sb.append("r=").append(fmt(p.range))
                    .append(" age=").append(age)
                    .append(" ht=").append(p.prevHurtTime)
                    .append(" rt=").append(p.prevResistTime)
                    .append(" ok=").append(eligible);

            shown++;
            if (shown >= 8) break;
        }

        HitSpanDebug.chat(sb.toString());
    }

    // -------------------------
    // Confirm-time recompute
    // -------------------------

    private double recomputeFromSnapshot(PendingHit p) {
        if (mc.thePlayer == null) return p.range;

        double r = computeRayDistanceAgainstBox(mc.thePlayer, p.bbSnap, p.yaw, p.pitch, 1.0F, p.maxReach);
        if (r < 0) return p.range;
        if (r > p.maxReach) r = p.maxReach;
        if (r < 0) r = 0;
        return r;
    }

    private static double computeRayDistanceAgainstBox(EntityLivingBase player,
                                                       AxisAlignedBB bb,
                                                       float yaw,
                                                       float pitch,
                                                       float partialTicks,
                                                       double maxDist) {
        Vec3 eyes = player.getPositionEyes(partialTicks);

        float yawRad = (float) Math.toRadians(-yaw) - (float) Math.PI;
        float pitchRad = (float) Math.toRadians(-pitch);

        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);

        Vec3 look = new Vec3(
                (double) (sinYaw * cosPitch),
                (double) (sinPitch),
                (double) (cosYaw * cosPitch)
        );

        Vec3 end = eyes.addVector(look.xCoord * maxDist, look.yCoord * maxDist, look.zCoord * maxDist);

        MovingObjectPosition hit = bb.calculateIntercept(eyes, end);
        if (hit == null || hit.hitVec == null) return -1;

        return hit.hitVec.distanceTo(eyes);
    }

    // -------------------------
    // Helpers
    // -------------------------

    private void confirmValue(double range) {
        lastRange = range;
        lastRangeTimeMs = System.currentTimeMillis();
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

        PendingHit(int entityId,
                   double range,
                   float yaw,
                   float pitch,
                   double maxReach,
                   AxisAlignedBB bbSnap,
                   int prevHurtTime,
                   int prevResistTime,
                   long attackTimeMs) {
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
