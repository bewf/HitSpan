package me.bewf.hitspan.mixin;

import me.bewf.hitspan.Range.util.RangeTracker;
import me.bewf.hitspan.combo.util.ComboTracker;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.debug.HitSpanDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityEventS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = {"handleEntityEvent", "m_76594826"}, at = @At("HEAD"))
    private void hitspan$onEntityStatus(EntityEventS2CPacket packet, CallbackInfo ci) {
        if (packet.getEvent() != 2) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.world == null) return;

        Entity e = packet.getEntity(mc.world);
        if (e == null) return;

        HitSpanConfig cfg = HitSpanConfig.INSTANCE;
        if (cfg != null && cfg.debugEnabled && cfg.debugPackets) {
            HitSpanDebug.chat("S19 opcode=2 ent=" + e.getNetworkId());
        }

        if (mc.player != null && e.getNetworkId() == mc.player.getNetworkId()) {
            HitSpanDebug.chat("S19: local player hurt (id=" + e.getNetworkId() + ")");
            if (ComboTracker.INSTANCE == null) {
                HitSpanDebug.chat("COMBO: ComboTracker.INSTANCE is null - cannot reset combo");
            } else {
                HitSpanDebug.chat("COMBO: resetting combo via mixin");
                ComboTracker.INSTANCE.onPlayerHurt();
            }
            return;
        }

        if (RangeTracker.INSTANCE != null) {
            RangeTracker.INSTANCE.confirmFromHurtPacket(e.getNetworkId());
        }

        if (ComboTracker.INSTANCE != null) {
            ComboTracker.INSTANCE.confirmFromHurtPacket(e.getNetworkId());
        }
    }
}
