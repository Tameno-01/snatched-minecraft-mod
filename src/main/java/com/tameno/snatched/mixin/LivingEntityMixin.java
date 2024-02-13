package com.tameno.snatched.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyReturnValue(
            method = "canHit",
            at = @At("RETURN")
    )
    public boolean canHitAndIsntSnatched(boolean original) {
        return original && !(this.getRootVehicle() instanceof HandSeatEntity);
    }
}
