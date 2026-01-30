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
import java.util.Iterator;

public class RangeTracker {

    private final Minecraft mc = Minecraft.getMinecraft();

    public static double lastRange = -1;
    public static long lastRangeTimeMs = 0;

    public static RangeTracker INSTANCE;
    public RangeTracker() { INSTANCE = this; }

    private static final int MAX_QUEUE = 60;
    private static final long CONFIRM_WINDOW_MS = 1200L;

    private final ArrayDeque<PendingHit> pending = new ArrayDeque<>(MAX_QUEUE);

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.rangeHudEnabled) {
            pending.clear();
            return;
        }

        long wt = mc.theWorld.getTotalWorldTime();
        long now = System.currentTimeMillis();

        HitSpanDebug.chatVerbose("tick wt=" + wt + " q=" + pending.size());

        if (pending.isEmpty()) return;

        Iterator<PendingHit> it = pending.iterator();
        while (it.hasNext()) {
            PendingHit p = it.next();

            if (now - p.attackTimeMs > CONFIRM_WINDOW_MS) {
                HitSpanDebug.chatVerbose("drop timeout id=" + p.entityId + " age=" + (now - p.attackTimeMs));
                it.remove();
                continue;
            }

            Entity e = mc.theWorld.getEntityByID(p.entityId);
            if (!(e instanceof EntityLivingBase)) continue;

            EntityLivingBase t = (EntityLivingBase) e;

            // Damage confirm (best path when health updates)
            float hp = t.getHealth();
            if (!p.damageChecked) {
                p.damageChecked = true;
            } else {
                if (hp + 0.001f < p.preHealth) {
                    if (cfg.debugEnabled && cfg.debugConfirms) {
                        HitSpanDebug.chat("CONFIRM health id=" + p.entityId + " r=" + fmt(p.range) + " hp " + p.preHealth + "->" + hp);
                    }
                    confirm(p, t);
                    purgeEntity(p.entityId);
                    break;
                }
            }

            // Invincible / no-health-change fallback
            boolean hurtTimeBumped = t.hurtTime > p.prevHurtTime + 1;
            boolean resistBumped = t.hurtResistantTime > p.prevResistTime + 2;

            HitSpanDebug.chatVerbose("check id=" + p.entityId + " ht=" + t.hurtTime + "/" + p.prevHurtTime + " rt=" + t.hurtResistantTime + "/" + p.prevResistTime);

            if (hurtTimeBumped || resistBumped) {
                if (cfg.debugEnabled && cfg.debugConfirms) {
                    HitSpanDebug.chat("CONFIRM hurt/resist id=" + p.entityId +
                            " r=" + fmt(p.range) +
                            " htB=" + hurtTimeBumped +
                            " rtB=" + resistBumped);
                }
                confirm(p, t);
                purgeEntity(p.entityId);
                break;
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

        // Knockback is independent from range confirmation.
        if (cfg.knockbackHudEnabled) {
            if (!cfg.knockbackPlayersOnly || target instanceof EntityPlayer) {
                KnockbackTracker.beginTracking(target);
            }
        }

        double maxReach = mc.thePlayer.capabilities.isCreativeMode ? 4.5D : 3.0D;
        double computed = computeEntityRayDistance(mc.thePlayer, target, 1.0F, maxReach);
        if (computed < 0) return;
        if (computed > maxReach) computed = maxReach;

        if (!cfg.confirmRangeOnHitConfirm) {
            lastRange = computed;
            lastRangeTimeMs = System.currentTimeMillis();

            if (cfg.debugEnabled && cfg.debugAttacks) {
                HitSpanDebug.chat("RANGE immediate id=" + target.getEntityId() + " r=" + fmt(computed));
            }
            return;
        }

        if (pending.size() >= MAX_QUEUE) pending.pollFirst();

        PendingHit p = new PendingHit(
                target.getEntityId(),
                computed,
                target.hurtTime,
                target.hurtResistantTime,
                target.getHealth(),
                System.currentTimeMillis()
        );
        pending.addLast(p);

        if (cfg.debugEnabled && cfg.debugAttacks) {
            HitSpanDebug.chat("ENQUEUE id=" + p.entityId +
                    " r=" + fmt(p.range) +
                    " ht=" + p.prevHurtTime +
                    " rt=" + p.prevResistTime +
                    " hp=" + p.preHealth +
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

        PendingHit best = null;
        for (PendingHit p : pending) {
            if (p.entityId != entityId) continue;
            if (now - p.attackTimeMs > CONFIRM_WINDOW_MS) continue;
            if (best == null || p.attackTimeMs > best.attackTimeMs) best = p;
        }

        if (cfg.debugEnabled && cfg.debugPackets) {
            HitSpanDebug.chat("PACKET id=" + entityId +
                    " q=" + pending.size() +
                    " best=" + (best == null ? "null" : ("r=" + fmt(best.range) + " age=" + (now - best.attackTimeMs))));
        }

        if (best == null) return;

        Entity e = mc.theWorld.getEntityByID(entityId);
        if (!(e instanceof EntityLivingBase)) {
            purgeEntity(entityId);
            return;
        }

        EntityLivingBase t = (EntityLivingBase) e;

        if (cfg.debugEnabled && cfg.debugConfirms) {
            HitSpanDebug.chat("CONFIRM packet id=" + entityId + " r=" + fmt(best.range));
        }

        confirm(best, t);
        purgeEntity(entityId);
    }

    private void confirm(PendingHit p, EntityLivingBase target) {
        lastRange = p.range;
        lastRangeTimeMs = System.currentTimeMillis();
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
        final int prevHurtTime;
        final int prevResistTime;
        final float preHealth;
        final long attackTimeMs;

        boolean damageChecked = false;

        PendingHit(int entityId, double range, int prevHurtTime, int prevResistTime, float preHealth, long attackTimeMs) {
            this.entityId = entityId;
            this.range = range;
            this.prevHurtTime = prevHurtTime;
            this.prevResistTime = prevResistTime;
            this.preHealth = preHealth;
            this.attackTimeMs = attackTimeMs;
        }
    }
}
