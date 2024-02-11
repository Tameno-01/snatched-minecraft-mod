package com.tameno.snatched.entity.custom;

import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HandSeatEntity extends Entity {

    private PlayerEntity handOwner;
    private Hand ownerHand;

    public HandSeatEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
    }

    public void setHandOwner(PlayerEntity newHandOwner) {
        this.handOwner = newHandOwner;
        updateHandPosition();
    }

    public void setHandHand(Hand hand) {

    }

    public void updateHandPosition() {
        float ownerSize = this.handOwner.getEyeHeight(this.handOwner.getPose());

        double side = 1.0;

        if (ownerHand == Hand.OFF_HAND) {
            side *= -1.0;
        }

        Vec3d pos = new Vec3d(-ownerSize * side * 0.2, -ownerSize * 0.2, ownerSize * 0.5);

        pos = pos.rotateX(this.handOwner.getPitch() * -0.01745329251f);
        pos = pos.rotateY(this.handOwner.getYaw() * -0.01745329251f);

        pos = pos.add(this.handOwner.getPos());
        pos = pos.add(0,  ownerSize * 0.9, 0);

        this.setPosition(pos);
    }

    @Override
    public void tick() {
        if (this.getWorld().isClient()) {
            return;
        }

        if (this.handOwner == null || handOwner.isRemoved() || this.getFirstPassenger() == null) {
            this.discard();
            return;
        }

        updateHandPosition();
    }

    @Override
    public double getMountedHeightOffset() {
        return 0.0;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    protected boolean couldAcceptPassenger() {
        return true;
    }

    @Override
    public PistonBehavior getPistonBehavior() {
        return PistonBehavior.IGNORE;
    }

    @Override
    public boolean canAvoidTraps() {
        return true;
    }
}
