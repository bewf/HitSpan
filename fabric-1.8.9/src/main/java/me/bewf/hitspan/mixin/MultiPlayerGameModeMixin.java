package me.bewf.hitspan.mixin;

import me.bewf.hitspan.Range.util.RangeTracker;
import me.bewf.hitspan.combo.util.ComboTracker;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void hitspan$onAttackEntity(PlayerEntity attacker, Entity target, CallbackInfo ci) {
        if (RangeTracker.INSTANCE != null) {
            RangeTracker.INSTANCE.onAttack(attacker, target);
        }

        if (ComboTracker.INSTANCE != null) {
            ComboTracker.INSTANCE.onAttack(attacker, target);
        }
    }
}
