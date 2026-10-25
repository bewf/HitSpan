package me.bewf.hitspan.mixin;

import me.bewf.hitspan.HitSpanHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void hitspan$onAttack(Entity target, CallbackInfo ci) {
        HitSpanHooks.onAttack((Player) (Object) this, target);
    }
}
