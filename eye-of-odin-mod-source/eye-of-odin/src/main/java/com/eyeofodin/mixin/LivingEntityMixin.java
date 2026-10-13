package com.eyeofodin.mixin;

import com.eyeofodin.EyeLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    // Changes the damage amount right when an entity is hurt (this is how the crit is applied)
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float eyeofodin$crit(float amount, ServerLevel level, DamageSource source) {
        return EyeLogic.modifyOutgoingDamage((LivingEntity) (Object) this, level, source, amount);
    }
}
