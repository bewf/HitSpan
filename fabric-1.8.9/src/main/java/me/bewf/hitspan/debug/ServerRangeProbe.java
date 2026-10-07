package me.bewf.hitspan.debug;

import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.HitResult;
import net.minecraft.util.math.Vec3d;

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

    public static synchronized void recordAttack(PlayerEntity attacker, Entity target) {
        if (attacker == null || target == null) return;

        double maxReach = attacker.abilities.creativeMode ? 4.5D : 3.0D;
        double r = computeEntityRayDistance(attacker, target, 1.0F, maxReach);
        if (r < 0) return;
        if (r > maxReach) r = maxReach;

        int id = target.getNetworkId();
        long now = System.currentTimeMillis();

        Deque<Entry> q = byEntity.computeIfAbsent(id, k -> new ArrayDeque<>(MAX_PER_ENTITY));
        q.addLast(new Entry(
                now,
                r,
                attacker.yaw,
                attacker.pitch,
                attacker.x,
                attacker.y,
                attacker.z
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

    private static double computeEntityRayDistance(LivingEntity player, Entity target, float pt, double maxDist) {
        Vec3d eyes = player.getEyePosition(pt);
        Vec3d look = player.getRotationVector(pt);
        Vec3d end = eyes.add(
                look.x * maxDist,
                look.y * maxDist,
                look.z * maxDist
        );

        float border = target.getPickRadius();
        Box bb = target.getShape().expanded(border, border, border);

        HitResult hit = bb.clip(eyes, end);
        if (hit == null || hit.facePos == null) return -1;

        return hit.facePos.distanceTo(eyes);
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
