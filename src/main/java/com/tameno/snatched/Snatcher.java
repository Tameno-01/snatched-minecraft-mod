package com.tameno.snatched;

import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface Snatcher {

    public void snatched$setCurrentHandSeat(HandSeatEntity newHandSeat);

    public HandSeatEntity snatched$getCurrentHandSeat(World world);

    public Vec3d snatched$getHoldPosition();

    public void snatched$setHoldPosition(Vec3d newHoldPosition);

    public boolean snatched$getFlipWhenUsingLeftHandAsMainHand();

    public void snatched$setFlipWhenUsingLeftHandAsMainHand(boolean newFlipWhenUsingLeftHandAsMainHand);

}
