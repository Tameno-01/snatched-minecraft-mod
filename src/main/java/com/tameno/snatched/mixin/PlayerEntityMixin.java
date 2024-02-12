package com.tameno.snatched.mixin;

import com.tameno.snatched.Snatched;
import com.tameno.snatched.Snatcher;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements Snatcher {

    @Shadow protected abstract void takeShieldHit(LivingEntity attacker);

    private static final String HAND_SEAT_KEY = "snatched_hand_seat";

    private HandSeatEntity snatched$currentHandSeat = null;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    public void snatched$setCurrentHandSeat(HandSeatEntity newHandSeat) {
        this.snatched$currentHandSeat = newHandSeat;
    }

    public HandSeatEntity snatched$getCurrentHandSeat() {
        return this.snatched$currentHandSeat;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readHandSeat(NbtCompound nbt, CallbackInfo callbackInfo) {
        this.snatched$currentHandSeat = (HandSeatEntity) this.getWorld().getEntityById(nbt.getInt(HAND_SEAT_KEY));
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeHandSeat(NbtCompound nbt, CallbackInfo callbackInfo) {
        nbt.putInt(HAND_SEAT_KEY, this.snatched$currentHandSeat.getId());
    }
}
