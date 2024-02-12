package com.tameno.snatched;

import com.tameno.snatched.entity.custom.HandSeatEntity;

public interface Snatcher {
    public void snatched$setCurrentHandSeat(HandSeatEntity newHandSeat);

    public HandSeatEntity snatched$getCurrentHandSeat();

}
