// src/main/java/me/bewf/hitspan/mixin/NetHandlerPlayClientMixin.java
package me.bewf.hitspan.mixin;

import me.bewf.hitspan.Range.util.RangeTracker;
import me.bewf.hitspan.combo.util.ComboTracker;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.HitSpanDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class NetHandlerPlayClientMixin {

    @Inject(method = "handleEntityStatus", at = @At("HEAD"))
    private void hitspan$onEntityStatus(S19PacketEntityStatus packet, CallbackInfo ci) {
        if (packet.getOpCode() != 2) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        Entity e = packet.getEntity(mc.theWorld);
        if (e == null) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg != null && cfg.debugEnabled && cfg.debugPackets) {
            HitSpanDebug.chat("S19 opcode=2 ent=" + e.getEntityId());
        }

        // Check if the hurt entity is the local player - reset combo if so
        if (mc.thePlayer != null && e.getEntityId() == mc.thePlayer.getEntityId()) {
            // Debug: confirm we saw a local-player hurt packet
            HitSpanDebug.chat("S19: local player hurt (id=" + e.getEntityId() + ")");
            if (ComboTracker.INSTANCE == null) {
                HitSpanDebug.chat("COMBO: ComboTracker.INSTANCE is null - cannot reset combo");
            } else {
                HitSpanDebug.chat("COMBO: resetting combo via mixin");
                ComboTracker.INSTANCE.onPlayerHurt();
            }
            return; // Don't process as a successful hit
        }

        if (RangeTracker.INSTANCE != null) {
            RangeTracker.INSTANCE.confirmFromHurtPacket(e.getEntityId());
        }
        
        if (ComboTracker.INSTANCE != null) {
            ComboTracker.INSTANCE.confirmFromHurtPacket(e.getEntityId());
        }
    }
}
