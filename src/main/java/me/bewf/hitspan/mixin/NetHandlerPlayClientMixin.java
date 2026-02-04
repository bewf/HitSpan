package me.bewf.hitspan.mixin;

import me.bewf.hitspan.Range.util.RangeTracker;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.HitSpanDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.SPacketEntityStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class NetHandlerPlayClientMixin {

    @Inject(method = "handleEntityStatus", at = @At("HEAD"))
    private void hitspan$onEntityStatus(SPacketEntityStatus packet, CallbackInfo ci) {
        if (packet.getOpCode() != 2) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;

        Entity e = packet.getEntity(mc.world);
        if (e == null) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg != null && cfg.debugEnabled && cfg.debugPackets) {
            HitSpanDebug.chat("SPacket opcode=2 ent=" + e.getEntityId());
        }

        if (RangeTracker.INSTANCE != null) {
            RangeTracker.INSTANCE.confirmFromHurtPacket(e.getEntityId());
        }
    }
}
