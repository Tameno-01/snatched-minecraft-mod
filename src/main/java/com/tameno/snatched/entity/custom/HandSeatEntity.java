package com.tameno.snatched.entity.custom;

import com.tameno.snatched.Snatched;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public class HandSeatEntity extends Entity {

    private static final TrackedData<Optional<UUID>> HAND_OWNER_ID = DataTracker.registerData(HandSeatEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    private PlayerEntity handOwner;

    public HandSeatEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
        Optional<java.util.UUID> handOwnerId = this.dataTracker.get(HAND_OWNER_ID);
        if (handOwnerId.isPresent()) {
            this.handOwner = this.getWorld().getPlayerByUuid(handOwnerId.get());
        }
    }

    public void setHandOwner(PlayerEntity newHandOwner) {
        this.handOwner = newHandOwner;
        this.dataTracker.set(HAND_OWNER_ID, Optional.of(newHandOwner.getUuid()));
        updateHandPosition();
    }

    public void updateHandPosition() {
        double ownerSize = Snatched.getSize(this.handOwner);

        double side = -1.0;
        if (this.handOwner.getMainArm() == Arm.LEFT) {
            side *= -1.0;
        }

        Vec3d pos = new Vec3d(-ownerSize * side * 0.3, -ownerSize * 0.3, ownerSize * 0.5);

        pos = pos.rotateX(this.handOwner.getPitch() * -0.01745329251f);
        pos = pos.rotateY(this.handOwner.getYaw() * -0.01745329251f);

        pos = pos.add(this.handOwner.getPos());
        pos = pos.add(0,  this.handOwner.getEyeHeight(this.handOwner.getPose()), 0);

        this.setPosition(pos);
    }

    @Override
    public void tick() {

        if (handOwner == null) {
            Optional<java.util.UUID> handOwnerId = this.dataTracker.get(HAND_OWNER_ID);
            if (handOwnerId.isPresent()) {
                this.handOwner = this.getWorld().getPlayerByUuid(handOwnerId.get());
            }
        }

        boolean isValid = !(this.handOwner == null || handOwner.isRemoved() || this.getFirstPassenger() == null);

        if (this.getWorld().isClient()) {
            if (!isValid) {
                return;
            }
            updateHandPosition();
        }

        if (!isValid) {
            this.discard();
            return;
        }

        updateHandPosition();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
    }

    @Override
    public double getMountedHeightOffset() {
        return 0.0;
    }

    @Override
    protected void initDataTracker() {
        Optional<java.util.UUID> handOwnerId;
        if (this.handOwner == null) {
            handOwnerId = Optional.empty();
        } else {
            handOwnerId = Optional.of(this.handOwner.getUuid());
        }
        this.dataTracker.startTracking(HAND_OWNER_ID, handOwnerId);
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
