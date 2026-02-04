package me.bewf.hitspan.mixin;

import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.ServerRangeProbe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketUseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public class NetHandlerPlayServerMixin {

    @Shadow public EntityPlayerMP player;

    @Inject(method = "processUseEntity", at = @At("HEAD"))
    private void hitspan$probeServerAttack(CPacketUseEntity packet, CallbackInfo ci) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.debugEnabled || !cfg.debugServerCompare) return;

        if (player == null || player.getServer() == null || !player.getServer().isSinglePlayer()) return;
        if (!player.getServer().isCallingFromMinecraftThread()) return;

        if (packet.getAction() != CPacketUseEntity.Action.ATTACK) return;

        Entity target = packet.getEntityFromWorld(player.world);
        if (target == null) return;

        ServerRangeProbe.recordAttack(player, target);
    }
}
