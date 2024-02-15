package com.tameno.snatched.mixin;

import com.tameno.snatched.Snatcher;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements Snatcher {

    @Shadow public abstract void startFallFlying();

    @Shadow protected abstract void takeShieldHit(LivingEntity attacker);

    @Shadow public abstract boolean giveItemStack(ItemStack stack);

    @Shadow public abstract PlayerInventory getInventory();

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

    @Inject(method = "tick", at = @At("HEAD"))
    private void cancelOffhandSlotIfNecessary(CallbackInfo callbackInfo) {
        HandSeatEntity handSeat = this.snatched$getCurrentHandSeat(this.getWorld());
        if (handSeat == null) {
            return;
        }
        if (handSeat.isRemoved()) {
            return;
        }
        ItemStack stack = this.getStackInHand(Hand.OFF_HAND);
        if (stack.isEmpty()) {
            return;
        }
        this.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        if (!this.getWorld().isClient()) {
            if (this.getInventory().getEmptySlot() == -1) {
                this.dropStack(stack);
            } else {
                this.giveItemStack(stack);
            }
        }
    }
}
