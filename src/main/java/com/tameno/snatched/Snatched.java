package com.tameno.snatched;

import com.tameno.snatched.entity.ModEntities;
import com.tameno.snatched.config.SnatcherSettings;
import com.tameno.snatched.entity.custom.HandSeatEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.UUID;

public class Snatched implements ModInitializer {
	public static String MOD_ID = "snatched";
    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final GameRules.Key<GameRules.IntRule> SIZE_THRESHOLD = GameRuleRegistry.register(
		"snatchedSizeThreshold",
		GameRules.Category.PLAYER,
		GameRuleFactory.createIntRule(75, 0)
	);
	public static Identifier SNATCHER_SETTINGS_SYNC_ID = new Identifier(MOD_ID, "sync_snatcher_settings");
	public static HashMap<UUID, SnatcherSettings> allSnatcherSettings = new HashMap<UUID, SnatcherSettings>();
	public static final Identifier ATTACK_AIR_PACKET_ID = new Identifier(MOD_ID, "attacked_air");

	@Override
	public void onInitialize() {

		ModEntities.registerModEntities();

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity.getRootVehicle() instanceof HandSeatEntity) {
				return !source.getType().msgId().equals("inWall");
			}
			return true;
		});

		UseEntityCallback.EVENT.register((PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) -> {

			Snatcher snatcherPlayer = (Snatcher) player;

			double sizeThreshold = ((double) world.getGameRules().getInt(SIZE_THRESHOLD)) / 100.0;

			boolean willSnatch = (
				player.isSneaking()
				&& canSnatch(player, entity, sizeThreshold)
				&& snatcherPlayer.snatched$getCurrentHandSeat(world) == null
			);

			if (world.isClient()) {
				return willSnatch ? ActionResult.SUCCESS : ActionResult.PASS;
			}

			if(!willSnatch) {
				return ActionResult.PASS;
			}

			HandSeatEntity newHandSeat = new HandSeatEntity(ModEntities.HAND_SEAT, world);
			newHandSeat.setHandOwner(player);
			newHandSeat.setPosition(player.getPos());
			world.spawnEntity(newHandSeat);
			entity.startRiding(newHandSeat, true);
			snatcherPlayer.snatched$setCurrentHandSeat(newHandSeat);

			return ActionResult.SUCCESS;

		});

		UseBlockCallback.EVENT.register((PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) -> {

			if (world.isClient()) {
				return ActionResult.PASS;
			}

			Snatcher snatcherPlayer = (Snatcher) player;
			HandSeatEntity handSeat = snatcherPlayer.snatched$getCurrentHandSeat(world);

			boolean willUnSnatch = hand == Hand.OFF_HAND && handSeat != null;

			if (!willUnSnatch) {
				return ActionResult.PASS;
			}

			if (!handSeat.hasPassengers()) {
				return ActionResult.PASS; // avoid potential crash (probably)
			}

			BlockPos releasePosBlock = hitResult.getBlockPos().offset(hitResult.getSide());
			BlockState blockState = world.getBlockState(releasePosBlock);
			if (!blockState.getCollisionShape(world, releasePosBlock).isEmpty()) {
				return ActionResult.PASS;
			}
			Vec3d releasePos = releasePosBlock.toCenterPos();
			releasePos = releasePos.add(0.0, -0.5, 0.0);
			Entity snatchedEntity = handSeat.getFirstPassenger();
			if (snatchedEntity == null) return ActionResult.PASS;
			snatchedEntity.dismountVehicle();
			snatchedEntity.setPosition(releasePos);

			snatcherPlayer.snatched$setCurrentHandSeat(null);

			return ActionResult.SUCCESS;
		});

		ServerPlayNetworking.registerGlobalReceiver(Snatched.ATTACK_AIR_PACKET_ID,
				(server, player, handler, buf, responseSender) -> {
			if (player instanceof Snatcher snatcher) {
				final HandSeatEntity handSeat = snatcher.snatched$getCurrentHandSeat(player.getWorld());
				if (handSeat == null) return;
				final Entity entity = handSeat.getFirstPassenger();
				if (entity == null) return;

				final Vec3d lookDirection = player.getRotationVector();
				final double launchPower = Math.sqrt(getSize(player));
				final Vec3d velocity = lookDirection.multiply(launchPower);
				if (entity instanceof ServerPlayerEntity serverPlayer) {
					serverPlayer.dismountVehicle();
					serverPlayer.addVelocity(velocity);
					entity.velocityDirty = true;
					entity.velocityModified = true;
				} else {
					entity.dismountVehicle();
					entity.setVelocity(velocity);
					entity.velocityDirty = true;
				}
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(Snatched.SNATCHER_SETTINGS_SYNC_ID,
				(server, player, handler, buffer, responseSender) -> {
			SnatcherSettings playerSettings = new SnatcherSettings();
			playerSettings.readFromBuf(buffer);
			PacketByteBuf newBuffer = PacketByteBufs.create();
			newBuffer.writeUuid(player.getUuid());
			playerSettings.writeToBuf(newBuffer);
			for (ServerPlayerEntity playerToSendPacketTo : PlayerLookup.all(server)) {
				ServerPlayNetworking.send(playerToSendPacketTo, Snatched.SNATCHER_SETTINGS_SYNC_ID, newBuffer);
			}
		});
	}

	private static boolean isInSnatchChain(Snatcher snatcher, Entity entity, World world) {
		while (entity != null) {
			if (entity == snatcher) return true;
			if (entity instanceof Snatcher snatcherEntity) {
				HandSeatEntity handSeat = snatcherEntity.snatched$getCurrentHandSeat(world);
				if (handSeat == null) return false;
				entity = handSeat.getFirstPassenger();
			} else {
				return false;
			}
		}
		return false;
	}

	private static boolean canSnatch(PlayerEntity snatcher, Entity entity, double sizeThreshold) {
		if (entity instanceof Snatcher snatcherEntity && !snatcherEntity.snatched$getSnatcherSettings().canBeSnatched) {
			return false;
		}
		return (
			entity instanceof LivingEntity
			&& entity.getFirstPassenger() == null
			&& (!(entity instanceof ShulkerEntity))
			&& (!entity.isSneaking())
			&& getSize(snatcher) * sizeThreshold >= getSize(entity)
		);
	}

	public static double getSize(Entity entity) {
		if (entity instanceof PlayerEntity player) {
            double baseHeight = player.getHeight();
			if (player.isSneaking()) {
				return baseHeight * 1.2;
			}
			if (player.isInSwimmingPose()) {
				return baseHeight * 3.0;
			}
			if (player.isFallFlying()) {
				return baseHeight * 3.0;
			}
			return baseHeight;
		}
		return entity.getHeight();
	}
}
