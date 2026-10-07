package me.bewf.hitspan.mixin;

import me.bewf.hitspan.combo.util.ComboTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = {"takeDamage", "m_40469716"}, at = @At("HEAD"))
    private void hitspan$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if ((Object) this != mc.player) return;

        if (source != null && source.getAttacker() instanceof PlayerEntity) {
            if (ComboTracker.INSTANCE != null) {
                ComboTracker.INSTANCE.onPlayerHurtByPlayer();
            }
        }
    }
}
