// src/main/java/me/bewf/hitspan/debug/ServerRangeProbe.java
package me.bewf.hitspan.debug;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ServerRangeProbe {

    private ServerRangeProbe() {}

    private static final long KEEP_MS = 3000L;
    private static final int MAX_PER_ENTITY = 60;

    private static final Map<Integer, Deque<Entry>> byEntity = new HashMap<>();

    public static synchronized void recordAttack(EntityPlayer attacker, Entity target) {
        if (attacker == null || target == null) return;

        double maxReach = attacker.capabilities.isCreativeMode ? 4.5D : 3.0D;
        double r = computeEntityRayDistance(attacker, target, 1.0F, maxReach);
        if (r < 0) return;
        if (r > maxReach) r = maxReach;

        int id = target.getEntityId();
        long now = System.currentTimeMillis();

        Deque<Entry> q = byEntity.computeIfAbsent(id, k -> new ArrayDeque<>(MAX_PER_ENTITY));
        q.addLast(new Entry(
                now,
                r,
                attacker.rotationYaw,
                attacker.rotationPitch,
                attacker.posX,
                attacker.posY,
                attacker.posZ
        ));

        while (q.size() > MAX_PER_ENTITY) q.pollFirst();
        purgeOld(now);
    }

    public static synchronized Match getClosestMatch(int entityId, long targetTimeMs, long windowMs) {
        purgeOld(System.currentTimeMillis());

        Deque<Entry> q = byEntity.get(entityId);
        if (q == null || q.isEmpty()) return null;

        Entry best = null;
        long bestDelta = Long.MAX_VALUE;

        for (Entry e : q) {
            long d = Math.abs(e.timeMs - targetTimeMs);
            if (d > windowMs) continue;
            if (best == null || d < bestDelta) {
                best = e;
                bestDelta = d;
            }
        }

        return best == null ? null :
                new Match(best.timeMs, best.range, best.yaw, best.pitch, best.ax, best.ay, best.az);
    }

    private static synchronized void purgeOld(long nowMs) {
        Iterator<Map.Entry<Integer, Deque<Entry>>> it = byEntity.entrySet().iterator();
        while (it.hasNext()) {
            Deque<Entry> q = it.next().getValue();
            while (!q.isEmpty() && nowMs - q.peekFirst().timeMs > KEEP_MS) q.pollFirst();
            if (q.isEmpty()) it.remove();
        }
    }

    private static double computeEntityRayDistance(EntityLivingBase player, Entity target, float pt, double maxDist) {
        Vec3 eyes = player.getPositionEyes(pt);
        Vec3 look = player.getLook(pt);
        Vec3 end = eyes.addVector(
                look.xCoord * maxDist,
                look.yCoord * maxDist,
                look.zCoord * maxDist
        );

        float border = target.getCollisionBorderSize();
        AxisAlignedBB bb = target.getEntityBoundingBox().expand(border, border, border);

        MovingObjectPosition hit = bb.calculateIntercept(eyes, end);
        if (hit == null || hit.hitVec == null) return -1;

        return hit.hitVec.distanceTo(eyes);
    }

    private static final class Entry {
        final long timeMs;
        final double range;
        final float yaw;
        final float pitch;
        final double ax, ay, az;

        Entry(long t, double r, float y, float p, double ax, double ay, double az) {
            this.timeMs = t;
            this.range = r;
            this.yaw = y;
            this.pitch = p;
            this.ax = ax;
            this.ay = ay;
            this.az = az;
        }
    }

    public static final class Match {
        public final long timeMs;
        public final double range;
        public final float yaw;
        public final float pitch;
        public final double ax, ay, az;

        Match(long t, double r, float y, float p, double ax, double ay, double az) {
            this.timeMs = t;
            this.range = r;
            this.yaw = y;
            this.pitch = p;
            this.ax = ax;
            this.ay = ay;
            this.az = az;
        }
    }
}
