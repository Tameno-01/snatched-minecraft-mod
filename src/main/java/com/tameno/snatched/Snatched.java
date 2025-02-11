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
import net.fabricmc.loader.api.FabricLoader;
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

public class Snatched implements ModInitializer {
	public static String MOD_ID = "snatched";
    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final GameRules.Key<GameRules.IntRule> SIZE_THRESHOLD = GameRuleRegistry.register(
		"snatchedSizeThreshold",
		GameRules.Category.PLAYER,
		GameRuleFactory.createIntRule(75, -1)
	);
	public static final GameRules.Key<GameRules.IntRule> CAPPED_THROW_SPEED = GameRuleRegistry.register(
		"snatchedCappedThrowSpeed",
		GameRules.Category.PLAYER,
		GameRuleFactory.createIntRule(100, -1)
	);
	public static Identifier SNATCHER_SETTINGS_SYNC_ID = new Identifier(MOD_ID, "sync_snatcher_settings");
	public static HashMap<UUID, SnatcherSettings> allSnatcherSettings = new HashMap<UUID, SnatcherSettings>();
	public static final Identifier ATTACK_AIR_PACKET_ID = new Identifier(MOD_ID, "attacked_air");
	private static Boolean isPehkuiLoaded = null;

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

			boolean willSnatch = (
				player.isSneaking()
				&& canSnatch(player, entity, world.getGameRules().getInt(SIZE_THRESHOLD))
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

			boolean willUnSnatch = handSeat != null;

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

				final Vec3d lookDirection = new Vec3d(
					buf.readDouble(),
					buf.readDouble(),
					buf.readDouble()
				);
				final Vec3d lookDifference = new Vec3d(
					buf.readDouble(),
					buf.readDouble(),
					buf.readDouble()
				);
				final Vec3d playerVelocity = new Vec3d(
					buf.readDouble(),
					buf.readDouble(),
					buf.readDouble()
				);
				final double launchPower = Math.sqrt(Snatched.getSize(player));

                Vec3d velocity = lookDirection.add(lookDifference);

                World world = player.getWorld();
                int cappedThrowSpeed = world.getGameRules().getInt(Snatched.CAPPED_THROW_SPEED);
                double cappedThrowSpeedProper = ((double) cappedThrowSpeed) / 100.0;
                if (cappedThrowSpeed != -1 && velocity.length() > cappedThrowSpeedProper) {
					velocity = velocity.normalize().multiply(cappedThrowSpeedProper);
                }

                velocity = velocity.multiply(launchPower);
                velocity = velocity.add(playerVelocity.multiply(1.5));
				final float motionScale = getMotionScale(entity);
				if (motionScale != 1F) {
					velocity = velocity.multiply(1.0 / motionScale);
				}

				entity.dismountVehicle();
				entity.setVelocity(velocity);
				entity.velocityDirty = true;
				entity.velocityModified = true;
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

	private static boolean canSnatch(PlayerEntity snatcher, Entity entity, int sizeThreshold) {
		if (!(entity instanceof LivingEntity)) {
			return false;
		}
		if (entity.getFirstPassenger() != null) {
			return false;
		}
		if (entity.isSneaking()) {
			return false;
		}
		if ( // Checks player's config file
			entity instanceof Snatcher snatcherEntity
			&& !snatcherEntity.snatched$getSnatcherSettings().canBeSnatched
		) {
			return false;
		}
		if (entity instanceof ShulkerEntity) {
			return false;
		}
		if (sizeThreshold != -1) {
			double sizeThresholdProper = ((double) sizeThreshold) / 100.0;
			return getSize(snatcher) * sizeThresholdProper >= getSize(entity);
		}
		return true;
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

	public static boolean getIsPehkuiLoaded() {
		if (isPehkuiLoaded == null) {
			isPehkuiLoaded = FabricLoader.getInstance().isModLoaded("pehkui");
		}
		return isPehkuiLoaded;
	}

	// If Pehkui is installed, use get the motion scale of a given player
	public static float getMotionScale(Entity entity) {
		if (!Snatched.getIsPehkuiLoaded()) return 1.0F;

		try {
			// Access the ScaleUtils class dynamically
			Class<?> scaleUtilsClass = Class.forName("virtuoel.pehkui.util.ScaleUtils");

			// Access the getVisibilityScale method that takes an Entity and tickDelta
			Method getMotionScale = scaleUtilsClass.getDeclaredMethod("getMotionScale", Entity.class);

			// Make the method accessible in case it's private or protected
			getMotionScale.setAccessible(true);

			// Invoke the method on ScaleUtils class with the provided entity and tickDelta
			return (float) getMotionScale.invoke(null, entity);
		} catch (Exception e) {
			Snatched.LOGGER.error("Pehkui was loaded, but we could not get the motion scale. See error below:");
			Snatched.LOGGER.error(e.toString());
		}

		// Return a default value or throw an exception if reflection fails
		return 1.0F; // Default scaling factor (no scaling)
	}
}
