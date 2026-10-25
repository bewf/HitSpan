package me.bewf.hitspan.debug;

import me.bewf.hitspan.util.McCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ServerRangeProbe {

    private ServerRangeProbe() {}

    private static final long KEEP_MS = 3000L;
    private static final int MAX_PER_ENTITY = 60;
    private static final double MAX_RAY = 6.0D;

    private static final Map<Integer, Deque<Entry>> byEntity = new HashMap<>();

    public static synchronized void recordAttack(Player attacker, Entity target) {
        if (attacker == null || target == null) return;

        double r = computeEntityRayDistance(attacker, target, 1.0F, MAX_RAY);
        if (r < 0) return;
        if (r > MAX_RAY) r = MAX_RAY;

        int id = target.getId();
        long now = System.currentTimeMillis();

        Deque<Entry> q = byEntity.computeIfAbsent(id, k -> new ArrayDeque<>(MAX_PER_ENTITY));
        q.addLast(new Entry(
                now,
                r,
                attacker.getYRot(),
                attacker.getXRot(),
                attacker.getX(),
                attacker.getY(),
                attacker.getZ()
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

    private static double computeEntityRayDistance(Entity viewer, Entity target, float pt, double maxDist) {
        Vec3 eyes = viewer.getEyePosition(pt);
        Vec3 look = viewer.getViewVector(pt);

        float border = target.getPickRadius();
        AABB bb = target.getBoundingBox().inflate(border, border, border);

        return McCompat.rayDistance(eyes, look, bb, maxDist);
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
