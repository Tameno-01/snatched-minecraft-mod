package com.tameno.snatched.item.custom;


import com.tameno.snatched.Snatched;
import com.tameno.snatched.entity.ModEntities;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class SnatchingGloveItem extends Item {

    private HandSeatEntity currentHandSeat;

    public SnatchingGloveItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {

        World world = user.getWorld();

        boolean willSnatch = hand == Hand.MAIN_HAND && currentHandSeat == null && canSnatch(entity);

        if (world.isClient()) {
            if (willSnatch) {
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }

        if(willSnatch)
        {
            //Spawn hand seat
            HandSeatEntity handSeat = new HandSeatEntity(ModEntities.HAND_SEAT, world);
            handSeat.setHandOwner(user);
            handSeat.setHandHand(hand);
            world.spawnEntity(handSeat);
            //Make entity ride hand seat
            entity.startRiding(handSeat, true);

            return ActionResult.SUCCESS;

        }
        return ActionResult.PASS;
    }

    private boolean canSnatch(Entity entity) {
        if (entity instanceof ShulkerEntity) {
            return false;
        }
        return true;
    }
}
