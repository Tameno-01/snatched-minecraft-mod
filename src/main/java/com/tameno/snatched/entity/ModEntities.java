package com.tameno.snatched.entity;

import com.tameno.snatched.Snatched;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<HandSeatEntity> HAND_SEAT = Registry.register(Registries.ENTITY_TYPE,
            new Identifier(Snatched.MOD_ID, "handseat"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, HandSeatEntity::new).build());

    public static void registerModEntities() {
        Snatched.LOGGER.info("Registering entities for " + Snatched.MOD_ID);
    }
}
