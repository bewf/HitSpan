package me.bewf.hitspan.mixin;

import me.bewf.hitspan.RangeTracker;
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
        // 2 = hurt animation opcode in 1.8.9
        if (packet.getOpCode() != 2) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        Entity e = packet.getEntity(mc.theWorld);
        if (e == null) return;

        if (RangeTracker.INSTANCE != null) {
            RangeTracker.INSTANCE.confirmFromHurtPacket(e.getEntityId());
        }
    }
}
