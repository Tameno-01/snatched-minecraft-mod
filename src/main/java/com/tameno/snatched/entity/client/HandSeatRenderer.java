package com.tameno.snatched.entity.client;

import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class HandSeatRenderer extends EntityRenderer<HandSeatEntity> {
    public HandSeatRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(HandSeatEntity entity) {
        return null;
    }
}
