package com.tameno.snatched.mixin;

import com.tameno.snatched.Snatched;
import com.tameno.snatched.Snatcher;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements Snatcher {

    @Shadow protected abstract void takeShieldHit(LivingEntity attacker);

    @Shadow public ScreenHandler currentScreenHandler;

    @Shadow public abstract void resetLastAttackedTicks();

    private static final String HAND_SEAT_KEY = "snatched_hand_seat";

    private UUID snatched$currentHandSeatUuid;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    public void snatched$setCurrentHandSeat(HandSeatEntity newHandSeat) {
        if (newHandSeat == null) {
            this.snatched$currentHandSeatUuid = null;
            return;
        }
        this.snatched$currentHandSeatUuid = newHandSeat.getUuid();
    }

    public HandSeatEntity snatched$getCurrentHandSeat(World world) {

        if (this.snatched$currentHandSeatUuid == null) {
            return null;
        }
        return (HandSeatEntity) ((ServerWorld) world).getEntity(this.snatched$currentHandSeatUuid);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readHandSeat(NbtCompound nbt, CallbackInfo callbackInfo) {
        if (nbt.contains(HAND_SEAT_KEY)) {
            this.snatched$currentHandSeatUuid = nbt.getUuid(HAND_SEAT_KEY);
        } else {
            this.snatched$currentHandSeatUuid = null;
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeHandSeat(NbtCompound nbt, CallbackInfo callbackInfo) {
        if (this.snatched$currentHandSeatUuid != null) {
            nbt.putUuid(HAND_SEAT_KEY, this.snatched$currentHandSeatUuid);
        }
    }
}
