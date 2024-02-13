package com.tameno.snatched;

import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.world.World;

public interface Snatcher {
    public void snatched$setCurrentHandSeat(HandSeatEntity newHandSeat);

    public HandSeatEntity snatched$getCurrentHandSeat(World world);

}
