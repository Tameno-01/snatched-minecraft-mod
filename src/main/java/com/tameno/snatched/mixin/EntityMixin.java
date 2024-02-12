package com.tameno.snatched.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tameno.snatched.Snatched;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public class EntityMixin {

    public EntityMixin() {
        Snatched.LOGGER.info("Mixin class initialized");
    }
    @Redirect(
            method = "pushAwayFrom",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isConnectedThroughVehicle(Lnet/minecraft/entity/Entity;)Z"
            )
    )
    private boolean isConnectedThroughVehicleOrIsSnatched(Entity instance, Entity entity) {
        if (entity.getRootVehicle() instanceof HandSeatEntity) {
            return true;
        }
        return instance.isConnectedThroughVehicle(entity);
    }
}
