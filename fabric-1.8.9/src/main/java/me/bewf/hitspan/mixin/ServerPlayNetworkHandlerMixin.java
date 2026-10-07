package me.bewf.hitspan.mixin;

import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.ServerRangeProbe;
import net.minecraft.entity.Entity;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import net.minecraft.server.network.handler.ServerPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow public ServerPlayerEntity player;

    @Inject(method = {"handleInteractEntity", "m_04040712"}, at = @At("HEAD"))
    private void hitspan$probeServerAttack(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.debugEnabled || !cfg.debugServerCompare) return;

        if (player == null || player.server == null || !player.server.isSingleplayer()) return;

        if (Thread.currentThread() != player.server.getThread()) return;

        if (packet == null || packet.getAction() != PlayerInteractEntityC2SPacket.Action.ATTACK) return;

        Entity target = packet.getInteractTarget(player.world);
        if (target == null) return;

        ServerRangeProbe.recordAttack(player, target);
    }
}
