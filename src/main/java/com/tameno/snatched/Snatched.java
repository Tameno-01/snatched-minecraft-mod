package com.tameno.snatched;

import com.tameno.snatched.entity.ModEntities;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import com.tameno.snatched.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Snatched implements ModInitializer {
	public static final String MOD_ID = "snatched";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModEntities.registerModEntities();
	}


}
