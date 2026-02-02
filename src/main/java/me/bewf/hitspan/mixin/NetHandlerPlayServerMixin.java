// src/main/java/me/bewf/hitspan/mixin/NetHandlerPlayServerMixin.java
package me.bewf.hitspan.mixin;

import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.ServerRangeProbe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public class NetHandlerPlayServerMixin {

    @Shadow public EntityPlayerMP playerEntity;

    @Inject(method = "processUseEntity", at = @At("HEAD"))
    private void hitspan$probeServerAttack(C02PacketUseEntity packet, CallbackInfo ci) {
        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg == null || !cfg.debugEnabled || !cfg.debugServerCompare) return;

        if (playerEntity == null || playerEntity.mcServer == null || !playerEntity.mcServer.isSinglePlayer()) return;

        // Critical: avoid recording on the Netty thread before the packet is enqueued to the server thread
        if (!playerEntity.mcServer.isCallingFromMinecraftThread()) return;

        if (packet == null || packet.getAction() != C02PacketUseEntity.Action.ATTACK) return;

        Entity target = packet.getEntityFromWorld(playerEntity.worldObj);
        if (target == null) return;

        ServerRangeProbe.recordAttack(playerEntity, target);
    }
}
