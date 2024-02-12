package com.tameno.snatched.item.custom;


import com.tameno.snatched.Snatched;
import com.tameno.snatched.entity.ModEntities;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.block.MyceliumBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.logging.Logger;

public class SnatchingGloveItem extends Item {

    private static final String HAND_SEAT_KEY = "handSeat";

    public SnatchingGloveItem(Settings settings) {
        super(settings);
        Snatched.LOGGER.info("Snatching glove initialized");
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        HandSeatEntity currentHandSeat = getCurrentHandSeat(itemStack, world);
        if (currentHandSeat == null) {
            return TypedActionResult.pass(itemStack);
        }
        if (!world.isClient()) {
            currentHandSeat.discard();
            setCurrentHandSeat(itemStack, null);
        }
        return TypedActionResult.success(itemStack);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {

        World world = user.getWorld();

        boolean willSnatch = hand == Hand.MAIN_HAND && getCurrentHandSeat(stack, world) == null && canSnatch(entity);

        if (world.isClient()) {
            if (willSnatch) {
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }

        if(willSnatch)
        {
            HandSeatEntity newHandSeat = new HandSeatEntity(ModEntities.HAND_SEAT, world);
            newHandSeat.setHandOwner(user);
            world.spawnEntity(newHandSeat);
            entity.startRiding(newHandSeat, true);
            setCurrentHandSeat(stack, newHandSeat);

            return ActionResult.SUCCESS;

        }
        return ActionResult.PASS;
    }

    private boolean canSnatch(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (entity instanceof ShulkerEntity) {
            return false;
        }
        return true;
    }

    private void setCurrentHandSeat(ItemStack stack, HandSeatEntity handSeat) {
        NbtCompound nbtCompound = stack.getOrCreateNbt();
        if (handSeat == null) {
            nbtCompound.remove(HAND_SEAT_KEY);
            return;
        }
        nbtCompound.putInt(HAND_SEAT_KEY, handSeat.getId());
    }

    private HandSeatEntity getCurrentHandSeat(ItemStack stack, World world) {
        NbtCompound nbtCompound = stack.getNbt();
        if (nbtCompound == null) {
            return null;
        }
        if (!nbtCompound.contains(HAND_SEAT_KEY)) {
            return null;
        }
        return (HandSeatEntity) (world.getEntityById(nbtCompound.getInt(HAND_SEAT_KEY)));
    }
}
