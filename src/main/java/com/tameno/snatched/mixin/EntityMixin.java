package com.tameno.snatched.mixin;

import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract Entity getRootVehicle();

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
