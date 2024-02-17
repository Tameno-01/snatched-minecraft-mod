package com.tameno.snatched;

import com.tameno.snatched.config.SnatcherSettings;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface Snatcher {

    public void snatched$setCurrentHandSeat(HandSeatEntity newHandSeat);

    public HandSeatEntity snatched$getCurrentHandSeat(World world);

    public SnatcherSettings snatched$getSnatcherSettings();

}
