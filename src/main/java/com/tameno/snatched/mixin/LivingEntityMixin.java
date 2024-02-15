package com.tameno.snatched.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow protected abstract void drop(DamageSource source);

    @Shadow protected abstract void takeShieldHit(LivingEntity attacker);

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyReturnValue(
            method = "canHit",
            at = @At("RETURN")
    )
    private boolean canHitAndIsntSnatched(boolean original) {
        return original && !(this.getRootVehicle() instanceof HandSeatEntity);
    }
}
