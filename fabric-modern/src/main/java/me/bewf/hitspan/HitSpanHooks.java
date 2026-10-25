package me.bewf.hitspan;

import me.bewf.hitspan.Range.util.RangeTracker;
import me.bewf.hitspan.combo.util.ComboTracker;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.HitSpanDebug;
import me.bewf.hitspan.debug.ServerRangeProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class HitSpanHooks {

    private HitSpanHooks() {}

    public static void onAttack(Player attacker, Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || attacker == null || target == null) return;

        if (attacker == mc.player) {
            if (RangeTracker.INSTANCE != null) {
                RangeTracker.INSTANCE.onAttack(attacker, target);
            }
            if (ComboTracker.INSTANCE != null) {
                ComboTracker.INSTANCE.onAttack(attacker, target);
            }
            return;
        }

        if (attacker instanceof ServerPlayer) {
            HitSpanConfig cfg = HitSpanConfig.INSTANCE;
            if (cfg == null || !cfg.debugEnabled || !cfg.debugServerCompare) return;
            if (mc.getSingleplayerServer() == null) return;
            ServerRangeProbe.recordAttack(attacker, target);
        }
    }

    private static final int NO_SOURCE = -1;
    private static final long DAMAGE_EVENTS_ACTIVE_MS = 10000L;

    private static long lastDamageEventMs = 0L;

    public static void onDamageEvent(int entityId, int causeId) {
        lastDamageEventMs = System.currentTimeMillis();
        handleHurt(entityId, causeId);
    }

    public static void onHurtAnimation(int entityId) {
        if (System.currentTimeMillis() - lastDamageEventMs < DAMAGE_EVENTS_ACTIVE_MS) return;
        handleHurt(entityId, NO_SOURCE);
    }

    private static void handleHurt(int entityId, int causeId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;

        Entity e = mc.level.getEntity(entityId);
        if (e == null) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg != null && cfg.debugEnabled && cfg.debugPackets) {
            HitSpanDebug.chat("Hurt packet ent=" + entityId + " cause=" + causeId);
        }

        if (e.getId() == mc.player.getId()) {
            if (ComboTracker.INSTANCE != null) {
                ComboTracker.INSTANCE.onPlayerHurt();
            }
            return;
        }

        boolean foreign = causeId != NO_SOURCE && causeId != mc.player.getId();
        if (foreign) {
            if (RangeTracker.INSTANCE != null) {
                RangeTracker.INSTANCE.rejectFromForeignDamage(entityId);
            }
            if (ComboTracker.INSTANCE != null) {
                ComboTracker.INSTANCE.rejectFromForeignDamage(entityId);
            }
            return;
        }

        if (RangeTracker.INSTANCE != null) {
            RangeTracker.INSTANCE.confirmFromHurtPacket(entityId);
        }

        if (ComboTracker.INSTANCE != null) {
            ComboTracker.INSTANCE.confirmFromHurtPacket(entityId);
        }
    }
}
